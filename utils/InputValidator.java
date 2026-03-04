package utils;

import model.VehicleType;

import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * Класс для валидации пользовательского ввода.
 * Содержит статические методы для проверки всех полей транспортного средства на соответствие требованиям лабораторной работы.
 * При несоответствии требованиям выбрасывает IllegalArgumentException с описанием ошибки.
 *
 * @author Polina
 * @version 1.0
 */

// Валидация пользовательского ввода. Проверяет все поля на соответствие требованиям
public class InputValidator {

    /**
     * Проверяет корректность имени транспортного средства.
     * Имя не может быть null или пустым (в том числе состоять только из пробелов).
     *
     * @param name проверяемое имя
     * @throws IllegalArgumentException если имя null или пустое
     */

    // Проверяет имя
    public static void validateName(String name) {
        if (name == null || name.trim().isEmpty()) { // удаляет пробелы по краям и проверяет пустоту
            throw new IllegalArgumentException("Имя не может быть пустым");
        }
    }

    /**
     * Проверяет корректность мощности двигателя.
     * Мощность должна быть не null и строго больше 0.
     *
     * @param enginePower проверяемая мощность двигателя
     * @throws IllegalArgumentException если мощность null или меньше/равна 0
     */

    // Проверяет enginePower
    public static void validateEnginePower(Double enginePower) {
        if (enginePower == null) {
            throw new IllegalArgumentException("enginePower не может быть null");
        }
        if (enginePower <= 0) {
            throw new IllegalArgumentException("enginePower должен быть больше 0");
        }
    }

    /**
     * Проверяет корректность вместимости транспортного средства.
     * Вместимость должна быть строго больше 0.
     *
     * @param capacity проверяемая вместимость
     * @throws IllegalArgumentException если capacity меньше или равна 0
     */

    // Проверяет capacity
    public static void validateCapacity(double capacity) {
        if (capacity <= 0) {
            throw new IllegalArgumentException("capacity должна быть больше 0");
        }
    }

    /**
     * Проверяет корректность координаты X.
     * X не может быть null и не должен превышать 636.
     *
     * @param x проверяемая координата X
     * @throws IllegalArgumentException если X null или больше 636
     */

    // Проверяет координату X
    public static void validateCoordinateX(Double x) {
        if (x == null) {
            throw new IllegalArgumentException("X не может быть null");
        }
        if (x > 636) {
            throw new IllegalArgumentException("X должен быть <= 636");
        }
    }

    /**
     * Проверяет корректность строки с типом транспортного средства и преобразует её в enum VehicleType.
     * Строка не может быть null или пустой. Приводится к верхнему регистру для сравнения с enum.
     * Если тип не найден, формирует сообщение со списком доступных типов.
     *
     * @param input строка с названием типа
     * @return соответствующий элемент перечисления VehicleType
     * @throws IllegalArgumentException если строка null/пуста или тип не найден
     *
     * @see VehicleType
     */

    // Проверяет и преобразует строку в VehicleType
    public static VehicleType validateAndParseVehicleType(String input) {
        if (input == null || input.trim().isEmpty()) {
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

    /**
     * Проверяет корректность идентификатора.
     * ID должен быть положительным числом (больше 0).
     *
     * @param id проверяемый идентификатор
     * @throws IllegalArgumentException если id меньше или равен 0
     */

    // Проверяет ID
    public static void validateId(int id) {
        if (id <= 0) {
            throw new IllegalArgumentException("ID должен быть больше 0");
        }
    }
}