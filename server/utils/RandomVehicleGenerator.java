package server.utils; // класс находится в пакете utils серверной части

import common.model.*; // импорт всех классов модели из общей части
import java.util.Random; // импорт класса для генерации случайных чисел
import java.util.concurrent.ThreadLocalRandom; // импорт потокобезопасного генератора случайных чисел

/**
 * Генерирует случайные транспортные средства с реалистичными случайными параметрами
 * Используется для команды add_random и тестирования
 * 
 * @author Anni
 * @version 1.0
 */
public class RandomVehicleGenerator { // объявляет класс для генерации случайных транспортных средств

    private static final String[] NAME_PREFIXES = {"Vehicle", "Auto", "Car", "Truck", "Bike", "Boat", "Train", "Bus", "Van", "Scooter"}; // массив префиксов для генерации имени
    private static final String[] NAME_SUFFIXES = {"Alpha", "Beta", "Gamma", "Delta", "X", "Z", "Pro", "Lite", "Max", "Turbo"}; // массив суффиксов для генерации имени

    /**
     * Генерирует полностью заполненный объект Vehicle со случайными параметрами
     * Создаёт случайное имя, координаты, мощность двигателя, вместимость, тип и тип топлива
     * 
     * @return новый объект Vehicle со случайными параметрами
     */
    public static Vehicle generateRandomVehicle() { // статический метод генерации случайного транспортного средства
        Vehicle vehicle = new Vehicle(); // создаём новый пустой объект транспортного средства
        
        vehicle.setName(generateRandomName()); // генерируем и устанавливаем случайное имя
        
        Coordinates coords = generateRandomCoordinates(); // генерируем случайные координаты
        vehicle.setCoordinates(coords); // устанавливаем координаты в транспортное средство
        
        vehicle.setEnginePower(generateRandomEnginePower()); // генерируем и устанавливаем случайную мощность двигателя
        
        vehicle.setCapacity(generateRandomCapacity()); // генерируем и устанавливаем случайную вместимость
        
        vehicle.setType(generateRandomVehicleType()); // генерируем и устанавливаем случайный тип транспортного средства
        
        vehicle.setFuelType(generateRandomFuelType()); // генерируем и устанавливаем случайный тип топлива
        
        return vehicle; // возвращаем сгенерированное транспортное средство
    }

    /**
     * Генерирует случайное имя для транспортного средства
     * Формат имени: префикс + суффикс + число от 0 до 999
     * Пример: "VehicleAlpha42", "CarTurbo777"
     * 
     * @return строка со случайным именем
     */
    private static String generateRandomName() { // приватный метод генерации случайного имени
        Random random = ThreadLocalRandom.current(); // получаем потокобезопасный генератор случайных чисел
        String prefix = NAME_PREFIXES[random.nextInt(NAME_PREFIXES.length)]; // выбираем случайный префикс из массива
        String suffix = NAME_SUFFIXES[random.nextInt(NAME_SUFFIXES.length)]; // выбираем случайный суффикс из массива
        int number = random.nextInt(1000); // генерируем случайное число от 0 до 999
        return prefix + suffix + number; // объединяем префикс, суффикс и число в одно имя
    }

    /**
     * Генерирует случайные координаты для транспортного средства
     * X в диапазоне [0, 636), Y в диапазоне [-1000, 1000)
     * 
     * @return новый объект Coordinates со случайными значениями
     */
    private static Coordinates generateRandomCoordinates() { // приватный метод генерации случайных координат
        Random random = ThreadLocalRandom.current(); // получаем потокобезопасный генератор случайных чисел
        Coordinates coords = new Coordinates(); // создаём новый пустой объект координат
        
        Float x = (float) (random.nextDouble() * 636); // генерируем случайный x от 0 до 636 (исключая 636)
        coords.setX(x); // устанавливаем значение x
        
        float y = random.nextFloat() * 2000 - 1000; // генерируем случайный y от -1000 до 1000
        coords.setY(y); // устанавливаем значение y
        
        return coords; // возвращаем сгенерированные координаты
    }

    /**
     * Генерирует случайную мощность двигателя
     * Диапазон: от 0.1 до 5000.0
     * 
     * @return случайное значение мощности двигателя (Float)
     */
    private static Float generateRandomEnginePower() { // приватный метод генерации случайной мощности двигателя
        Random random = ThreadLocalRandom.current(); // получаем потокобезопасный генератор случайных чисел
        return 0.1f + random.nextFloat() * 5000; // возвращаем случайное число от 0.1 до 5000
    }

    /**
     * Генерирует случайную вместимость транспортного средства
     * Диапазон: от 1 до 100000
     * 
     * @return случайное значение вместимости (int)
     */
    private static int generateRandomCapacity() { // приватный метод генерации случайной вместимости
        Random random = ThreadLocalRandom.current(); // получаем потокобезопасный генератор случайных чисел
        return 1 + random.nextInt(100000); // возвращаем случайное число от 1 до 100000 (исключая 0)
    }

    /**
     * Генерирует случайный тип транспортного средства из доступных вариантов
     * Доступные типы: CAR, SUBMARINE, BICYCLE, HOVERBOARD
     * 
     * @return случайное значение VehicleType
     */
    private static VehicleType generateRandomVehicleType() { // приватный метод генерации случайного типа транспортного средства
        VehicleType[] types = {VehicleType.CAR, VehicleType.SUBMARINE, VehicleType.BICYCLE, VehicleType.HOVERBOARD}; // массив доступных типов транспортных средств
        Random random = ThreadLocalRandom.current(); // получаем потокобезопасный генератор случайных чисел
        return types[random.nextInt(types.length)]; // возвращаем случайный тип из массива
    }

    /**
     * Генерирует случайный тип топлива из доступных вариантов
     * Доступные типы: GASOLINE, KEROSENE, ALCOHOL, MANPOWER
     * С вероятностью 50% возвращает null (топливо не указано)
     * 
     * @return случайное значение FuelType или null
     */
    private static FuelType generateRandomFuelType() { // приватный метод генерации случайного типа топлива
        FuelType[] types = {FuelType.GASOLINE, FuelType.KEROSENE, FuelType.ALCOHOL, FuelType.MANPOWER}; // массив доступных типов топлива
        Random random = ThreadLocalRandom.current(); // получаем потокобезопасный генератор случайных чисел
        if (random.nextBoolean()) { // проверяем случайное булево значение (50% вероятность)
            return null; // возвращаем null (топливо не указано)
        }
        return types[random.nextInt(types.length)]; // возвращаем случайный тип топлива из массива
    }
}