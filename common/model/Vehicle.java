package common.model; // класс находится в пакете model в общей части

import java.io.Serializable; // импорт маркерного интерфейса для сериализации
import java.time.LocalDate; // импорт класса для работы с датами

/**
 * Класс, представляющий транспортное средство
 * Содержит все поля для описания транспортного средства и методы доступа к ним
 * Реализует Comparable для сравнения и Serializable для передачи по сети
 * 
 * @author Anni
 * @version 3.0
 */
public class Vehicle implements Comparable<Vehicle>, Serializable { // объявляет класс транспортного средства, реализующий сравнение и сериализацию
    private static final long serialVersionUID = 3L; // уникальный идентификатор версии класса для сериализации
    
    private int id; // уникальный идентификатор транспортного средства
    private String name; // название транспортного средства
    private Coordinates coordinates; // координаты транспортного средства
    private LocalDate creationDate; // дата создания записи
    private Double enginePower; // мощность двигателя (не может быть null)
    private double capacity; // вместимость транспортного средства
    private VehicleType type; // тип транспортного средства (не может быть null)
    private FuelType fuelType; // тип топлива (может быть null)
    private String ownerLogin; // логин владельца транспортного средства

    /**
     * Конструктор по умолчанию
     * Устанавливает текущую дату как дату создания
     */
    public Vehicle() { // конструктор по умолчанию
        this.creationDate = LocalDate.now(); // устанавливаем текущую дату
    }
    
    /**
     * Конструктор с параметрами для создания транспортного средства со всеми полями
     * 
     * @param name название транспортного средства
     * @param coordinates координаты транспортного средства
     * @param enginePower мощность двигателя
     * @param capacity вместимость
     * @param type тип транспортного средства
     * @param fuelType тип топлива (может быть null)
     * @param ownerLogin логин владельца
     */
    public Vehicle(String name, Coordinates coordinates, Double enginePower, 
                   double capacity, VehicleType type, FuelType fuelType, String ownerLogin) { // конструктор с параметрами
        setName(name); // устанавливаем имя с проверкой
        setCoordinates(coordinates); // устанавливаем координаты с проверкой
        setEnginePower(enginePower); // устанавливаем мощность с проверкой
        setCapacity(capacity); // устанавливаем вместимость с проверкой
        setType(type); // устанавливаем тип с проверкой
        setFuelType(fuelType); // устанавливаем тип топлива (без проверки, может быть null)
        this.creationDate = LocalDate.now(); // устанавливаем текущую дату
        this.ownerLogin = ownerLogin; // устанавливаем логин владельца
    }
    
    /**
     * Возвращает идентификатор транспортного средства
     * 
     * @return id транспортного средства
     */
    public int getId() { // геттер для поля id
        return id; // возвращаем id
    }

    /**
     * Устанавливает идентификатор транспортного средства
     * 
     * @param id идентификатор (должен быть больше 0)
     * @throws IllegalArgumentException если id <= 0
     */
    public void setId(int id) { // сеттер для поля id
        if (id <= 0) { // проверяем, что id положительный
            throw new IllegalArgumentException("ID должен быть больше 0"); // выбрасываем исключение
        }
        this.id = id; // сохраняем id
    }

    /**
     * Возвращает название транспортного средства
     * 
     * @return название транспортного средства
     */
    public String getName() { // геттер для поля name
        return name; // возвращаем имя
    }

    /**
     * Устанавливает название транспортного средства
     * 
     * @param name название (не может быть null или пустым)
     * @throws IllegalArgumentException если name == null или состоит только из пробелов
     */
    public void setName(String name) { // сеттер для поля name
        if (name == null || name.trim().isEmpty()) { // проверяем, что имя не null и не пустое после обрезки пробелов
            throw new IllegalArgumentException("Имя не может быть null или пустым"); // выбрасываем исключение
        }
        this.name = name; // сохраняем имя
    }

    /**
     * Возвращает координаты транспортного средства
     * 
     * @return объект Coordinates с координатами
     */
    public Coordinates getCoordinates() { // геттер для поля coordinates
        return coordinates; // возвращаем координаты
    }

    /**
     * Устанавливает координаты транспортного средства
     * 
     * @param coordinates объект Coordinates (не может быть null)
     * @throws IllegalArgumentException если coordinates == null
     */
    public void setCoordinates(Coordinates coordinates) { // сеттер для поля coordinates
        if (coordinates == null) { // проверяем, что координаты не null
            throw new IllegalArgumentException("Координаты не могут быть null"); // выбрасываем исключение
        }
        this.coordinates = coordinates; // сохраняем координаты
    }

    /**
     * Возвращает дату создания записи
     * 
     * @return дата создания
     */
    public LocalDate getCreationDate() { // геттер для поля creationDate
        return creationDate; // возвращаем дату создания
    }

    /**
     * Устанавливает дату создания записи
     * 
     * @param creationDate дата создания (не может быть null)
     * @throws IllegalArgumentException если creationDate == null
     */
    public void setCreationDate(LocalDate creationDate) { // сеттер для поля creationDate
        if (creationDate == null) { // проверяем, что дата не null
            throw new IllegalArgumentException("Дата создания не может быть null"); // выбрасываем исключение
        }
        this.creationDate = creationDate; // сохраняем дату
    }

    /**
     * Возвращает мощность двигателя
     * 
     * @return мощность двигателя (Double)
     */
    public Double getEnginePower() { // геттер для поля enginePower
        return enginePower; // возвращаем мощность
    }

    /**
     * Устанавливает мощность двигателя
     * 
     * @param enginePower мощность (не может быть null и должна быть больше 0)
     * @throws IllegalArgumentException если enginePower == null или enginePower <= 0
     */
    public void setEnginePower(Double enginePower) { // сеттер для поля enginePower
        if (enginePower == null) { // проверяем, что мощность не null
            throw new IllegalArgumentException("Engine power не может быть null"); // выбрасываем исключение
        }
        if (enginePower <= 0) { // проверяем, что мощность положительная
            throw new IllegalArgumentException("Engine power должен быть больше 0"); // выбрасываем исключение
        }
        this.enginePower = enginePower; // сохраняем мощность
    }

    /**
     * Возвращает вместимость транспортного средства
     * 
     * @return вместимость (double)
     */
    public double getCapacity() { // геттер для поля capacity
        return capacity; // возвращаем вместимость
    }

    /**
     * Устанавливает вместимость транспортного средства
     * 
     * @param capacity вместимость (должна быть больше 0)
     * @throws IllegalArgumentException если capacity <= 0
     */
    public void setCapacity(double capacity) { // сеттер для поля capacity
        if (capacity <= 0) { // проверяем, что вместимость положительная
            throw new IllegalArgumentException("Capacity должна быть больше 0"); // выбрасываем исключение
        }
        this.capacity = capacity; // сохраняем вместимость
    }

    /**
     * Возвращает тип транспортного средства
     * 
     * @return тип VehicleType
     */
    public VehicleType getType() { // геттер для поля type
        return type; // возвращаем тип
    }

    /**
     * Устанавливает тип транспортного средства
     * 
     * @param type тип (не может быть null)
     * @throws IllegalArgumentException если type == null
     */
    public void setType(VehicleType type) { // сеттер для поля type
        if (type == null) { // проверяем, что тип не null
            throw new IllegalArgumentException("Тип не может быть null"); // выбрасываем исключение
        }
        this.type = type; // сохраняем тип
    }

    /**
     * Возвращает тип топлива транспортного средства
     * 
     * @return тип FuelType или null если не указан
     */
    public FuelType getFuelType() { // геттер для поля fuelType
        return fuelType; // возвращаем тип топлива (может быть null)
    }

    /**
     * Устанавливает тип топлива транспортного средства
     * 
     * @param fuelType тип топлива (может быть null)
     */
    public void setFuelType(FuelType fuelType) { // сеттер для поля fuelType
        this.fuelType = fuelType; // сохраняем тип топлива (без проверки, может быть null)
    }

    /**
     * Возвращает логин владельца транспортного средства
     * 
     * @return логин владельца
     */
    public String getOwnerLogin() { // геттер для поля ownerLogin
        return ownerLogin; // возвращаем логин владельца
    }

    /**
     * Устанавливает логин владельца транспортного средства
     * 
     * @param ownerLogin логин владельца
     */
    public void setOwnerLogin(String ownerLogin) { // сеттер для поля ownerLogin
        this.ownerLogin = ownerLogin; // сохраняем логин владельца
    }

    /**
     * Возвращает строковое представление транспортного средства
     * 
     * @return строка со всеми полями транспортного средства
     */
    @Override
    public String toString() { // переопределённый метод строкового представления
        return String.format("Vehicle{id=%d, name='%s', coordinates=%s, date=%s, " + // форматируем строку с полями
                "enginePower=%.2f, capacity=%.2f, type=%s, fuelType=%s, owner='%s'}", // продолжаем форматирование
                id, name, coordinates, creationDate, enginePower, capacity, type, fuelType, ownerLogin); // подставляем значения
    }

    /**
     * Сравнивает два транспортных средства по имени
     * Используется для естественной сортировки
     * 
     * @param o другое транспортное средство для сравнения
     * @return отрицательное число, если текущее меньше, 0 если равны, положительное если больше
     */
    @Override
    public int compareTo(Vehicle o) { // переопределённый метод сравнения для сортировки
        return this.name.compareTo(o.name); // сравниваем имена лексикографически
    }
}