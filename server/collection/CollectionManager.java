package server.collection; // класс находится в пакете collection серверной части

import server.database.VehicleRepository;

import java.sql.SQLException;
import java.time.LocalDate; // импорт класса для работы с датами
import java.util.*; // импорт всех утилитных классов из java.util
import java.util.stream.Collectors; // импорт коллектора для stream api
import java.util.concurrent.locks.ReentrantReadWriteLock; // импорт для синхронизации потоков

import common.model.Vehicle; // импорт класса транспортного средства из общей модели
import common.model.VehicleType; // импорт перечисления типов транспортных средств из общей модели

/**
 * Управляет коллекцией объектов Vehicle
 * Обеспечивает основные операции добавления, удаления, обновления и поиска элементов
 * 
 * @author Anni
 * @version 3.0
 */
public class CollectionManager { // объявляем класс для управления коллекцией
    private Stack<Vehicle> collection; // хранилище элементов в виде стека
    private final LocalDate initializationDate; // дата создания менеджера коллекции
    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock(); // блокировка для синхронизации доступа к коллекции
    private final String currentUser; // имя текущего пользователя
    private final VehicleRepository vehicleRepository; // репозиторий для работы с бд

    /**
     * Создаёт новый менеджер коллекции
     * Инициализирует дату создания и загружает коллекцию из базы данных
     * 
     * @param vehicleRepository репозиторий для работы с бд
     * @param currentUser имя текущего пользователя
     */
    public CollectionManager(VehicleRepository vehicleRepository, String currentUser) { // конструктор класса
        this.collection = new Stack<>(); // инициализируем пустой стек
        this.initializationDate = LocalDate.now(); // запоминаем текущую дату как дату инициализации
        this.currentUser = currentUser; // сохраняем имя пользователя
        this.vehicleRepository = vehicleRepository; // сохраняем репозиторий
        loadFromDatabase(); // загружаем коллекцию из базы данных
    }

    /**
     * Возвращает информацию о коллекции
     * Включает тип коллекции, дату инициализации и количество элементов
     * 
     * @return строка с информацией о состоянии коллекции
     */
    public String getInfo() { // метод получения информации о коллекции
        lock.readLock().lock(); // захватываем блокировку на чтение
        try { // начало блока перехвата исключений
            return String.format("Тип коллекции: %s\nДата инициализации: %s\nКоличество элементов: %d\nПользователь: %s", // форматируем строку с параметрами
                    collection.getClass().getName(), initializationDate, collection.size(), currentUser); // подставляем тип, дату, размер и имя пользователя
        } finally { // блок, выполняющийся в любом случае
            lock.readLock().unlock(); // освобождаем блокировку
        }
    }

    /**
     * Возвращает отсортированный список всех элементов коллекции
     * Используется естественный порядок сортировки
     * 
     * @return новый список с отсортированными элементами
     */
    public List<Vehicle> getAllSorted() { // метод получения всех отсортированных элементов
        lock.readLock().lock(); // захватываем блокировку на чтение
        try { // начало блока перехвата исключений
            return collection.stream() // создаём поток из коллекции
                    .sorted() // сортируем элементы в естественном порядке
                    .collect(Collectors.toList()); // собираем результат в новый список
        } finally { // блок, выполняющийся в любом случае
            lock.readLock().unlock(); // освобождаем блокировку
        }
    }

    /**
     * Добавляет новый элемент в коллекцию
     * Автоматически генерирует уникальный id и устанавливает текущую дату создания
     * 
     * @param vehicle добавляемое транспортное средство
     * @throws SQLException если ошибка при работе с бд
     */
    public void add(Vehicle vehicle) throws SQLException { // метод добавления элемента
        lock.writeLock().lock(); // захватываем блокировку на запись
        try { // начало блока перехвата исключений
            vehicle.setCreationDate(LocalDate.now()); // устанавливаем текущую дату создания
            int newId = vehicleRepository.addVehicle(vehicle, currentUser); // сохраняем в бд и получаем новый id
            vehicle.setId(newId); // устанавливаем полученный id
            vehicle.setOwnerUsername(currentUser); // устанавливаем владельца
            collection.push(vehicle); // помещаем элемент в стек
            System.out.println("Элемент добавлен с ID: " + vehicle.getId()); // выводим сообщение с id добавленного элемента
        } finally { // блок, выполняющийся в любом случае
            lock.writeLock().unlock(); // освобождаем блокировку
        }
    }

    /**
     * Обновляет существующий элемент по его идентификатору
     * Сохраняет оригинальную дату создания, обновляя остальные поля
     * 
     * @param id идентификатор обновляемого элемента
     * @param newVehicle новый объект с обновлёнными данными
     * @return true если элемент найден и обновлён, false если элемент не существует
     * @throws SQLException если ошибка при работе с бд
     */
    public boolean updateById(int id, Vehicle newVehicle) throws SQLException { // метод обновления элемента по id
        lock.writeLock().lock(); // захватываем блокировку на запись
        try { // начало блока перехвата исключений
            if (!vehicleRepository.checkOwnership(id, currentUser)) { // проверяем, принадлежит ли элемент пользователю
                return false; // возвращаем неудачу - нет прав
            }
            Optional<Vehicle> optional = collection.stream() // создаём поток из коллекции
                    .filter(v -> v.getId() == id) // фильтруем элементы с нужным id
                    .findFirst(); // находим первый подходящий элемент
            if (optional.isPresent()) { // проверяем, найден ли элемент
                Vehicle old = optional.get(); // получаем существующий элемент
                int index = collection.indexOf(old); // находим его индекс в стеке
                newVehicle.setId(id); // устанавливаем старый id в новый элемент
                newVehicle.setCreationDate(old.getCreationDate()); // сохраняем оригинальную дату создания
                newVehicle.setOwnerUsername(currentUser); // устанавливаем владельца
                vehicleRepository.updateVehicle(id, newVehicle, currentUser); // обновляем запись в бд
                collection.set(index, newVehicle); // заменяем элемент в стеке по индексу
                return true; // возвращаем успех операции
            }
            return false; // возвращаем неудачу - элемент не найден
        } finally { // блок, выполняющийся в любом случае
            lock.writeLock().unlock(); // освобождаем блокировку
        }
    }

    /**
     * Удаляет элемент из коллекции по его идентификатору
     * 
     * @param id идентификатор удаляемого элемента
     * @return true если элемент удалён, false если элемент не найден
     * @throws SQLException если ошибка при работе с бд
     */
    public boolean removeById(int id) throws SQLException { // метод удаления элемента по id
        lock.writeLock().lock(); // захватываем блокировку на запись
        try { // начало блока перехвата исключений
            if (!vehicleRepository.checkOwnership(id, currentUser)) { // проверяем, принадлежит ли элемент пользователю
                return false; // возвращаем неудачу - нет прав
            }
            vehicleRepository.deleteVehicle(id, currentUser); // удаляем запись из бд
            return collection.removeIf(v -> v.getId() == id); // удаляем все элементы с указанным id и возвращаем результат
        } finally { // блок, выполняющийся в любом случае
            lock.writeLock().unlock(); // освобождаем блокировку
        }
    }

    /**
     * Полностью очищает коллекцию для текущего пользователя
     * Удаляет все его элементы без возможности восстановления
     * 
     * @throws SQLException если ошибка при работе с бд
     */
    public void clear() throws SQLException { // метод очистки коллекции
        lock.writeLock().lock(); // захватываем блокировку на запись
        try { // начало блока перехвата исключений
            vehicleRepository.clearUserVehicles(currentUser); // удаляем из бд все элементы пользователя
            collection.removeIf(v -> v.getOwnerUsername().equals(currentUser)); // удаляем из коллекции все элементы пользователя
            System.out.println("Коллекция очищена для пользователя " + currentUser); // выводим сообщение об очистке
        } finally { // блок, выполняющийся в любом случае
            lock.writeLock().unlock(); // освобождаем блокировку
        }
    }

    /**
     * Удаляет последний элемент текущего пользователя из коллекции (верхушку стека)
     * Если у пользователя нет элементов, ничего не делает
     * 
     * @throws SQLException если ошибка при работе с бд
     */
    public void removeLast() throws SQLException { // метод удаления последнего элемента
        lock.writeLock().lock(); // захватываем блокировку на запись
        try { // начало блока перехвата исключений
            Optional<Vehicle> lastUserVehicle = collection.stream() // создаём поток из коллекции
                    .filter(v -> v.getOwnerUsername().equals(currentUser)) // оставляем только элементы текущего пользователя
                    .reduce((first, second) -> second); // находим последний элемент через свёртку
            if (lastUserVehicle.isPresent()) { // проверяем, найден ли элемент
                int id = lastUserVehicle.get().getId(); // получаем id найденного элемента
                removeById(id); // удаляем элемент по id
                System.out.println("Последний элемент удален"); // выводим сообщение об удалении
            }
        } finally { // блок, выполняющийся в любом случае
            lock.writeLock().unlock(); // освобождаем блокировку
        }
    }

    /**
     * Удаляет все элементы текущего пользователя, которые меньше указанного транспортного средства
     * Сравнение выполняется через метод compareto
     * 
     * @param vehicle эталонное транспортное средство для сравнения
     * @return количество удалённых элементов
     * @throws SQLException если ошибка при работе с бд
     */
    public int removeLower(Vehicle vehicle) throws SQLException { // метод удаления меньших элементов
        lock.writeLock().lock(); // захватываем блокировку на запись
        try { // начало блока перехвата исключений
            List<Vehicle> toRemove = collection.stream() // создаём поток из коллекции
                    .filter(v -> v.getOwnerUsername().equals(currentUser)) // оставляем только элементы текущего пользователя
                    .filter(v -> v.compareTo(vehicle) < 0) // оставляем только меньшие эталонного
                    .collect(Collectors.toList()); // собираем результат в список
            int removed = 0; // счётчик удалённых элементов
            for (Vehicle v : toRemove) { // перебираем все элементы для удаления
                if (removeById(v.getId())) { // пытаемся удалить элемент по id
                    removed++; // увеличиваем счётчик при успехе
                }
            }
            return removed; // возвращаем количество удалённых элементов
        } finally { // блок, выполняющийся в любом случае
            lock.writeLock().unlock(); // освобождаем блокировку
        }
    }

    /**
     * Сортирует коллекцию в естественном порядке
     * Использует Stream API для сортировки и полностью обновляет стек
     */
    public void sort() { // метод сортировки коллекции
        lock.writeLock().lock(); // захватываем блокировку на запись
        try { // начало блока перехвата исключений
            List<Vehicle> sortedList = collection.stream() // создаём поток из коллекции
                    .sorted() // сортируем элементы в естественном порядке
                    .collect(Collectors.toList()); // собираем отсортированные элементы в список
            collection.clear(); // очищаем исходный стек
            collection.addAll(sortedList); // добавляем все отсортированные элементы обратно
            System.out.println("Коллекция отсортирована"); // выводим сообщение о завершении сортировки
        } finally { // блок, выполняющийся в любом случае
            lock.writeLock().unlock(); // освобождаем блокировку
        }
    }

    /**
     * Вычисляет сумму вместимости всех транспортных средств в коллекции
     * 
     * @return общая сумма всех значений capacity
     */
    public double getSumOfCapacity() { // метод получения суммы вместимостей
        lock.readLock().lock(); // захватываем блокировку на чтение
        try { // начало блока перехвата исключений
            return collection.stream() // создаём поток из коллекции
                    .mapToDouble(Vehicle::getCapacity) // преобразуем каждый элемент в его вместимость
                    .sum(); // вычисляем сумму всех значений
        } finally { // блок, выполняющийся в любом случае
            lock.readLock().unlock(); // освобождаем блокировку
        }
    }

    /**
     * Возвращает список транспортных средств с указанной вместимостью
     * Сравнение выполняется с учётом погрешности для чисел с плавающей точкой
     * 
     * @param capacity значение вместимости для фильтрации
     * @return список транспортных средств с заданной вместимостью
     */
    public List<Vehicle> filterByCapacity(double capacity) { // метод фильтрации по вместимости
        lock.readLock().lock(); // захватываем блокировку на чтение
        try { // начало блока перехвата исключений
            return collection.stream() // создаём поток из коллекции
                    .filter(v -> Math.abs(v.getCapacity() - capacity) < 0.0001) // фильтруем с погрешностью 0.0001
                    .collect(Collectors.toList()); // собираем результат в список
        } finally { // блок, выполняющийся в любом случае
            lock.readLock().unlock(); // освобождаем блокировку
        }
    }

    /**
     * Возвращает список транспортных средств, тип которых меньше указанного
     * Сравнение выполняется по порядковому номеру типа в перечислении
     * 
     * @param type эталонный тип для сравнения
     * @return список транспортных средств с меньшим типом
     */
    public List<Vehicle> filterLessThanType(VehicleType type) { // метод фильтрации по типу (меньше чем)
        lock.readLock().lock(); // захватываем блокировку на чтение
        try { // начало блока перехвата исключений
            return collection.stream() // создаём поток из коллекции
                    .filter(v -> v.getType().ordinal() < type.ordinal()) // фильтруем, где порядковый номер типа меньше
                    .collect(Collectors.toList()); // собираем результат в список
        } finally { // блок, выполняющийся в любом случае
            lock.readLock().unlock(); // освобождаем блокировку
        }
    }

    /**
     * Проверяет существование элемента с указанным идентификатором
     * 
     * @param id проверяемый идентификатор
     * @return true если элемент с таким id существует, false если нет
     */
    public boolean containsId(int id) { // метод проверки наличия id в коллекции
        lock.readLock().lock(); // захватываем блокировку на чтение
        try { // начало блока перехвата исключений
            return collection.stream().anyMatch(v -> v.getId() == id); // проверяем, есть ли хоть один элемент с таким id
        } finally { // блок, выполняющийся в любом случае
            lock.readLock().unlock(); // освобождаем блокировку
        }
    }

    /**
     * Проверяет, пуста ли коллекция
     * 
     * @return true если коллекция не содержит элементов, false если есть элементы
     */
    public boolean isEmpty() { // метод проверки на пустоту
        lock.readLock().lock(); // захватываем блокировку на чтение
        try { // начало блока перехвата исключений
            return collection.isEmpty(); // возвращаем результат проверки пустоты стека
        } finally { // блок, выполняющийся в любом случае
            lock.readLock().unlock(); // освобождаем блокировку
        }
    }
    
    /**
     * Возвращает количество элементов в коллекции
     * 
     * @return размер коллекции
     */
    public int size() { // метод получения размера коллекции
        lock.readLock().lock(); // захватываем блокировку на чтение
        try { // начало блока перехвата исключений
            return collection.size(); // возвращаем количество элементов в стеке
        } finally { // блок, выполняющийся в любом случае
            lock.readLock().unlock(); // освобождаем блокировку
        }
    }
    
    /**
     * Проверяет, принадлежит ли элемент с указанным id текущему пользователю
     * 
     * @param id идентификатор элемента
     * @return true если элемент принадлежит пользователю, false иначе
     */
    public boolean isOwner(int id) { // метод проверки принадлежности элемента
        lock.readLock().lock(); // захватываем блокировку на чтение
        try { // начало блока перехвата исключений
            return collection.stream() // создаём поток из коллекции
                    .filter(v -> v.getId() == id) // оставляем элемент с нужным id
                    .anyMatch(v -> v.getOwnerUsername().equals(currentUser)); // проверяем, совпадает ли владелец с текущим пользователем
        } finally { // блок, выполняющийся в любом случае
            lock.readLock().unlock(); // освобождаем блокировку
        }
    }
    
    /**
     * Возвращает имя текущего пользователя
     * 
     * @return имя пользователя
     */
    public String getCurrentUser() { // геттер для поля currentUser
        return currentUser; // возвращаем имя текущего пользователя
    }
    
    /**
     * Загружает коллекцию из базы данных
     * Используется при инициализации после авторизации
     * 
     * @throws SQLException если ошибка при загрузке из бд
     */
    public void loadFromDatabase() throws SQLException { // метод загрузки коллекции из бд
        lock.writeLock().lock(); // захватываем блокировку на запись
        try { // начало блока перехвата исключений
            collection = vehicleRepository.loadAllVehicles(); // загружаем все транспортные средства из бд
            System.out.println("Загружено " + collection.size() + " элементов из БД"); // выводим сообщение об успешной загрузке
        } finally { // блок, выполняющийся в любом случае
            lock.writeLock().unlock(); // освобождаем блокировку
        }
    }
}