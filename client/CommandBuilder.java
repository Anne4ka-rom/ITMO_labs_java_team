package client;

import common.*;
import common.model.*;

/**
 * Отвечает за разбор строки ввода и создание соответствующих объектов Command.
 * Фабричный слой между вводом пользователя и сетевым слоем.
 *
 * @author Polina
 * @version 2.0
 */
public class CommandBuilder {

    private final VehicleReader vehicleReader; // для чтения Vehicle из консоли
    private String login; // логин текущего пользователя
    private String password; // пароль текущего пользователя

    // конструктор: сохраняем VehicleReader и учетные данные пользователя
    public CommandBuilder(VehicleReader vehicleReader, String login, String password) {
        this.vehicleReader = vehicleReader;
        this.login = login;
        this.password = password;
    }

    // обновление учетных данных после успешного логина
    public void setCredentials(String login, String password) {
        this.login = login;
        this.password = password;
    }

    // основной метод: создает команду из строки ввода
    public Command createCommand(String commandName, String argument) {
        // преобразуем имя команды в тип перечисления
        CommandType type = CommandType.fromString(commandName);
        if (type == null) return null; // неизвестная команда

        switch (type) {
            // команды без аргументов - просто создаем с логином/паролем
            case HELP: case INFO: case SHOW: case CLEAR:
            case REMOVE_LAST: case SORT: case SUM_OF_CAPACITY:
                return new Command(type, login, password);

            // команды регистрации и входа - требуют логин и пароль
            case REGISTER:
            case LOGIN:
                // проверяем, что аргумент передан
                if (argument == null) {
                    System.out.println("Укажите логин и пароль: " + commandName + " <login> <password>");
                    return null;
                }
                // разбиваем аргумент на логин и пароль
                String[] parts = argument.split("\\s+", 2);
                if (parts.length < 2) {
                    System.out.println("Укажите логин и пароль");
                    return null;
                }
                // передаем массив [логин, пароль] в команду
                return new Command(type, new String[]{parts[0], parts[1]}, login, password);

            // добавление элемента - требует ввода данных
            case ADD:
                return createAddCommand();

            // добавление случайных элементов - без аргументов
            case ADD_RANDOM:
                return new Command(type, login, password);

            // обновление элемента - требует id
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

            // удаление по id
            case REMOVE_BY_ID:
                if (argument == null) {
                    System.out.println("Укажите id элемента");
                    return null;
                }
                try {
                    int id = Integer.parseInt(argument);
                    return new Command(type, id, login, password);
                } catch (NumberFormatException e) {
                    System.out.println("id должен быть числом");
                    return null;
                }

            // удаление меньших - требует ввода эталонного элемента
            case REMOVE_LOWER:
                return createRemoveLowerCommand();

            // фильтрация по вместимости
            case FILTER_BY_CAPACITY:
                if (argument == null) {
                    System.out.println("Укажите значение capacity");
                    return null;
                }
                try {
                    double capacity = Double.parseDouble(argument);
                    return new Command(type, capacity, login, password);
                } catch (NumberFormatException e) {
                    System.out.println("capacity должно быть числом");
                    return null;
                }

            // фильтрация по типу (меньше указанного)
            case FILTER_LESS_THAN_TYPE:
                if (argument == null) {
                    System.out.println("Укажите тип");
                    return null;
                }
                try {
                    VehicleType vehicleType = InputValidator.validateAndParseVehicleType(argument);
                    return new Command(type, vehicleType, login, password);
                } catch (IllegalArgumentException e) {
                    System.out.println(e.getMessage());
                    return null;
                }

            // выполнение скрипта из файла
            case EXECUTE_SCRIPT:
                if (argument == null) {
                    System.out.println("Укажите имя файла скрипта");
                    return null;
                }
                return new Command(type, argument, login, password);

            // выход из программы
            case EXIT:
                return new Command(type, login, password);

            default:
                return null; // неизвестная команда
        }
    }

    // создает команду ADD: читает Vehicle и устанавливает владельца
    private Command createAddCommand() {
        System.out.println("Введите данные для добавления:");
        Vehicle vehicle = vehicleReader.readVehicle(); // читаем данные транспорта
        vehicle.setOwnerLogin(login); // устанавливаем владельца
        return new Command(CommandType.ADD, vehicle, login, password);
    }

    // создает команду UPDATE: читает новые данные для элемента с указанным id
    private Command createUpdateCommand(int id) {
        System.out.println("Введите новые данные для элемента с ID " + id + ":");
        Vehicle vehicle = vehicleReader.readVehicle(); // читаем новые данные
        vehicle.setOwnerLogin(login); // устанавливаем владельца
        return new Command(CommandType.UPDATE, new Object[]{id, vehicle}, login, password);
    }

    // создает команду REMOVE_LOWER: читает эталонный элемент для сравнения
    private Command createRemoveLowerCommand() {
        System.out.println("Введите эталонный элемент для сравнения:");
        Vehicle vehicle = vehicleReader.readVehicle(); // читаем эталонный транспорт
        vehicle.setOwnerLogin(login); // устанавливаем владельца
        return new Command(CommandType.REMOVE_LOWER, vehicle, login, password);
    }
}
