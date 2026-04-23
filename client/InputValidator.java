package client;

import common.model.VehicleType;

import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * Класс для валидации пользовательского ввода.
 * Содержит статические методы для проверки всех полей транспортного средства на соответствие требованиям лабораторной работы.
 * При несоответствии требованиям выбрасывает IllegalArgumentException с описанием ошибки.
 *
 * @author Polina
 * @version 2.0
 */

// валидация пользовательского ввода, проверяет все поля на соответствие требованиям
public class InputValidator {

    // проверяет корректность имени транспортного средства
    public static void validateName(String name) {
        if (name == null || name.trim().isEmpty()) { // удаляет пробелы по краям и проверяет пустоту
            throw new IllegalArgumentException("Имя не может быть пустым");
        }
    }

    // проверяет enginePower (корректность мощности двигателя
    public static void validateEnginePower(Double enginePower) {
        if (enginePower == null) {
            throw new IllegalArgumentException("enginePower не может быть null");
        }
        if (enginePower <= 0) {
            throw new IllegalArgumentException("enginePower должен быть больше 0");
        }
    }

    // проверяет capacity (корректность вместимости транспортного средства)
    public static void validateCapacity(double capacity) {
        if (capacity <= 0) {
            throw new IllegalArgumentException("capacity должна быть больше 0");
        }
    }

    // проверяет координату X
    public static void validateCoordinateX(Double x) {
        if (x == null) {
            throw new IllegalArgumentException("X не может быть null");
        }
        if (x > 636) {
            throw new IllegalArgumentException("X должен быть <= 636");
        }
    }

    // проверяет и преобразует строку в VehicleType
    public static VehicleType validateAndParseVehicleType(String input) {
        if (input == null || input.trim().isEmpty()) { // строка не может быть null
            throw new IllegalArgumentException("Тип не может быть пустым");
        }

        try {
            return VehicleType.valueOf(input.toUpperCase().trim()); // преобразование строки в enum
        } catch (IllegalArgumentException e) {
            String available = Arrays.stream(VehicleType.values()) // формируем список допустимых значений для подсказки
                    .map(Enum::name)
                    .collect(Collectors.joining(", "));
            throw new IllegalArgumentException("Тип должен быть одним из: " + available);
        }
    }

    // проверяет ID (должен быть положительным числом)
    public static void validateId(int id) {
        if (id <= 0) {
            throw new IllegalArgumentException("ID должен быть больше 0");
        }
    }
}
