package collection; // класс находится в папке collection

import model.Vehicle; // импорт класса Vehicle для работы с транспортными средствами
import model.VehicleType; // импорт перечисления VehicleType для работы с типами транспортного средства
import file.FileManager; // импорт класса FileManager для работы с файлами
import utils.IdGenerator; // импорт утилиты для генерации ID

import java.time.LocalDate; // импорт для работы с датами
import java.util.*; // импорт коллекций (Stack, Collections) и вспомогательных классов
import java.util.stream.Collectors; // импорт для сбора результатов Stream API

/**
 * Менеджер коллекции транспортных средств
 * Хранит коллекцию Stack<Vehicle> и предоставляет методы для управления ею:
 * добавление, удаление, обновление, фильтрация, сортировка и сохранение
 * 
 * @author Anni
 * @version 1.0
 * @see Vehicle
 * @see FileManager
 * @see IdGenerator
 */
public class CollectionManager { // объявляем класс для управления коллекцией
    private Stack<Vehicle> collection; // стек для хранения транспортных средств
    private final LocalDate initializationDate; // дата инициализации менеджера (final -- неизменяемая)
    private final FileManager fileManager; // менеджер файлов для сохранения/загрузки (final - неизменяемый)

    /**
     * Конструктор класса CollectionManager
     * Инициализирует пустую коллекцию, устанавливает текущую дату инициализации
     * и автоматически загружает существующую коллекцию из файла
     * 
     * @param fileManager менеджер файлов для работы с XML
     */
    public CollectionManager(FileManager fileManager) { // конструктор класса CollectionManager
        this.collection = new Stack<>(); // инициализируем пустой стек
        this.initializationDate = LocalDate.now(); // устанавливаем текущую дату как дату инициализации
        this.fileManager = fileManager; // сохраняем ссылку на FileManager
        loadCollection(); // загружаем коллекцию из файла при создании объекта
    }

    /**
     * Загружает коллекцию из файла
     * При успешной загрузке обновляет генератор ID
     * При ошибке создает пустую коллекцию
     */
    private void loadCollection() { // приватный метод загрузки коллекции из файла
        try { // блок try для обработки исключений при загрузке
            collection = fileManager.loadCollection(); // загружаем коллекцию через FileManager
            IdGenerator.updateLastId(collection); // обновляем генератор ID на основе загруженной коллекции
            System.out.println("Загружено " + collection.size() + " элементов"); // выводим информацию о количестве загруженных элементов
        } catch (Exception e) { // ловим любые исключения при загрузке
            System.err.println("Ошибка загрузки коллекции: " + e.getMessage()); // выводим сообщение об ошибке
            System.err.println("Будет создана пустая коллекция"); // информируем о создании пустой коллекции
            collection = new Stack<>(); // создаем пустую коллекцию в случае ошибки
        }
    }

    /**
     * Возвращает информацию о коллекции
     * 
     * @return строка, содержащая тип коллекции, дату инициализации и количество элементов
     */
    public String getInfo() { // метод для получения информации о коллекции
        return String.format("Тип коллекции: %s\nДата инициализации: %s\nКоличество элементов: %d",
                collection.getClass().getName(), initializationDate, collection.size()); // форматированный вывод информации
    }

    /**
     * Выводит все элементы коллекции в стандартный поток вывода
     * Если коллекция пуста, выводит соответствующее сообщение
     */
    public void showAll() { // метод для вывода всех элементов
        if (collection.isEmpty()) { // проверяем, пуста ли коллекция
            System.out.println("Коллекция пуста"); // выводим сообщение о пустой коллекции
            return; // выходим из метода
        }
        collection.forEach(System.out::println); // выводим каждый элемент коллекции через ссылку на метод
    }

    /**
     * Добавляет новый элемент в коллекцию
     * Автоматически генерирует ID и устанавливает текущую дату создания
     * 
     * @param vehicle объект Vehicle для добавления (без ID, генерируется автоматически)
     */
    public void add(Vehicle vehicle) { // метод добавления нового элемента
        vehicle.setId(IdGenerator.generateId(collection)); // генерируем новый ID на основе существующей коллекции
        vehicle.setCreationDate(LocalDate.now()); // устанавливаем текущую дату как дату создания
        collection.push(vehicle); // добавляем элемент в стек
        System.out.println("Элемент добавлен с ID: " + vehicle.getId()); // выводим подтверждение с ID добавленного элемента
    }

    /**
     * Обновляет элемент по ID
     * Заменяет существующий элемент новым, сохраняя оригинальный ID и дату создания
     * 
     * @param id идентификатор элемента для обновления
     * @param newVehicle новый объект Vehicle с обновленными данными
     * @return true, если элемент с указанным ID найден и обновлен; false в противном случае
     */
    public boolean updateById(int id, Vehicle newVehicle) { // метод обновления элемента по ID
        for (int i = 0; i < collection.size(); i++) { // проходим по всем индексам коллекции
            if (collection.get(i).getId() == id) { // если нашли элемент с нужным ID
                newVehicle.setId(id); // устанавливаем тот же ID для нового элемента
                newVehicle.setCreationDate(collection.get(i).getCreationDate()); // сохраняем оригинальную дату создания
                collection.set(i, newVehicle); // заменяем элемент на новую версию
                return true; // возвращаем true -- обновление успешно
            }
        }
        return false; // возвращаем false -- элемент с таким ID не найден
    }

    /**
     * Удаляет элемент по ID
     * 
     * @param id идентификатор элемента для удаления
     * @return true, если элемент был удален; false, если элемент с указанным ID не найден
     */
    public boolean removeById(int id) { // метод удаления элемента по ID
        return collection.removeIf(v -> v.getId() == id); // удаляем элементы, удовлетворяющие условию (ID совпадает), возвращаем true если что-то удалено
    }

    /**
     * Очищает коллекцию
     * Удаляет все элементы из коллекции
     */
    public void clear() { // метод очистки коллекции
        collection.clear(); // удаляем все элементы из стека
    }

    /**
     * Сохраняет коллекцию в файл
     * 
     * @throws Exception если произошла ошибка при сохранении в файл
     */
    public void save() throws Exception { // метод сохранения коллекции в файл
        fileManager.saveCollection(collection); // сохраняем коллекцию через FileManager
        System.out.println("Коллекция сохранена в файл"); // выводим подтверждение сохранения
    }

    /**
     * Удаляет последний элемент коллекции
     * Если коллекция пуста, выводит соответствующее сообщение
     */
    public void removeLast() { // метод удаления последнего элемента
        if (!collection.isEmpty()) { // проверяем, что коллекция не пуста
            collection.pop(); // удаляем верхний элемент стека (последний добавленный)
            System.out.println("Последний элемент удален"); // выводим подтверждение
        } else { // если коллекция пуста
            System.out.println("Коллекция пуста"); // выводим сообщение
        }
    }

    /**
     * Удаляет все элементы, меньшие заданного.
     * Сравнение производится с использованием метода {@link Vehicle#compareTo(Vehicle)}
     * 
     * @param vehicle эталонный объект для сравнения
     */
    public void removeLower(Vehicle vehicle) { // метод удаления элементов, меньших заданного
        int initialSize = collection.size(); // запоминаем исходный размер коллекции
        collection.removeIf(v -> v.compareTo(vehicle) < 0); // удаляем элементы, которые меньше переданного (compareTo < 0)
        System.out.println("Удалено элементов: " + (initialSize - collection.size())); // выводим количество удаленных элементов
    }

    /**
     * Сортирует коллекцию в естественном порядке (по имени)
     * Использует {@link Collections#sort(List)}.
     */
    public void sort() { // метод сортировки коллекции
        Collections.sort(collection); // сортируем коллекцию с использованием естественного порядка (по имени)
        System.out.println("Коллекция отсортирована"); // выводим подтверждение сортировки
    }

    /**
     * Возвращает сумму значений capacity всех элементов коллекции
     * 
     * @return сумма грузоподъемностей всех транспортных средств
     */
    public double getSumOfCapacity() { // метод для получения суммы грузоподъемностей
        return collection.stream() // создаем поток из коллекции
                .mapToDouble(Vehicle::getCapacity) // преобразуем поток Vehicle в поток double (значения capacity)
                .sum(); // суммируем все значения
    }

    /**
     * Возвращает список элементов с заданным значением capacity
     * Сравнение производится с учетом погрешности double
     * 
     * @param capacity значение грузоподъемности для фильтрации
     * @return список Vehicle, у которых capacity равен заданному
     */
    public List<Vehicle> filterByCapacity(double capacity) { // метод фильтрации по грузоподъемности
        return collection.stream() // создаем поток из коллекции
                .filter(v -> Math.abs(v.getCapacity() - capacity) < 0.0001) // оставляем элементы с capacity, равным заданному (с учетом погрешности double)
                .collect(Collectors.toList()); // собираем результат в список
    }

    /**
     * Возвращает список элементов, тип которых меньше заданного
     * Сравнение производится по порядковому номеру в перечислении {@link VehicleType}
     * 
     * @param type эталонный тип для сравнения
     * @return список Vehicle, у которых тип меньше заданного
     */
    public List<Vehicle> filterLessThanType(VehicleType type) { // метод фильтрации по типу (меньше заданного)
        return collection.stream() // создаем поток из коллекции
                .filter(v -> v.getType().ordinal() < type.ordinal()) // оставляем элементы, у которых порядковый номер типа меньше заданного
                .collect(Collectors.toList()); // собираем результат в список
    }

    /**
     * Проверяет существование элемента с заданным ID
     * 
     * @param id идентификатор для проверки
     * @return true, если элемент с указанным ID существует в коллекции; false в противном случае
     */
    public boolean containsId(int id) { // метод проверки существования ID
        return collection.stream().anyMatch(v -> v.getId() == id); // создаем поток из коллекции и возвращаем true, если хотя бы один элемент имеет заданный ID
    }
}