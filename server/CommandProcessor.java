package server; // объявление пакета server, где находится класс для обработки команд

import common.*; // импорт всех классов из пакета common 
import server.collection.CollectionManager; // импорт менеджера коллекции для управления данными
import server.model.Vehicle; // импорт класса Vehicle
import server.model.VehicleType; // импорт перечисления типов транспортных средств

/**
 * Модуль обработки полученных команд
 * 
 * Содержит логику выполнения всех команд с использованием Stream API
 * Класс является центральным элементом обработки запросов от клиента
 * 
 * Каждая команда обрабатывается отдельным приватным методом
 * Основной метод {@link #process(Command)} определяет тип команды и делегирует выполнение соответствующему обработчику
 * 
 * Все методы возвращают объект {@link Response}, который содержит статус выполнения, текстовое сообщение и опционально данные
 * 
 * @author Anni
 * @version 2.0
 * @see Command
 * @see Response
 * @see CollectionManager
 */
public class CommandProcessor { // объявление класса CommandProcessor
    private final CollectionManager collectionManager; // финальное поле менеджера коллекции (хранит ссылку на управляющий объект)
    
    /**
     * Конструктор процессора команд
     * 
     * Инициализирует процессор команд, сохраняя ссылку на менеджер коллекции
     * Менеджер коллекции содержит все данные и методы для работы с ними
     * 
     * @param collectionManager менеджер коллекции (управляет хранением и операциями с Vehicle)
     */
    public CommandProcessor(CollectionManager collectionManager) { // конструктор класса, принимает менеджер коллекции
        this.collectionManager = collectionManager; // сохранение менеджера коллекции в поле класса
    }
    
    /**
     * Обрабатывает команду и возвращает ответ
     * 
     * Основной метод класса, который принимает команду от клиента, определяет её тип и вызывает соответствующий приватный метод-обработчик
     * 
     * При неизвестной команде возвращает ответ с ошибкой
     * Команда EXIT обрабатывается отдельно без вызова дополнительных методов
     * 
     * @param command команда от клиента (содержит тип и аргументы)
     * @return ответ сервера (статус, сообщение, опционально данные)
     * @see Command
     * @see Response
     */
    public Response process(Command command) { // метод обработки команды, принимает объект Command, возвращает Response
        CommandType type = command.getType(); // получение типа команды из объекта command
        
        System.out.println("Обработка команды: " + type); // вывод в консоль информации о начале обработки команды
        
        switch (type) { // оператор выбора в зависимости от типа команды
            case HELP: // если команда HELP (справка)
                return processHelp(); // вызов метода обработки справки и возврат результата
            case INFO: // если команда INFO (информация о коллекции)
                return processInfo(); // вызов метода получения информации о коллекции
            case SHOW: // если команда SHOW (показать все элементы)
                return processShow(); // вызов метода отображения всех элементов
            case ADD: // если команда ADD (добавить элемент)
                return processAdd(command); // вызов метода добавления элемента с передачей команды
            case UPDATE: // если команда UPDATE (обновить элемент)
                return processUpdate(command); // вызов метода обновления элемента с передачей команды
            case REMOVE_BY_ID: // если команда REMOVE_BY_ID (удалить по id)
                return processRemoveById(command); // вызов метода удаления по id с передачей команды
            case CLEAR: // если команда CLEAR (очистить коллекцию)
                return processClear(); // вызов метода очистки коллекции
            case REMOVE_LAST: // если команда REMOVE_LAST (удалить последний элемент)
                return processRemoveLast(); // вызов метода удаления последнего элемента
            case REMOVE_LOWER: // если команда REMOVE_LOWER (удалить меньшие элементы)
                return processRemoveLower(command); // вызов метода удаления элементов, меньших заданного
            case SORT: // если команда SORT (отсортировать коллекцию)
                return processSort(); // вызов метода сортировки коллекции
            case SUM_OF_CAPACITY: // если команда SUM_OF_CAPACITY (сумма грузоподъемностей)
                return processSumOfCapacity(); // вызов метода подсчёта суммы грузоподъёмностей
            case FILTER_BY_CAPACITY: // если команда FILTER_BY_CAPACITY (фильтр по грузоподъёмности)
                return processFilterByCapacity(command); // вызов метода фильтрации по грузоподъёмности
            case FILTER_LESS_THAN_TYPE: // если команда FILTER_LESS_THAN_TYPE (фильтр по типу)
                return processFilterLessThanType(command); // вызов метода фильтрации по типу (меньше заданного)
            case SAVE: // если команда SAVE (сохранить коллекцию)
                return processSave(); // вызов метода сохранения коллекции в файл
            case EXIT: // если команда EXIT (завершить работу клиента)
                return new Response(ResponseStatus.SUCCESS, "До свидания!"); // создание ответа с успешным статусом и прощальным сообщением
            default: // если тип команды не распознан
                return new Response(ResponseStatus.ERROR, // создание ответа с ошибкой
                    "Неизвестная команда: " + type); // сообщение с неизвестным типом команды
        }
    }
    
    /**
     * Обработка команды HELP
     * 
     * Возвращает справочную информацию со списком всех доступных команд и кратким описанием их назначения
     * 
     * @return ответ со статусом SUCCESS и текстом справки
     */
    private Response processHelp() { // метод обработки команды help (вывод справки)
        String helpText = "Доступные команды:\n" +
            "  help - вывести справку\n" +
            "  info - информация о коллекции\n" +
            "  show - вывести все элементы\n" +
            "  add - добавить новый элемент\n" +
            "  update <id> - обновить элемент по ID\n" +
            "  remove_by_id <id> - удалить элемент по ID\n" +
            "  clear - очистить коллекцию\n" +
            "  remove_last - удалить последний элемент\n" +
            "  remove_lower - удалить элементы, меньшие заданного\n" +
            "  sort - отсортировать коллекцию\n" +
            "  sum_of_capacity - сумма грузоподъемностей\n" +
            "  filter_by_capacity <capacity> - фильтр по грузоподъемности\n" +
            "  filter_less_than_type <type> - фильтр по типу\n" +
            "  exit - завершить работу клиента"; // формирование строки со списком команд
        
        return new Response(ResponseStatus.SUCCESS, helpText); // возврат ответа с успешным статусом и текстом справки
    }
    
    /**
     * Обработка команды INFO
     * 
     * Возвращает информацию о коллекции: тип, дата инициализации, количество элементов
     * Данные получаются через {@link CollectionManager#getInfo()}
     * 
     * @return ответ со статусом SUCCESS и информацией о коллекции
     */
    private Response processInfo() { // метод обработки команды info (информация о коллекции)
        String info = collectionManager.getInfo(); // получение информации о коллекции от менеджера
        return new Response(ResponseStatus.SUCCESS, info); // возврат ответа с успешным статусом и информацией
    }
    
    /**
     * Обработка команды SHOW
     * 
     * Возвращает все элементы коллекции в виде строки
     * Если коллекция пуста, возвращает соответствующее сообщение
     * Использует {@link CollectionManager#getAllSorted()} для получения отсортированных элементов с помощью Stream API
     */
    private Response processShow() { // метод обработки команды show (показать все элементы)
        if (collectionManager.isEmpty()) { // проверка, пуста ли коллекция
            return new Response(ResponseStatus.SUCCESS, "Коллекция пуста"); // возврат ответа с сообщением о пустоте
        }
        
        var vehicles = collectionManager.getAllSorted(); // получение всех элементов коллекции в отсортированном порядке
        StringBuilder sb = new StringBuilder(); // создание строителя строк для формирования ответа
        for (Vehicle v : vehicles) { // цикл по всем транспортным средствам
            sb.append(v).append("\n"); // добавление строкового представления элемента и переноса строки
        }
        return new Response(ResponseStatus.SUCCESS, sb.toString(), vehicles); // возврат ответа с успешным статусом, текстом и списком элементов
    }
    
    /**
     * Обработка команды ADD
     * 
     * Добавляет новый элемент в коллекцию
     * Аргумент команды должен быть объектом {@link Vehicle}
     * 
     * @param command команда с аргументом Vehicle
     * @return ответ со статусом SUCCESS при успехе или ERROR при ошибке
     */
    private Response processAdd(Command command) { // метод обработки команды add (добавить элемент)
        try { // начало блока перехвата исключений
            Vehicle vehicle = (Vehicle) command.getArgument(); // получение аргумента команды и приведение к типу Vehicle
            collectionManager.add(vehicle); // добавление транспортного средства в коллекцию
            return new Response(ResponseStatus.SUCCESS, // возврат ответа с успешным статусом
                "Элемент добавлен с ID: " + vehicle.getId());  // сообщение с id добавленного элемента
        } catch (Exception e) { // обработка любых исключений
            return new Response(ResponseStatus.ERROR, // возврат ответа с ошибкой
                "Ошибка добавления: " + e.getMessage()); // сообщение с деталями ошибки
        }
    }
    
    /**
     * Обработка команды UPDATE
     * 
     * Обновляет существующий элемент по ID
     * Аргумент команды должен быть массивом Object[]: [id, newVehicle]
     * 
     * @param command команда с аргументами [id, Vehicle]
     * @return ответ со статусом SUCCESS при успехе или ERROR при ошибке
     */
    private Response processUpdate(Command command) { // метод обработки команды update (обновить элемент по id)
        try { // начало блока перехвата исключений
            Object[] args = (Object[]) command.getArgument(); // получение аргумента команды и приведение к массиву Object
            int id = (int) args[0]; // извлечение id из первого элемента массива (приведение к int)
            Vehicle newVehicle = (Vehicle) args[1]; // извлечение нового транспортного средства из второго элемента массива
            
            if (!collectionManager.containsId(id)) { // проверка существования элемента с указанным id
                return new Response(ResponseStatus.ERROR, // возврат ответа с ошибкой
                    "Элемент с ID " + id + " не найден"); // сообщение о том, что элемент не найден
            }
            
            collectionManager.updateById(id, newVehicle); // обновление элемента с указанным id
            return new Response(ResponseStatus.SUCCESS, // возврат ответа с успешным статусом
                "Элемент с ID " + id + " обновлен"); // сообщение об успешном обновлении
        } catch (Exception e) { // обработка любых исключений
            return new Response(ResponseStatus.ERROR, // возврат ответа с ошибкой
                "Ошибка обновления: " + e.getMessage()); // сообщение с деталями ошибки
        }
    }
    
    /**
     * Обработка команды REMOVE_BY_ID
     * 
     * Удаляет элемент из коллекции по указанному ID
     * Аргумент команды должен быть целым числом (int)
     * 
     * @param command команда с аргументом id
     * @return ответ со статусом SUCCESS при успехе или ERROR при ошибке
     */
    private Response processRemoveById(Command command) { // метод обработки команды remove_by_id (удалить по id)
        try { // начало блока перехвата исключений
            int id = (int) command.getArgument(); // получение аргумента команды и приведение к int
            
            if (collectionManager.removeById(id)) { // попытка удаления элемента (true - успех, false - не найден)
                return new Response(ResponseStatus.SUCCESS, // возврат ответа с успешным статусом
                    "Элемент с ID " + id + " удален"); // сообщение об успешном удалении
            } else { // если элемент не найден
                return new Response(ResponseStatus.ERROR, // возврат ответа с ошибкой
                    "Элемент с ID " + id + " не найден"); // сообщение о том, что элемент не найден
            }
        } catch (Exception e) { // обработка любых исключений
            return new Response(ResponseStatus.ERROR, // возврат ответа с ошибкой
                "Ошибка удаления: " + e.getMessage()); // сообщение с деталями ошибки
        }
    }
    
    /**
     * Обработка команды CLEAR
     * 
     * Полностью очищает коллекцию, удаляя все элементы
     * 
     * @return ответ со статусом SUCCESS и сообщением об очистке
     */
    private Response processClear() { // метод обработки команды clear (очистить коллекцию)
        collectionManager.clear(); // вызов метода очистки коллекции у менеджера
        return new Response(ResponseStatus.SUCCESS, "Коллекция очищена"); // возврат ответа с успешным статусом и сообщением
    }
    
    /**
     * Обработка команды REMOVE_LAST
     * 
     * Удаляет последний элемент из коллекции
     * Если коллекция пуста, возвращает ошибку
     * 
     * @return ответ со статусом SUCCESS при успехе или ERROR при пустой коллекции
     */
    private Response processRemoveLast() { // метод обработки команды remove_last (удалить последний элемент)
        if (collectionManager.isEmpty()) { // проверка, пуста ли коллекция
            return new Response(ResponseStatus.ERROR, "Коллекция пуста"); // возврат ответа с ошибкой
        }
        collectionManager.removeLast(); // вызов метода удаления последнего элемента
        return new Response(ResponseStatus.SUCCESS, "Последний элемент удален"); // возврат ответа с успешным статусом
    }
    
    /**
     * Обработка команды REMOVE_LOWER
     * 
     * Удаляет все элементы, меньшие заданного
     * Использует Stream API через {@link CollectionManager#removeLower(Vehicle)}
     * 
     * @param command команда с аргументом Vehicle (эталон для сравнения)
     * @return ответ со статусом SUCCESS и количеством удалённых элементов
     */
    private Response processRemoveLower(Command command) { // метод обработки команды remove_lower (удалить элементы, меньшие заданного)
        try { // начало блока перехвата исключений
            Vehicle vehicle = (Vehicle) command.getArgument(); // получение аргумента команды и приведение к типу Vehicle
            int removedCount = collectionManager.removeLower(vehicle); // удаление элементов, меньших заданного, получение количества удалённых
            return new Response(ResponseStatus.SUCCESS, // возврат ответа с успешным статусом
                "Удалено элементов: " + removedCount); // сообщение с количеством удалённых элементов
        } catch (Exception e) { // обработка любых исключений
            return new Response(ResponseStatus.ERROR, // возврат ответа с ошибкой
                "Ошибка удаления: " + e.getMessage()); // сообщение с деталями ошибки
        }
    }
    
    /**
     * Обработка команды SORT
     * 
     * Сортирует коллекцию с использованием естественного порядка (Comparable)
     * 
     * @return ответ со статусом SUCCESS и сообщением о сортировке
     */
    private Response processSort() { // метод обработки команды sort (отсортировать коллекцию)
        collectionManager.sort(); // вызов метода сортировки коллекции у менеджера
        return new Response(ResponseStatus.SUCCESS, "Коллекция отсортирована"); // возврат ответа с успешным статусом
    }
    
    /**
     * Обработка команды SUM_OF_CAPACITY
     * 
     * Вычисляет сумму грузоподъёмностей всех элементов коллекции
     * Использует Stream API: {@link CollectionManager#getSumOfCapacity()} с применением {@code mapToDouble().sum()}
     * 
     * @return ответ со статусом SUCCESS и вычисленной суммой
     */
    private Response processSumOfCapacity() { // метод обработки команды sum_of_capacity (сумма грузоподъёмностей)
        double sum = collectionManager.getSumOfCapacity(); // получение суммы грузоподъёмностей от менеджера
        return new Response(ResponseStatus.SUCCESS, // возврат ответа с успешным статусом
            "Сумма грузоподъемностей: " + sum); // сообщение с вычисленной суммой
    }
    
    /**
     * Обработка команды FILTER_BY_CAPACITY
     * 
     * Фильтрует элементы коллекции по заданной грузоподъёмности
     * Использует Stream API: {@link CollectionManager#filterByCapacity(double)} с применением {@code filter().collect()}
     * 
     * @param command команда с аргументом capacity (double)
     * @return ответ со статусом SUCCESS, текстовым представлением и отфильтрованным списком
     */
    private Response processFilterByCapacity(Command command) { // метод обработки команды filter_by_capacity (фильтр по грузоподъёмности)
        try { // начало блока перехвата исключений
            double capacity = (double) command.getArgument(); // получение аргумента команды и приведение к double
            var filtered = collectionManager.filterByCapacity(capacity); // получение отфильтрованных элементов по грузоподъёмности
            
            if (filtered.isEmpty()) { // проверка, пуст ли результат фильтрации
                return new Response(ResponseStatus.SUCCESS, // возврат ответа с успешным статусом
                    "Элементы с capacity " + capacity + " не найдены"); // сообщение о том, что элементы не найдены
            }
            
            StringBuilder sb = new StringBuilder(); // создание строителя строк для формирования ответа
            for (Vehicle v : filtered) { // цикл по отфильтрованным элементам
                sb.append(v).append("\n"); // добавление строкового представления элемента и переноса строки
            }
            return new Response(ResponseStatus.SUCCESS, sb.toString(), filtered); // возврат ответа с успешным статусом, текстом и списком
        } catch (Exception e) { // обработка любых исключений
            return new Response(ResponseStatus.ERROR, // возврат ответа с ошибкой
                "Ошибка фильтрации: " + e.getMessage()); // сообщение с деталями ошибки
        }
    }
    
    /**
     * Обработка команды FILTER_LESS_THAN_TYPE
     * 
     * Фильтрует элементы коллекции по типу, оставляя только те, у которых тип меньше заданного (в порядке enum)
     * Использует Stream API: {@link CollectionManager#filterLessThanType(VehicleType)}
     * 
     * @param command команда с аргументом VehicleType
     * @return ответ со статусом SUCCESS, текстовым представлением и отфильтрованным списком
     */
    private Response processFilterLessThanType(Command command) { // метод обработки команды filter_less_than_type (фильтр по типу, меньше заданного)
        try {
            VehicleType type = (VehicleType) command.getArgument(); // получение аргумента команды и приведение к VehicleType
            var filtered = collectionManager.filterLessThanType(type); // получение отфильтрованных элементов по типу (меньше заданного)
            
            if (filtered.isEmpty()) { // проверка, пуст ли результат фильтрации
                return new Response(ResponseStatus.SUCCESS, // возврат ответа с успешным статусом
                    "Элементы с типом меньше " + type + " не найдены"); // сообщение о том, что элементы не найдены
            }
            
            StringBuilder sb = new StringBuilder(); // создание строителя строк для формирования ответа
            for (Vehicle v : filtered) { // цикл по отфильтрованным элементам
                sb.append(v).append("\n"); // добавление строкового представления элемента и переноса строки
            }
            return new Response(ResponseStatus.SUCCESS, sb.toString(), filtered); // возврат ответа с успешным статусом, текстом и списком
        } catch (Exception e) { // обработка любых исключений
            return new Response(ResponseStatus.ERROR, // возврат ответа с ошибкой
                "Ошибка фильтрации: " + e.getMessage()); // сообщение с деталями ошибки
        }
    }
    
    /**
     * Обработка команды SAVE (только на сервере)
     * 
     * Сохраняет текущее состояние коллекции в XML файл
     * Команда отправляется клиентом, но сохранение выполняется на стороне сервера
     * Клиент не сохраняет коллекцию у себя локально - он лишь инициирует сохранение той коллекции, которая находится в памяти сервера
     * 
     * Это необходимо, так как вся коллекция хранится исключительно на сервере
     * Клиент работает с копией данных или просто отправляет команды, а сервер отвечает за персистентность (сохранение в файл)
     * 
     * @return ответ со статусом SUCCESS при успешном сохранении или ERROR при ошибке
     */
    private Response processSave() { // метод обработки команды save (сохранить коллекцию в файл)
        try { // начало блока перехвата исключений
            collectionManager.save(); // вызов метода сохранения коллекции в файл
            return new Response(ResponseStatus.SUCCESS, // возврат ответа с успешным статусом
                "Коллекция сохранена в файл"); // сообщение об успешном сохранении
        } catch (Exception e) { // обработка любых исключений
            return new Response(ResponseStatus.ERROR, // возврат ответа с ошибкой
                "Ошибка сохранения: " + e.getMessage()); // сообщение с деталями ошибки
        }
    }
}