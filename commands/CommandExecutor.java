package commands;

import collection.CollectionManager;
import model.*;
import utils.InputValidator;

import java.io.*;
import java.util.*;

/**
 * Единый класс для обработки всех команд пользователя.
 * Содержит логику выполнения всех 15 команд лабораторной работы.
 * Реализует основной цикл обработки команд и выполнение скриптов.
 *
 * @author Polina
 * @version 1.0
 */

// Единый класс для обработки всех команд. Содержит логику выполнения всех 15 команд
public class CommandExecutor {
    private final CollectionManager collectionManager;  // бизнес-логика (добавить, удалить, сохранить)
    private final Scanner scanner; // читает ввод пользователя
    private boolean isRunning; // флаг управления циклом while в start()
    private final Set<String> scriptsInProgress; // хранит имена выполняемых скриптов (защита от рекурсии)

    /**
     * Конструктор класса.
     * Инициализирует все поля и подготавливает командный процессор к работе.
     *
     * @param collectionManager менеджер коллекции для выполнения операций
     */

    // Конструктор класса
    public CommandExecutor(CollectionManager collectionManager) {
        this.collectionManager = collectionManager; // передаем зависимость
        this.scanner = new Scanner(System.in); // стандартный поток ввода
        this.isRunning = true; // флаг работы
        this.scriptsInProgress = new HashSet<>(); // пустое множество для отслеживания скриптов
    }

    /**
     * Запускает основной цикл обработки команд.
     * Выводит приглашение к вводу и обрабатывает команды до команды exit.
     */

    // Запускает основной цикл обработки команд
    public void start() {
        System.out.println("Программа запущена. Введите 'help' для списка команд.");
        while (isRunning) { // бесконечный цикл до exit
            System.out.print("> ");
            String input = scanner.nextLine().trim(); // чтение строки + удаление пробелов по краям
            if (!input.isEmpty()) { // пропускает пустые строки
                processCommand(input.split("\\s+")); // разбиение строки по пробелам
            }
        }
        scanner.close(); // закрывает поток ввода
    }

    /**
     * Обрабатывает команду, разбитую на части.
     * Определяет тип команды и вызывает соответствующий обработчик.
     *
     * @param commandParts массив строк, где первый элемент - команда, остальные - аргументы
     */

    // Обрабатывает команду
    private void processCommand(String[] commandParts) {
        String command = commandParts[0].toLowerCase(); // не обращает внимание на регистр
        String[] args = Arrays.copyOfRange(commandParts, 1, commandParts.length);

        // Обработка ошибок
        try {
            switch (command) { // выбираем действие в зависимости от команд
                case "help": // вывод справки
                    printHelp();
                    break;
                case "info":
                    System.out.println(collectionManager.getInfo()); // вывод информации о коллекции
                    break;
                case "show":
                    collectionManager.showAll(); // показать все элементы коллекции
                    break;
                case "add":
                    handleAdd(); // обработка добавления элемента
                    break;
                case "update":
                    handleUpdate(args); // обработка обновления элемента (с аргументами)
                    break;
                case "remove_by_id":
                    handleRemoveById(args); // обработка удаления по ID
                    break;
                case "clear":
                    collectionManager.clear(); // очистка коллекции
                    System.out.println("Коллекция очищена");
                    break;
                case "save":
                    collectionManager.save(); // сохранение коллекции в файл
                    break;
                case "execute_script":
                    handleExecuteScript(args); // выполнение скрипта из файла
                    break;
                case "exit":
                    isRunning = false; // флаг завершения
                    System.out.println("Программа завершена");
                    break;
                case "remove_last":
                    collectionManager.removeLast(); // удаление последнего элемента
                    break;
                case "remove_lower":
                    handleRemoveLower(); // удаление элементов меньше заданного
                    break;
                case "sort":
                    collectionManager.sort(); // сортировка коллекции
                    break;
                case "sum_of_capacity":
                    System.out.println("Сумма capacity: " + collectionManager.getSumOfCapacity());
                    break;
                case "filter_by_capacity":
                    handleFilterByCapacity(args); // фильтр по значению capacity
                    break;
                case "filter_less_than_type":
                    handleFilterLessThanType(args); // фильтр по типу (меньше заданного)
                    break;
                default: // если команда не найдена
                    System.out.println("Неизвестная команда. Введите 'help' для справки.");
            }
        } catch (IllegalArgumentException e) { // обработка ошибок, связанных с неверными аргументами
            System.out.println("Ошибка в аргументах: " + e.getMessage());
        } catch (Exception e) { // обработка непредвиденных ошибок (NullPointerException, IOException)
            System.out.println("Ошибка выполнения команды: " + e.getMessage());
        }
    }

    // Выводит справку по командам
    private void printHelp() {
        System.out.println("""
                Доступные команды:
                help                                      - вывести справку
                info                                      - информация о коллекции
                show                                      - вывести все элементы
                add                                       - добавить новый элемент
                update id                                 - обновить элемент по id
                remove_by_id id                           - удалить элемент по id
                clear                                     - очистить коллекцию
                save                                      - сохранить коллекцию в файл
                execute_script file_name                  - выполнить скрипт
                exit                                      - завершить программу
                remove_last                               - удалить последний элемент
                remove_lower                              - удалить элементы меньше заданного
                sort                                      - отсортировать коллекцию
                sum_of_capacity                           - сумма значений capacity
                filter_by_capacity capacity               - фильтр по capacity
                filter_less_than_type type                - фильтр по типу (CAR, SUBMARINE, BICYCLE, HOVERBOARD)
                """);
    }

    /**
     * Обрабатывает команду добавления нового элемента.
     * Запрашивает данные у пользователя и добавляет элемент в коллекцию.
     */

    // Обрабатывает команду add
    private void handleAdd() {
        Vehicle vehicle = readVehicle(); // читает объект Vehicle с консоли
        collectionManager.add(vehicle); // добавляет его в коллекцию через менеджер
    }

    /**
     * Обрабатывает команду обновления элемента по ID.
     *
     * @param args массив аргументов, должен содержать ID элемента
     */

    // Обрабатывает команду update
    private void handleUpdate(String[] args) {
        if (args.length == 0) { // проверяет, передан ли аргумент
            System.out.println("Укажите id элемента");
            return; // выход из метода
        }

        try {
            int id = Integer.parseInt(args[0]); // парсит строку в целое число (может выбросить NumberFormatException)
            if (!collectionManager.containsId(id)) { // проверяет, существует ли элемент с таким ID
                System.out.println("Элемент с id " + id + " не найден");
                return;
            }

            Vehicle vehicle = readVehicle(); // читаем новый объект Vehicle
            if (collectionManager.updateById(id, vehicle)) { // обновляет элемент по ID
                System.out.println("Элемент с id " + id + " обновлен");
            }
        } catch (NumberFormatException e) { // ловит ошибку парсинга числа
            System.out.println("id должен быть числом");
        }
    }

    /**
     * Обрабатывает команду удаления элемента по ID.
     *
     * @param args массив аргументов, должен содержать ID элемента
     */

    // Обрабатывает команду remove_by_id
    private void handleRemoveById(String[] args) {
        if (args.length == 0) { // проверяет наличие аргумента
            System.out.println("Укажите id элемента");
            return;
        }

        try {
            int id = Integer.parseInt(args[0]); // парсит ID
            if (collectionManager.removeById(id)) { // удаление элемента
                System.out.println("Элемент с id " + id + " удален");
            } else {
                System.out.println("Элемент с id " + id + " не найден");
            }
        } catch (NumberFormatException e) { // ошибка парсинга
            System.out.println("id должен быть числом");
        }
    }

    /**
     * Обрабатывает команду выполнения скрипта из файла.
     * Содержит защиту от рекурсивного выполнения.
     *
     * @param args массив аргументов, должен содержать имя файла
     */

    // Обрабатывает команду execute_script
    private void handleExecuteScript(String[] args) {
        if (args.length == 0) { // проверяет наличие имени файла
            System.out.println("Укажите имя файла");
            return;
        }

        String filename = args[0]; // получение имени файла из аргументов


        // Защита от рекурсии (иначе программа будет падать)
        if (scriptsInProgress.contains(filename)) { // проверка не выполняется ли уже этот файл
            System.out.println("Ошибка: обнаружена рекурсия! Файл " + filename + " уже выполняется");
            return; // прерывает выполнение при обнаружении рекурсии
        }

        scriptsInProgress.add(filename); // добавляет файл в множество выполняющихся

        // try-with-resources - автоматически закрывает reader после использования
        try (BufferedReader reader = new BufferedReader(new FileReader(filename))) { // эффективнее
            System.out.println("Выполнение скрипта: " + filename);
            String line;
            int lineNumber = 0; // счетчик строк для удобства отладки

            while ((line = reader.readLine()) != null) { // читает файл построчно до конца
                lineNumber++;
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) continue;  // пропускает пустые строки и комментарии (#)

                System.out.println("[" + filename + ":" + lineNumber + "] " + line); // выводит выполняемую команду
                processCommand(line.split("\\s+")); // рекурсивная обработка
            }

            System.out.println("Скрипт " + filename + " выполнен");
        } catch (FileNotFoundException e) {
            System.out.println("Файл не найден: " + filename);
        } catch (IOException e) {
            System.out.println("Ошибка чтения файла: " + e.getMessage());
        } finally { // гарантия очистки даже при ошибке
            scriptsInProgress.remove(filename); // убирает файл из набора выполняющихся
        }
    }

    /**
     * Обрабатывает команду удаления элементов, меньших заданного.
     * Запрашивает эталонный элемент у пользователя.
     */

    // Обрабатывает команду remove_lower
    private void handleRemoveLower() {
        System.out.println("Введите элемент для сравнения:");
        Vehicle vehicle = readVehicle(); // читает эталонный объект
        collectionManager.removeLower(vehicle); // удаляет все элементы меньше
    }

    /**
     * Обрабатывает команду фильтрации элементов по значению capacity.
     *
     * @param args массив аргументов, должен содержать значение capacity
     */

    // Обрабатывает команду filter_by_capacity
    private void handleFilterByCapacity(String[] args) {
        if (args.length == 0) { // проверяет наличие аргумента
            System.out.println("Укажите значение capacity");
            return;
        }

        try {
            double capacity = Double.parseDouble(args[0]); // парсит число с плавающей точкой
            List<Vehicle> result = collectionManager.filterByCapacity(capacity); // получает отфильтрованный список

            if (result.isEmpty()) { // если ничего не найдено
                System.out.println("Элементы с capacity = " + capacity + " не найдены");
            } else {
                System.out.println("Найдено элементов: " + result.size());
                result.forEach(System.out::println); // выводит каждый элемент
            }
        } catch (NumberFormatException e) { // ошибка парсинга числа
            System.out.println("capacity должно быть числом");
        }
    }

    /**
     * Обрабатывает команду фильтрации элементов по типу (меньше заданного).
     *
     * @param args массив аргументов, должен содержать тип транспортного средства
     */

    // Обрабатывает команду filter_less_than_type
    private void handleFilterLessThanType(String[] args) {
        if (args.length == 0) { // проверяет наличие аргумента
            System.out.println("Укажите тип (CAR, SUBMARINE, BICYCLE, HOVERBOARD)");
            return;
        }

        try {
            VehicleType type = InputValidator.validateAndParseVehicleType(args[0]); // валидация и парсинг типа
            List<Vehicle> result = collectionManager.filterLessThanType(type); // получает отфильтрованный список

            if (result.isEmpty()) { // если ничего не найдено
                System.out.println("Элементы с типом меньше " + type + " не найдены");
            } else {
                System.out.println("Найдено элементов: " + result.size());
                result.forEach(System.out::println); // выводит каждый элемент
            }
        } catch (IllegalArgumentException e) { // ошибка валидации типа
            System.out.println(e.getMessage());
        }
    }

    /**
     * Читает данные транспортного средства с консоли.
     * Запрашивает все поля по очереди с валидацией каждого.
     *
     * @return готовый объект Vehicle с заполненными полями
     */

    // Читает объект Vehicle с консоли
    private Vehicle readVehicle() {
        Vehicle vehicle = new Vehicle(); // создает новый пустой объект
        System.out.println("Введите данные транспортного средства:");

        // Ввод имени
        while (true) { // цикл до успешного ввода
            System.out.print("  name (не пустое): ");
            String input = scanner.nextLine().trim(); // убирает пробелы
            try {
                InputValidator.validateName(input); // проверка имени через валидатор
                vehicle.setName(input); // установка имени
                break;
            } catch (IllegalArgumentException e) {
                System.out.println("  Ошибка: " + e.getMessage());
            }
        }

        // Ввод координаты X
        while (true) {
            System.out.print("  coordinate x (Double, <= 636): ");
            String input = scanner.nextLine().trim();
            try {
                Double x = input.isEmpty() ? null : Double.parseDouble(input); // преобразует в Double (может быть null)
                InputValidator.validateCoordinateX(x);

                Coordinates coords = vehicle.getCoordinates(); // получает текущие координаты (или null)
                if (coords == null) coords = new Coordinates(); // если координат нет, создаем новые
                coords.setX(x); // установка X
                vehicle.setCoordinates(coords); // сохранение координат в объект
                break;
            } catch (NumberFormatException e) { // ошибка парсинга
                System.out.println("  Ошибка: введите число");
            } catch (IllegalArgumentException e) { // ошибка валидации
                System.out.println("  Ошибка: " + e.getMessage());
            }
        }

        // Ввод координаты Y
        System.out.print("  coordinate y (int): ");
        while (true) {
            try {
                String input = scanner.nextLine().trim();
                int y = Integer.parseInt(input); // парсит целое число
                vehicle.getCoordinates().setY(y); // установка Y
                break;
            } catch (NumberFormatException e) { // ошибка парсинга
                System.out.print("  Ошибка: введите целое число: ");
            }
        }

        // Ввод enginePower
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

        // Ввод capacity
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

        // Ввод типа
        while (true) {
            System.out.print("  type (" + VehicleType.getTypes() + "): ");
            String input = scanner.nextLine().trim().toUpperCase(); // к верхнему регистру для enum
            try {
                VehicleType type = InputValidator.validateAndParseVehicleType(input);
                vehicle.setType(type);
                break;
            } catch (IllegalArgumentException e) {
                System.out.println("  Ошибка: " + e.getMessage());
            }
        }

        // Ввод типа топлива (может быть null)
        System.out.print("  fuelType (" + FuelType.getTypes() + ", или пустая строка для null): ");
        String fuelInput = scanner.nextLine().trim();
        if (!fuelInput.isEmpty()) { // если пользователь ввел не пустую строку
            try {
                FuelType fuelType = FuelType.valueOf(fuelInput.toUpperCase());
                vehicle.setFuelType(fuelType);
            } catch (IllegalArgumentException e) {
                System.out.println("  Неверный тип топлива, будет установлен null");
                vehicle.setFuelType(null);
            }
        } else { // если строка пустая
            vehicle.setFuelType(null);
        }

        return vehicle;
    }
}
