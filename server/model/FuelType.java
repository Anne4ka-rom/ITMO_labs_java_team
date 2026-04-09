package server.model; // класс находится в папке model

/**
 * Enum типов топлива
 * Определяет возможные типы топлива для транспорта
 * Значение FuelType может быть null
 * 
 * @author Anni
 * @version 1.0
 */
public enum FuelType { // объявляем перечисление (enum) с константами типов топлива
    GASOLINE, // бензин
    KEROSENE, // керосин
    ALCOHOL, // спирт
    MANPOWER; // человеческая сила

    /**
     * Возвращает строку со всеми доступными типами топлива
     * 
     * @return строка с типами через запятую
     */
    public static String getTypes() { // объявляем публичный статический метод
            StringBuilder sb = new StringBuilder(); // инициализируем изменяемую строку для сборки результата
            for (FuelType type : values()) { // проходимся по всем значениям перечисления
                sb.append(type.name()).append(", "); // добавляем название топлива и разделитель в буфер
            }
            return sb.substring(0, sb.length() - 2); // удаляем лишние запятую и пробел в конце
        }
}