package client;

import common.*;
import common.model.*;

import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.Scanner;

/**
 * Главный класс клиентского приложения.
 *
 * @author Polina
 * @version 1.0
 */
public class Client {
    private static final String DEFAULT_HOST = "localhost"; // хост сервера по умолчанию
    private static final int DEFAULT_PORT = 1067; // порт сервера по умолчанию
    private static final int RECONNECT_DELAY = 5000; // задержка перед повторным подключением (5 секунд)
    private static final int MAX_SCRIPT_DEPTH = 5; // максимальная глубина вложенности скриптов

    private final String host; // хост сервера
    private final int port; // порт сервера
    private boolean isRunning; // флаг работы клиента
    private Scanner scanner; // сканер для чтения ввода пользователя
    private Socket socket; // сокет для соединения с сервером
    private DataOutputStream out; // поток для отправки данных с префиксом длины
    private DataInputStream in; // поток для приема данных с префиксом длины

    private int scriptDepth = 0; // текущая глубина вложенности скриптов
    private ArrayList<String> scriptStack = new ArrayList<>(); // стек выполняемых скриптов (для защиты от рекурсии)

    public Client(String host, int port) { // конструктор клиента
        this.host = host;
        this.port = port;
        this.isRunning = true;
        this.scanner = new Scanner(System.in);
    }

    public void start() { // запуск клиента: подключение → ввод команд → отправка → получение ответа
        System.out.println("Клиент запущен. Подключение к серверу " + host + ":" + port);

        while (isRunning) { // основной цикл
            try {
                connect(); // подключаемся к серверу
                System.out.println("Подключение установлено!");

                while (isRunning) { // цикл ввода команд
                    System.out.print("> ");
                    String input = scanner.nextLine().trim();

                    if (input.isEmpty()) continue; // пропускаем пустые строки

                    if (input.equalsIgnoreCase("exit")) { // команда exit - завершаем клиент
                        isRunning = false;
                        System.out.println("Клиент завершен");
                        break;
                    }

                    sendCommand(input); // отправляем команду на сервер
                }

            } catch (IOException e) { // сервер недоступен - ждем и пытаемся переподключиться
                System.out.println("Сервер недоступен. Повторное подключение через " +
                        RECONNECT_DELAY / 1000 + " секунд...");
                try {
                    Thread.sleep(RECONNECT_DELAY);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }
        close(); // закрываем ресурсы
    }

    private void connect() throws IOException { // устанавливает соединение с сервером
        socket = new Socket(host, port); // создаем сокет
        out = new DataOutputStream(socket.getOutputStream()); // поток на отправку (с префиксом длины)
        in = new DataInputStream(socket.getInputStream()); // поток на чтение (с префиксом длины)
    }

    private void sendCommand(String input) throws IOException { // отправляет команду на сервер и выводит ответ
        // разбираем введенную строку
        String[] parts = input.split("\\s+", 2);
        String commandName = parts[0].toLowerCase();
        String argument = parts.length > 1 ? parts[1] : null;

        Command command = createCommand(commandName, argument); // создаем объект Command

        // если команда не распознана - выходим
        if (command == null) {
            System.out.println("Неизвестная команда. Введите 'help' для справки.");
            return;
        }

        // execute_script выполняется локально, не отправляется на сервер
        if (command.getType() == CommandType.EXECUTE_SCRIPT) {
            executeScript((String) command.getArgument());
            return;
        }

        // отправка с префиксом длины (чтобы сервер знал, сколько байт читать)
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(bos);
        oos.writeObject(new Request(command));
        oos.flush();
        byte[] data = bos.toByteArray();

        out.writeInt(data.length); // сначала отправляем длину
        out.write(data); // потом сами данные
        out.flush();

        // чтение ответа с префиксом длины
        int responseLength = in.readInt(); // читаем длину ответа
        byte[] responseData = new byte[responseLength];
        in.readFully(responseData); // читаем данные

        try (ByteArrayInputStream bis = new ByteArrayInputStream(responseData);
             ObjectInputStream ois = new ObjectInputStream(bis)) {
            Response response = (Response) ois.readObject(); // десериализуем ответ

            // вывод результата
            if (response.getStatus() == ResponseStatus.SUCCESS) {
                System.out.println(response.getMessage());
                if (response.getData() != null) {
                    Object dataObj = response.getData();
                    if (dataObj instanceof java.util.List) { // для команды show - выводим поэлементно
                        @SuppressWarnings("unchecked")
                        java.util.List<Vehicle> list = (java.util.List<Vehicle>) dataObj;
                        for (Vehicle v : list) {
                            System.out.println(v);
                        }
                    } else {
                        System.out.println(dataObj);
                    }
                }
            } else {
                System.err.println("Ошибка: " + response.getMessage());
            }
        } catch (ClassNotFoundException e) {
            System.err.println("Ошибка десериализации ответа");
        }
    }

    // создает объект Command на основе введенной строки
    private Command createCommand(String commandName, String argument) {
        CommandType type = CommandType.fromString(commandName);
        if (type == null) return null;

        switch (type) {
            // команды без аргументов
            case HELP: case INFO: case SHOW: case CLEAR:
            case REMOVE_LAST: case SORT: case SUM_OF_CAPACITY:
                return new Command(type);

            // add - требует ввода vehicle
            case ADD:
                return createAddCommand();

            // update - требует id и новый vehicle
            case UPDATE:
                if (argument == null) {
                    System.out.println("Укажите id элемента");
                    return null;
                }
                try {
                    int id = Integer.parseInt(argument);
                    return createUpdateCommand(id);
                } catch (NumberFormatException e) {
                    System.out.println("id должен быть числом");
                    return null;
                }

                // remove_by_id - требует id
            case REMOVE_BY_ID:
                if (argument == null) {
                    System.out.println("Укажите id элемента");
                    return null;
                }
                try {
                    int id = Integer.parseInt(argument);
                    return new Command(type, id);
                } catch (NumberFormatException e) {
                    System.out.println("id должен быть числом");
                    return null;
                }

                // remove_lower - требует эталонный vehicle
            case REMOVE_LOWER:
                return createRemoveLowerCommand();

            // filter_by_capacity - требует значение capacity
            case FILTER_BY_CAPACITY:
                if (argument == null) {
                    System.out.println("Укажите значение capacity");
                    return null;
                }
                try {
                    double capacity = Double.parseDouble(argument);
                    return new Command(type, capacity);
                } catch (NumberFormatException e) {
                    System.out.println("capacity должно быть числом");
                    return null;
                }

                // filter_less_than_type - требует тип
            case FILTER_LESS_THAN_TYPE:
                if (argument == null) {
                    System.out.println("Укажите тип");
                    return null;
                }
                try {
                    VehicleType vehicleType = InputValidator.validateAndParseVehicleType(argument);
                    return new Command(type, vehicleType);
                } catch (IllegalArgumentException e) {
                    System.out.println(e.getMessage());
                    return null;
                }

                // execute_script - требует имя файла
            case EXECUTE_SCRIPT:
                if (argument == null) {
                    System.out.println("Укажите имя файла скрипта");
                    return null;
                }
                return new Command(type, argument);

            // exit
            case EXIT:
                return new Command(type);

            default:
                return null;
        }
    }

    private Command createAddCommand() { // создает команду add
        System.out.println("Введите данные для добавления:");
        Vehicle vehicle = readVehicle();
        return new Command(CommandType.ADD, vehicle);
    }

    private Command createUpdateCommand(int id) { // создает команду update
        System.out.println("Введите новые данные для элемента с ID " + id + ":");
        Vehicle vehicle = readVehicle();
        return new Command(CommandType.UPDATE, new Object[]{id, vehicle});
    }

    private Command createRemoveLowerCommand() { // создает команду remove_lower
        System.out.println("Введите эталонный элемент для сравнения:");
        Vehicle vehicle = readVehicle();
        return new Command(CommandType.REMOVE_LOWER, vehicle);
    }

    // выполняет скрипт из файла
    private void executeScript(String filename) {
        // проверка на глубину вложенности
        if (scriptDepth >= MAX_SCRIPT_DEPTH) {
            System.out.println("Ошибка: превышена максимальная глубина вложенности скриптов (" + MAX_SCRIPT_DEPTH + ")");
            return;
        }

        // проверка на рекурсию (один и тот же скрипт вызывает сам себя)
        if (scriptStack.contains(filename)) {
            System.out.println("Ошибка: обнаружена рекурсия (скрипт " + filename + " уже выполняется)");
            return;
        }

        scriptStack.add(filename); // добавляем в стек
        scriptDepth++; // увеличиваем глубину

        try (BufferedReader reader = new BufferedReader(new FileReader(filename))) {
            String line;
            int lineNum = 0;

            while ((line = reader.readLine()) != null) {
                lineNum++;
                line = line.trim();

                if (line.isEmpty() || line.startsWith("#")) continue; // пропускаем пустые строки и комментарии

                System.out.println("[Скрипт " + filename + ":" + lineNum + "] > " + line);
                sendCommand(line); // выполняем команду из скрипта
            }

            System.out.println("Скрипт " + filename + " выполнен успешно");

        } catch (FileNotFoundException e) {
            System.out.println("Ошибка: файл не найден - " + filename);
        } catch (IOException e) {
            System.out.println("Ошибка чтения файла: " + e.getMessage());
        } finally {
            scriptDepth--; // уменьшаем глубину
            scriptStack.remove(scriptStack.size() - 1); // убираем из стека
        }
    }

    // читает данные транспортного средства с консоли
    private Vehicle readVehicle() {
        Vehicle vehicle = new Vehicle();
        System.out.println("Введите данные транспортного средства:");

        // ввод имени
        while (true) {
            System.out.print("  name (не пустое): ");
            String input = scanner.nextLine().trim();
            try {
                InputValidator.validateName(input);
                vehicle.setName(input);
                break;
            } catch (IllegalArgumentException e) {
                System.out.println("  Ошибка: " + e.getMessage());
            }
        }

        // ввод координаты x
        while (true) {
            System.out.print("  coordinate x (Double, <= 636): ");
            String input = scanner.nextLine().trim();
            try {
                Double x = input.isEmpty() ? null : Double.parseDouble(input);
                InputValidator.validateCoordinateX(x);

                Coordinates coords = vehicle.getCoordinates();
                if (coords == null) coords = new Coordinates();
                coords.setX(x);
                vehicle.setCoordinates(coords);
                break;
            } catch (NumberFormatException e) {
                System.out.println("  Ошибка: введите число");
            } catch (IllegalArgumentException e) {
                System.out.println("  Ошибка: " + e.getMessage());
            }
        }

        // ввод координаты y
        System.out.print("  coordinate y (int): ");
        while (true) {
            try {
                String input = scanner.nextLine().trim();
                int y = Integer.parseInt(input);
                vehicle.getCoordinates().setY(y);
                break;
            } catch (NumberFormatException e) {
                System.out.print("  Ошибка: введите целое число: ");
            }
        }

        // ввод мощности двигателя
        while (true) {
            System.out.print("  enginePower (Double > 0): ");
            String input = scanner.nextLine().trim();
            try {
                Double power = Double.parseDouble(input);
                InputValidator.validateEnginePower(power);
                vehicle.setEnginePower(power);
                break;
            } catch (NumberFormatException e) {
                System.out.println("  Ошибка: введите число");
            } catch (IllegalArgumentException e) {
                System.out.println("  Ошибка: " + e.getMessage());
            }
        }

        // ввод вместимости
        while (true) {
            System.out.print("  capacity (double > 0): ");
            String input = scanner.nextLine().trim();
            try {
                double capacity = Double.parseDouble(input);
                InputValidator.validateCapacity(capacity);
                vehicle.setCapacity(capacity);
                break;
            } catch (NumberFormatException e) {
                System.out.println("  Ошибка: введите число");
            } catch (IllegalArgumentException e) {
                System.out.println("  Ошибка: " + e.getMessage());
            }
        }

        // ввод типа транспорта
        while (true) {
            System.out.print("  type (" + VehicleType.getTypes() + "): ");
            String input = scanner.nextLine().trim().toUpperCase();
            try {
                VehicleType type = InputValidator.validateAndParseVehicleType(input);
                vehicle.setType(type);
                break;
            } catch (IllegalArgumentException e) {
                System.out.println("  Ошибка: " + e.getMessage());
            }
        }

        // ввод типа топлива (может быть null)
        System.out.print("  fuelType (" + FuelType.getTypes() + ", или пустая строка для null): ");
        String fuelInput = scanner.nextLine().trim();
        if (!fuelInput.isEmpty()) {
            try {
                vehicle.setFuelType(FuelType.valueOf(fuelInput.toUpperCase()));
            } catch (IllegalArgumentException e) {
                System.out.println("  Неверный тип топлива, будет установлен null");
                vehicle.setFuelType(null);
            }
        } else {
            vehicle.setFuelType(null);
        }

        return vehicle;
    }

    // закрывает все ресурсы клиента
    private void close() {
        try {
            if (in != null) in.close();
            if (out != null) out.close();
            if (socket != null) socket.close();
            if (scanner != null) scanner.close();
        } catch (IOException e) {
            System.err.println("Ошибка закрытия ресурсов: " + e.getMessage());
        }
    }

    // точка входа в клиентское приложение
    public static void main(String[] args) {
        String host = DEFAULT_HOST;
        int port = DEFAULT_PORT;

        if (args.length >= 1) host = args[0];
        if (args.length >= 2) {
            try {
                port = Integer.parseInt(args[1]);
            } catch (NumberFormatException e) {
                System.err.println("Порт должен быть числом. Используется порт по умолчанию");
            }
        }

        new Client(host, port).start();
    }
}
