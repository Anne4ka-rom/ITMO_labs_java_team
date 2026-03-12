package model; // класс находится в папке model

import java.time.LocalDate; // импорт класса LocalDate для работы с датами (дата создания транспортного средства)

/**
 * Класс, представляющий транспортное средство
 * Содержит всю необходимую информацию о транспортном средстве:
 * идентификатор, название, координаты, дату создания, мощность двигателя,
 * грузоподъемность, тип транспортного средства и тип топлива.
 * Реализует интерфейс {@link Comparable} для естественной сортировки по имени
 * 
 * @author Anni
 * @version 1.0
 * @see Coordinates
 * @see VehicleType
 * @see FuelType
 */
public class Vehicle implements Comparable<Vehicle> { // объявляем класс Vehicle, реализующий интерфейс Comparable
    private int id; // уникальный номер
    private String name; // имя
    private Coordinates coordinates; // координаты транспортного средства
    private LocalDate creationDate; // дата создания транспортного средства
    private Double enginePower; // мощность двигателя транспортного средства
    private double capacity; // грузоподъёмность транспортного средства
    private VehicleType type; // тип транспортного средства
    private FuelType fuelType; // тип топлива транспортного средства

    public Vehicle() { // конструктор по умолчанию
        this.creationDate = LocalDate.now(); // автоматически устанавливаем текущую дату как дату создания
    }
    
    /**
     * Конструктор для создания нового транспортного средства
     * Создает объект Vehicle с переданными параметрами
     * Дата создания устанавливается автоматически на текущую дату
     * Идентификатор должен быть установлен отдельно через {@link #setId(int)}
     * 
     * @param name название транспортного средства (не может быть null или пустым)
     * @param coordinates координаты транспортного средства (не могут быть null)
     * @param enginePower мощность двигателя (не может быть null, должна быть > 0)
     * @param capacity грузоподъемность (должна быть > 0)
     * @param type тип транспортного средства (не может быть null)
     * @param fuelType тип топлива (может быть null)
     * @throws IllegalArgumentException если любой из параметров не проходит валидацию
     */
    public Vehicle(String name, Coordinates coordinates, Double enginePower, 
                   double capacity, VehicleType type, FuelType fuelType) { // конструктор с параметрами для создания транспортного средства
        setName(name); // устанавливаем имя с валидацией
        setCoordinates(coordinates); // устанавливаем координаты с валидацией
        setEnginePower(enginePower); // устанавливаем мощность с валидацией
        setCapacity(capacity); // устанавливаем грузоподъемность с валидацией
        setType(type); // устанавливаем тип с валидацией
        setFuelType(fuelType); // устанавливаем тип топлива (может быть null)
        this.creationDate = LocalDate.now(); // автоматически устанавливаем текущую дату
    }
    
    /**
     * Возвращает уникальный идентификатор транспортного средства
     * 
     * @return идентификатор транспортного средства
     */
    public int getId() { // геттер для поля id
        return id; // возвращает уникальный идентификатор
    }

    /**
     * Устанавливает уникальный идентификатор транспортного средства
     * 
     * @param id новый идентификатор (должен быть больше 0)
     * @throws IllegalArgumentException если id <= 0
     */
    public void setId(int id) { // сеттер для поля id с валидацией
        if (id <= 0) { // проверяем, что id положительный
            throw new IllegalArgumentException("ID должен быть больше 0"); // выбрасываем исключение, если id <= 0
        }
        this.id = id; // присваиваем значение после проверки
    }

    /**
     * Возвращает название транспортного средства
     * 
     * @return название транспортного средства
     */
    public String getName() { // геттер для поля name
        return name; // возвращает название транспортного средства
    }

    /**
     * Устанавливает название транспортного средства
     * 
     * @param name новое название (не может быть null или пустым)
     * @throws IllegalArgumentException если name == null или name.trim().isEmpty()
     */
    public void setName(String name) { // сеттер для поля name с валидацией
        if (name == null || name.trim().isEmpty()) { // проверка на null и пустую строку
            throw new IllegalArgumentException("Имя не может быть null или пустым"); // выбрасываем исключение, если имя не прошло валидацию
        }
        this.name = name; // присваиваем значение после проверки
    }

    /**
     * Возвращает координаты транспортного средства
     * 
     * @return объект {@link Coordinates} с координатами транспортного средства
     */
    public Coordinates getCoordinates() { // геттер для поля coordinates
        return coordinates; // возвращает объект Coordinates
    }

    /**
     * Устанавливает координаты транспортного средства
     * 
     * @param coordinates новые координаты (не могут быть null)
     * @throws IllegalArgumentException если coordinates == null
     */
    public void setCoordinates(Coordinates coordinates) { // сеттер для поля coordinates с валидацией
        if (coordinates == null) { // проверка на null
            throw new IllegalArgumentException("Координаты не могут быть null"); // выбрасываем исключение, если координаты -- null
        }
        this.coordinates = coordinates; // присваиваем значение после проверки
    }

    /**
     * Возвращает дату создания записи о транспортном средстве
     * 
     * @return дата создания (не может быть null)
     */
    public LocalDate getCreationDate() { // геттер для поля creationDate
        return creationDate; // возвращает дату создания
    }

    /**
     * Устанавливает дату создания транспортного средства
     * Обычно дата устанавливается автоматически при создании объекта
     * и не требует ручного изменения.
     * 
     * @param creationDate новая дата создания (не может быть null)
     * @throws IllegalArgumentException если creationDate == null
     */
    public void setCreationDate(LocalDate creationDate) { // сеттер для поля creationDate с валидацией
        if (creationDate == null) { // проверка на null
            throw new IllegalArgumentException("Дата создания не может быть null"); // выбрасываем исключение, если дата -- null
        }
        this.creationDate = creationDate; // присваиваем значение после проверки
    }

    /**
     * Возвращает мощность двигателя транспортного средства
     * 
     * @return мощность двигателя (не может быть null)
     */
    public Double getEnginePower() { // геттер для поля enginePower
        return enginePower; // возвращает мощность двигателя
    }

    /**
     * Устанавливает мощность двигателя транспортного средства
     * 
     * @param enginePower новая мощность (не может быть null, должна быть > 0)
     * @throws IllegalArgumentException если enginePower == null
     * @throws IllegalArgumentException если enginePower <= 0
     */
    public void setEnginePower(Double enginePower) { // сеттер для поля enginePower с валидацией
        if (enginePower == null) { // проверка на null
            throw new IllegalArgumentException("Engine power не может быть null"); // выбрасываем исключение, если мощность -- null
        }
        if (enginePower <= 0) { // проверка на положительное значение
            throw new IllegalArgumentException("Engine power должен быть больше 0"); // выбрасываем исключение, если мощность <= 0
        }
        this.enginePower = enginePower; // присваиваем значение после проверок
    }

    /**
     * Возвращает грузоподъемность транспортного средства
     * 
     * @return грузоподъемность (всегда > 0)
     */
    public double getCapacity() { // геттер для поля capacity
        return capacity; // возвращает грузоподъемность
    }

    /**
     * Устанавливает грузоподъемность транспортного средства
     * 
     * @param capacity новая грузоподъемность (должна быть > 0)
     * @throws IllegalArgumentException если capacity <= 0
     */
    public void setCapacity(double capacity) { // сеттер для поля capacity с валидацией
        if (capacity <= 0) { // проверка на положительное значение
            throw new IllegalArgumentException("Capacity должна быть больше 0"); // выбрасываем исключение, если грузоподъёмность <= 0
        }
        this.capacity = capacity; // присваиваем значение после проверки
    }

    /**
     * Возвращает тип транспортного средства
     * 
     * @return тип транспортного средства (не может быть null)
     * @see VehicleType
     */
    public VehicleType getType() { // геттер для поля type
        return type; // возвращает тип транспортного средства
    }

    /**
     * Устанавливает тип транспортного средства
     * 
     * @param type новый тип (не может быть null)
     * @throws IllegalArgumentException если type == null
     * @see VehicleType
     */
    public void setType(VehicleType type) { // сеттер для поля type с валидацией
        if (type == null) { // проверка на null
            throw new IllegalArgumentException("Тип не может быть null"); // выбрасываем исключение, если тип -- null
        }
        this.type = type; // присваиваем значение после проверки
    }

    /**
     * Возвращает тип топлива транспортного средства
     * 
     * @return тип топлива (может быть null)
     * @see FuelType
     */
    public FuelType getFuelType() { // геттер для поля fuelType
        return fuelType; // возвращает тип топлива (может быть null)
    }

    /**
     * Устанавливает тип топлива транспортного средства
     * 
     * @param fuelType новый тип топлива (может быть null)
     * @see FuelType
     */
    public void setFuelType(FuelType fuelType) { // сеттер для поля fuelType
        this.fuelType = fuelType; // присваиваем значение после проверки
    }

    /**
     * Возвращает строковое представление объекта Vehicle
     * Формат вывода:
     * Vehicle{id=1, name='Car', coordinates=(10.5, 20), date=2024-01-15,
     * enginePower=150.50, capacity=2000.00, type=CAR, fuelType=GASOLINE}
     * 
     * @return строковое представление транспортного средства
     */
    @Override // аннотация Override указывает, что метод переопределяет метод суперкласса (Object)
    public String toString() {
        return String.format("Vehicle{id=%d, name='%s', coordinates=%s, date=%s, " +
                "enginePower=%.2f, capacity=%.2f, type=%s, fuelType=%s}",
                id, name, coordinates, creationDate, enginePower, capacity, type, fuelType); // форматированный вывод всех полей объекта, где %d -- целое число, %s -- строка, %.2f -- число с двумя знаками после запятой
    }

    /**
     * Сравнивает текущий объект Vehicle с другим объектом Vehicle по имени
     * Используется для естественной сортировки транспортных средств по алфавиту
     * 
     * @param o другой объект Vehicle для сравнения
     * @return отрицательное число, если текущий объект меньше (по алфавиту);
     *         0, если объекты равны;
     *         положительное число, если текущий объект больше
     */
    @Override // аннотация Override указывает на переопределение метода compareTo из интерфейса Comparable
    public int compareTo(Vehicle o) { // метод для сравнения объектов Vehicle при сортировке
        return this.name.compareTo(o.name); // сравниваем строки name: значение отрицательное, если this.name меньше; 0, если равны; положительное, если this.name больше
    }
}