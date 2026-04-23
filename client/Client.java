package client;

import common.*;
import common.model.*;

import java.io.*;
import java.net.*;
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

    private final String host; // хост сервера
    private final int port; // порт сервера
    private boolean isRunning; // флаг работы клиента
    private Scanner scanner; // сканер для чтения ввода пользователя
    private Socket socket; // сокет для соединения с сервером
    private ObjectOutputStream out; // поток для отправки сериализованных объектов
    private ObjectInputStream in; // поток для приема сериализованных объектов

    public Client(String host, int port) { // конструктор клиента
        this.host = host;
        this.port = port;
        this.isRunning = true;
        this.scanner = new Scanner(System.in);
    }

    public void start() { // запуск клиента: подключение → ввод команд → отправка → получение ответа
        System.out.println("Клиент запущен. Подключение к серверу " + host + ":" + port);

        while (isRunning) {  // основной цикл
            try {
                connect(); // подключаемся к серверу
                System.out.println("Подключение установлено!");

                while (isRunning) { // цикл ввода команд
                    System.out.print("> ");
                    String input = scanner.nextLine().trim();

                    if (input.isEmpty()) {  // пропускаем пустые строки
                        continue;
                    }

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
        out = new ObjectOutputStream(socket.getOutputStream()); // поток на отправку
        in = new ObjectInputStream(socket.getInputStream());  // поток на чтение
    }

    private void sendCommand(String input) { // отправляет команду на сервер и выводит ответ
        try {
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

            // создаем и отправляем запрос
            Request request = new Request(command);
            out.writeObject(request);
            out.flush();

            Response response = (Response) in.readObject(); // получаем ответ от сервера

            // выводим результат
            if (response.getStatus() == ResponseStatus.SUCCESS) {
                System.out.println(response.getMessage());
                if (response.getData() != null) {
                    System.out.println(response.getData());
                }
            } else {
                System.err.println("Ошибка: " + response.getMessage());
            }

        } catch (IOException e) {
            System.out.println("Ошибка связи с сервером.");
            throw new RuntimeException("Connection lost", e);
        } catch (ClassNotFoundException e) {
            System.err.println("Ошибка десериализации ответа");
        }
    }

    // создает объект Command на основе введенной строки
    private Command createCommand(String commandName, String argument) {
        CommandType type = CommandType.fromString(commandName);

        if (type == null) {
            return null;
        }

        switch (type) {
            // команды без аргументов
            case HELP:
            case INFO:
            case SHOW:
            case CLEAR:
            case REMOVE_LAST:
            case SORT:
            case SUM_OF_CAPACITY:
                return new Command(type);

            // команда ADD - требует ввода Vehicle
            case ADD:
                return createAddCommand();

            // команда UPDATE - требует ID и новый Vehicle
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

            // команда REMOVE_BY_ID - требует ID
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

            // команда REMOVE_LOWER - требует эталонный Vehicle
            case REMOVE_LOWER:
                return createRemoveLowerCommand();

             // команда FILTER_BY_CAPACITY - требует значение capacity
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

            // команда FILTER_LESS_THAN_TYPE - требует тип
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

            case EXIT:
                return new Command(type);

            default:
                return null;
        }
    }

     private Command createAddCommand() { // создает команду ADD
        System.out.println("Введите данные для добавления:");
        Vehicle vehicle = readVehicle();
        return new Command(CommandType.ADD, vehicle);
    }

     private Command createUpdateCommand(int id) { // создает команду UPDATE
        System.out.println("Введите новые данные для элемента с ID " + id + ":");
        Vehicle vehicle = readVehicle();
        return new Command(CommandType.UPDATE, new Object[]{id, vehicle});
    }

   private Command createRemoveLowerCommand() { // создает команду REMOVE_LOWER
        System.out.println("Введите эталонный элемент для сравнения:");
        Vehicle vehicle = readVehicle();
        return new Command(CommandType.REMOVE_LOWER, vehicle);
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

        // ввод координаты X
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

        // ввод координаты Y
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

        // ввод enginePower
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

        // ввод capacity
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

        // ввод типа
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

        // ввод типа топлива
        System.out.print("  fuelType (" + FuelType.getTypes() + ", или пустая строка для null): ");
        String fuelInput = scanner.nextLine().trim();
        if (!fuelInput.isEmpty()) {
            try {
                FuelType fuelType = FuelType.valueOf(fuelInput.toUpperCase());
                vehicle.setFuelType(fuelType);
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

        // парсим аргументы командной строки
        if (args.length >= 1) {
            host = args[0];
        }
        if (args.length >= 2) {
            try {
                port = Integer.parseInt(args[1]);
            } catch (NumberFormatException e) {
                System.err.println("Порт должен быть числом. Используется порт по умолчанию");
            }
        }

        Client client = new Client(host, port);
        client.start();
    }
}
