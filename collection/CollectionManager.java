package collection;

import model.Vehicle;
import model.VehicleType;
import file.FileManager;
import utils.IdGenerator;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

// Менеджер коллекции транспортных средств. Хранит коллекцию Stack<Vehicle> и управляет ею.
public class CollectionManager {
    private Stack<Vehicle> collection;
    private final LocalDate initializationDate;
    private final FileManager fileManager;

    public CollectionManager(FileManager fileManager) {
        this.collection = new Stack<>();
        this.initializationDate = LocalDate.now();
        this.fileManager = fileManager;
        loadCollection();
    }

    // Загружает коллекцию из файла
    private void loadCollection() {
        try {
            collection = fileManager.loadCollection();
            IdGenerator.updateLastId(collection);
            System.out.println("Загружено " + collection.size() + " элементов");
        } catch (Exception e) {
            System.err.println("Ошибка загрузки коллекции: " + e.getMessage());
            System.err.println("Будет создана пустая коллекция");
            collection = new Stack<>();
        }
    }

    // Возвращает информацию о коллекции
    public String getInfo() {
        return String.format("Тип коллекции: %s\nДата инициализации: %s\nКоличество элементов: %d",
                collection.getClass().getName(), initializationDate, collection.size());
    }

    // Выводит все элементы коллекции
    public void showAll() {
        if (collection.isEmpty()) {
            System.out.println("Коллекция пуста");
            return;
        }
        collection.forEach(System.out::println);
    }

    // Добавляет новый элемент в коллекцию
    public void add(Vehicle vehicle) {
        vehicle.setId(IdGenerator.generateId(collection));
        vehicle.setCreationDate(LocalDate.now());
        collection.push(vehicle);
        System.out.println("Элемент добавлен с ID: " + vehicle.getId());
    }

    // Обновляет элемент по ID
    public boolean updateById(int id, Vehicle newVehicle) {
        for (int i = 0; i < collection.size(); i++) {
            if (collection.get(i).getId() == id) {
                newVehicle.setId(id);
                newVehicle.setCreationDate(collection.get(i).getCreationDate());
                collection.set(i, newVehicle);
                return true;
            }
        }
        return false;
    }

    // Удаляет элемент по ID
    public boolean removeById(int id) {
        return collection.removeIf(v -> v.getId() == id);
    }

    // Очищает коллекцию
    public void clear() {
        collection.clear();
    }

    // Сохраняет коллекцию в файл
    public void save() throws Exception {
        fileManager.saveCollection(collection);
        System.out.println("Коллекция сохранена в файл");
    }

    // Удаляет последний элемент
    public void removeLast() {
        if (!collection.isEmpty()) {
            collection.pop();
            System.out.println("Последний элемент удален");
        } else {
            System.out.println("Коллекция пуста");
        }
    }

    // Удаляет все элементы, меньшие заданного
    public void removeLower(Vehicle vehicle) {
        int initialSize = collection.size();
        collection.removeIf(v -> v.compareTo(vehicle) < 0);
        System.out.println("Удалено элементов: " + (initialSize - collection.size()));
    }

    // Сортирует коллекцию
    public void sort() {
        Collections.sort(collection);
        System.out.println("Коллекция отсортирована");
    }

    // Возвращает сумму значений capacity
    public double getSumOfCapacity() {
        return collection.stream()
                .mapToDouble(Vehicle::getCapacity)
                .sum();
    }

    // Возвращает элементы с заданным capacity
    public List<Vehicle> filterByCapacity(double capacity) {
        return collection.stream()
                .filter(v -> Math.abs(v.getCapacity() - capacity) < 0.0001)
                .collect(Collectors.toList());
    }

    // Возвращает элементы с типом меньше заданного
    public List<Vehicle> filterLessThanType(VehicleType type) {
        return collection.stream()
                .filter(v -> v.getType().ordinal() < type.ordinal())
                .collect(Collectors.toList());
    }

    // Проверяет существование элемента с заданным ID
    public boolean containsId(int id) {
        return collection.stream().anyMatch(v -> v.getId() == id);
    }
}