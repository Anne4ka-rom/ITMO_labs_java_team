package model; // класс находится в папке model

import java.time.LocalDate; // импорт класса LocalDate для работы с датами (дата создания транспортного средства)

/**
 * Класс, представляющий транспортное средство
 * Реализует Comparable для естественной сортировки по имени
 */
public class Vehicle implements Comparable<Vehicle> { // объявляем класс Vehicle, реализующий интерфейс Comparable
    private int id; // уникальный номер
    private String name; // имя
    private Coordinates coordinates; // координаты транспортного средства
    private LocalDate creationDate; // дата создания транспортного средства
    private Double enginePower; // мощность двигателя транспортного средства
    private double capacity; // грузоподъёмность транспортного средства
    private VehicleType type; // тип транспортного средства
    private FuelType fuelType; // тип топлива траспортного средства
    
    /**
     * Конструктор для создания нового транспортного средства
     * id генерируется автоматически, creationDate устанавливается автоматически
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
    
    public int getId() { // геттер для поля id
        return id; // возвращает уникальный идентификатор
    }

    public void setId(int id) { // сеттер для поля id с валидацией
        if (id <= 0) { // проверяем, что id положительный
            throw new IllegalArgumentException("ID должен быть больше 0"); // выбрасываем исключение, если id <= 0
        }
        this.id = id; // присваиваем значение после проверки
    }

    public String getName() { // геттер для поля name
        return name; // возвращает название транспортного средства
    }

    public void setName(String name) { // сеттер для поля name с валидацией
        if (name == null || name.trim().isEmpty()) { // проверка на null и пустую строку
            throw new IllegalArgumentException("Имя не может быть null или пустым"); // выбрасываем исключение, если имя не прошло валидацию
        }
        this.name = name; // присваиваем значение после проверки
    }

    public Coordinates getCoordinates() { // геттер для поля coordinates
        return coordinates; // возвращает объект Coordinates
    }

    public void setCoordinates(Coordinates coordinates) { // сеттер для поля coordinates с валидацией
        if (coordinates == null) { // проверка на null
            throw new IllegalArgumentException("Координаты не могут быть null"); // выбрасываем исключение, если координаты -- null
        }
        this.coordinates = coordinates; // присваиваем значение после проверки
    }

    public LocalDate getCreationDate() { // геттер для поля creationDate
        return creationDate; // возвращает дату создания
    }

    public void setCreationDate(LocalDate creationDate) { // сеттер для поля creationDate с валидацией
        if (creationDate == null) { // проверка на null
            throw new IllegalArgumentException("Дата создания не может быть null"); // выбрасываем исключение, если дата -- null
        }
        this.creationDate = creationDate; // присваиваем значение после проверки
    }

    public Double getEnginePower() { // геттер для поля enginePower
        return enginePower; // возвращает мощность двигателя
    }

    public void setEnginePower(Double enginePower) { // сеттер для поля enginePower с валидацией
        if (enginePower == null) { // проверка на null
            throw new IllegalArgumentException("Engine power не может быть null"); // выбрасываем исключение, если мощность -- null
        }
        if (enginePower <= 0) { // проверка на положительное значение
            throw new IllegalArgumentException("Engine power должен быть больше 0"); // выбрасываем исключение, если мощность <= 0
        }
        this.enginePower = enginePower; // присваиваем значение после проверок
    }

    public double getCapacity() { // геттер для поля capacity
        return capacity; // возвращает грузоподъемность
    }

    public void setCapacity(double capacity) { // сеттер для поля capacity с валидацией
        if (capacity <= 0) { // проверка на положительное значение
            throw new IllegalArgumentException("Capacity должна быть больше 0"); // выбрасываем исключение, если грузоподъёмность <= 0
        }
        this.capacity = capacity; // присваиваем значение после проверки
    }

    public VehicleType getType() { // геттер для поля type
        return type; // возвращает тип транспортного средства
    }

    public void setType(VehicleType type) { // сеттер для поля type с валидацией
        if (type == null) { // проверка на null
            throw new IllegalArgumentException("Тип не может быть null"); // выбрасываем исключение, если тип -- null
        }
        this.type = type; // присваиваем значение после проверки
    }

    public FuelType getFuelType() { // геттер для поля fuelType
        return fuelType; // возвращает тип топлива (может быть null)
    }

    public void setFuelType(FuelType fuelType) { // сеттер для поля fuelType
        this.fuelType = fuelType; // присваиваем значение после проверки
    }

    @Override // аннотация Override указывает, что метод переопределяет метод суперкласса (Object)
    public String toString() {
        return String.format("Vehicle{id=%d, name='%s', coordinates=%s, date=%s, " +
                "enginePower=%.2f, capacity=%.2f, type=%s, fuelType=%s}",
                id, name, coordinates, creationDate, enginePower, capacity, type, fuelType); // форматированный вывод всех полей объекта, где %d -- целое число, %s -- строка, %.2f -- число с двумя знаками после запятой
    }

    @Override // аннотация Override указывает на переопределение метода compareTo из интерфейса Comparable
    public int compareTo(Vehicle o) { // метод для сравнения объектов Vehicle при сортировке
        return this.name.compareTo(o.name); // сравниваем строки name: значение отрицательное, если this.name меньше; 0, если равны; положительное, если this.name больше
    }
}