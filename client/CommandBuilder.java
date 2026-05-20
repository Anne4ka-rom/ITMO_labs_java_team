package client;

import common.*;
import common.model.*;

/**
 * Отвечает за разбор строки ввода и создание соответствующих объектов Command.
 * Фабричный слой между вводом пользователя и сетевым слоем.
 *
 * @author Polina
 * @version 1.1
 */
public class CommandBuilder {

    private final VehicleReader vehicleReader; // используется для чтения vehicle при командах add/update/remove_lower

    public CommandBuilder(VehicleReader vehicleReader) {
        this.vehicleReader = vehicleReader;
    }

    // создает объект Command на основе введенной строки
    public Command createCommand(String commandName, String argument) {
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

            // add_random - требует количество элементов
            case ADD_RANDOM:
                return new Command(type);

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
        Vehicle vehicle = vehicleReader.readVehicle();
        return new Command(CommandType.ADD, vehicle);
    }

    private Command createUpdateCommand(int id) { // создает команду update
        System.out.println("Введите новые данные для элемента с ID " + id + ":");
        Vehicle vehicle = vehicleReader.readVehicle();
        return new Command(CommandType.UPDATE, new Object[]{id, vehicle});
    }

    private Command createRemoveLowerCommand() { // создает команду remove_lower
        System.out.println("Введите эталонный элемент для сравнения:");
        Vehicle vehicle = vehicleReader.readVehicle();
        return new Command(CommandType.REMOVE_LOWER, vehicle);
    }
}