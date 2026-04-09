package server.utils; // класс находится в папке utils

import java.util.Collection; // импорт интерфейса Collection для работы с коллекциями

import server.model.Vehicle;

/**
 * Генератор уникальных ID для объектов Vehicle
 * Предоставляет методы для генерации и проверки уникальности идентификаторов
 * 
 * @author Anni
 * @version 1.0
 */
public class IdGenerator { // объявляем утилитный класс для генерации ID
    private static int lastId = 0; // хранит последний сгенерированный ID (для метода без параметров)

    /**
     * Генерирует новый уникальный ID на основе существующей коллекции
     * 
     * @param collection коллекция существующих Vehicle
     * @return новый уникальный ID (максимальный ID в коллекции + 1)
     */
    public static int generateId(Collection<Vehicle> collection) { // статический метод генерации ID по коллекции
        return collection.stream() // создаем поток из коллекции
                .mapToInt(Vehicle::getId) // преобразуем поток Vehicle в поток их ID
                .max() // находим максимальный ID в коллекции
                .orElse(0) + 1; // если коллекция пуста, берем 0, иначе берем максимум и добавляем 1
    }

    /**
     * Альтернативный генератор с сохранением последнего ID
     * 
     * @return новый уникальный ID (последний ID + 1)
     */
    public static int generateId() { // статический метод генерации ID без параметров
        return ++lastId; // увеличиваем lastId на 1 и возвращаем новое значение
    }

    /**
     * Обновляет lastId на основе существующей коллекции
     * Используется для синхронизации счетчика с уже существующими данными
     * 
     * @param collection коллекция существующих Vehicle
     */
    public static void updateLastId(Collection<Vehicle> collection) { // метод синхронизации счетчика
        lastId = collection.stream() // создаем поток из коллекции
                .mapToInt(Vehicle::getId) // преобразуем поток Vehicle в поток их ID
                .max() // находим максимальный ID
                .orElse(0); // если коллекция пуста, устанавливаем 0, иначе устанавливаем максимальный ID
    }

    /**
     * Проверяет уникальность ID
     * 
     * @param id проверяемый идентификатор
     * @param collection коллекция существующих Vehicle
     * @return true если ID уникален (не найден в коллекции), false если уже существует
     */
    public static boolean isIdUnique(int id, Collection<Vehicle> collection) { // метод проверки уникальности
        return collection.stream().noneMatch(v -> v.getId() == id); // создаем поток из коллекции и проверяем, что ни один Vehicle не имеет такого ID
    }
}