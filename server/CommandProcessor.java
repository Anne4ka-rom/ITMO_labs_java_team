package server; // класс находится в пакете server

import common.*; // импорт всех общих классов и утилит
import common.model.Vehicle; // импорт класса транспортного средства из общей модели
import common.model.VehicleType; // импорт перечисления типов транспортных средств
import server.collection.CollectionManager; // импорт менеджера коллекции из подпакета collection
import server.utils.RandomVehicleGenerator;

import java.sql.SQLException; // импорт для обработки ошибок бд
import java.util.List; // импорт интерфейса списка
import java.util.stream.Collectors; // импорт коллектора для stream api

/**
 * Обрабатывает команды от клиента
 * Преобразует запросы в операции над коллекцией и формирует ответы
 * 
 * @author Anni
 * @version 2.1
 */
public class CommandProcessor { // объявляет класс для обработки команд
    private final CollectionManager collectionManager; // менеджер коллекции для выполнения операций
    
    /**
     * Создаёт новый процессор команд с указанным менеджером коллекции
     * 
     * @param collectionManager менеджер для управления коллекцией
     */
    public CommandProcessor(CollectionManager collectionManager) { // конструктор класса
        this.collectionManager = collectionManager; // сохраняем ссылку на менеджер коллекции
    }
    
    /**
     * Обрабатывает запрос от конкретного клиента
     * Проверяет состояние обработки, десериализует запрос и вызывает соответствующую команду
     * 
     * @param handler обработчик клиента, содержащий запрос
     */
    public void processRequest(ClientHandler handler) { // метод обработки запроса от клиента
        if (handler.isProcessing() && handler.hasResponseToSend()) { // проверяем, не обрабатывается ли уже запрос и есть ли ответ
            return; // выходим, чтобы не обрабатывать повторно
        }
        
        try { // начало блока перехвата исключений
            byte[] requestData = handler.getCompleteRequest(); // получаем массив байтов запроса из обработчика
            Request request = RequestReader.deserializeRequest(requestData); // десериализуем запрос в объект
            Command command = request.getCommand(); // извлекаем команду из запроса
            
            System.out.println("Обработка команды: " + command.getType()); // выводим в консоль тип обрабатываемой команды
            
            if (command.getType() == CommandType.SAVE) { // проверяем, не пытается ли клиент выполнить команду save
                Response response = new Response( // создаём ответ с ошибкой
                    ResponseStatus.ERROR, // устанавливаем статус ошибки
                    "Команда save недоступна для клиентов. Только сервер может сохранять коллекцию." // сообщение об ошибке
                );
                handler.setResponseToSend(response); // сохраняем ответ в обработчике клиента
                handler.clearCompleteRequest(); // очищаем сохранённый запрос
                return; // выходим из метода
            }
            
            Response response = process(command); // обрабатываем команду и получаем ответ
            handler.setResponseToSend(response); // сохраняем ответ в обработчике
            handler.clearCompleteRequest(); // очищаем запрос после обработки
            
        } catch (Exception e) { // обрабатываем любые исключения при обработке
            Response errorResponse = new Response( // создаём ответ с ошибкой
                ResponseStatus.ERROR, // статус ошибки
                "Ошибка обработки запроса: " + e.getMessage() // сообщение с деталями исключения
            );
            handler.setResponseToSend(errorResponse); // сохраняем ответ об ошибке
            handler.clearCompleteRequest(); // очищаем запрос
        }
    }
    
    /**
     * Обрабатывает команду и возвращает соответствующий ответ
     * Использует switch для выбора нужного метода обработки в зависимости от типа команды
     * 
     * @param command команда для выполнения
     * @return ответ с результатом выполнения команды
     */
    public Response process(Command command) { // метод обработки команды
        CommandType type = command.getType(); // получаем тип команды
        Object argument = command.getArgument(); // получаем аргумент команды (может быть null)
        
        try { // начало блока перехвата исключений для всех команд
            switch (type) { // выбираем обработчик в зависимости от типа команды
                case HELP: return processHelp(); // вызов метода обработки help
                case INFO: return processInfo(); // вызов метода обработки info
                case SHOW: return processShow(); // вызов метода обработки show
                case ADD: return processAdd(argument); // вызов метода add с аргументом
                case ADD_RANDOM: return processAddRandom(); // вызов метода add_random
                case UPDATE: return processUpdate(argument); // вызов метода update с аргументом
                case REMOVE_BY_ID: return processRemoveById(argument); // вызов метода удаления по id
                case CLEAR: return processClear(); // вызов метода очистки коллекции
                case REMOVE_LAST: return processRemoveLast(); // вызов метода удаления последнего элемента
                case REMOVE_LOWER: return processRemoveLower(argument); // вызов метода удаления меньших элементов
                case SORT: return processSort(); // вызов метода сортировки
                case SUM_OF_CAPACITY: return processSumOfCapacity(); // вызов метода подсчёта суммы вместимостей
                case FILTER_BY_CAPACITY: return processFilterByCapacity(argument); // вызов метода фильтрации по вместимости
                case FILTER_LESS_THAN_TYPE: return processFilterLessThanType(argument); // вызов метода фильтрации по типу
                case EXECUTE_SCRIPT: // команда выполнения скрипта
                    return new Response(ResponseStatus.SUCCESS, "Скрипт выполняется на клиенте"); // ответ о выполнении на клиенте
                case EXIT: // команда выхода
                    return new Response(ResponseStatus.SUCCESS, "До свидания!"); // прощальное сообщение
                default: // если тип команды не распознан
                    return new Response(ResponseStatus.ERROR, "Неизвестная команда: " + type); // сообщение об ошибке
            }
        } catch (SQLException e) { // обрабатываем ошибки базы данных
            return new Response(ResponseStatus.ERROR, "Ошибка базы данных: " + e.getMessage()); // возвращаем ошибку бд
        } catch (Exception e) { // обрабатываем любые другие исключения
            return new Response(ResponseStatus.ERROR, "Ошибка выполнения команды: " + e.getMessage()); // возвращаем ошибку с деталями
        }
    }
    
    /**
     * Обрабатывает команду help - выводит список всех доступных команд с описанием
     * 
     * @return ответ со списком команд
     */
    private Response processHelp() { // метод обработки команды help
        String helpText = "Доступные команды:\n" + // заголовок справки
            "  help - вывести справку\n" + // описание команды help
            "  info - информация о коллекции\n" + // описание команды info
            "  show - вывести все элементы\n" + // описание команды show
            "  add - добавить новый элемент\n" + // описание команды add
            "  add_random - добавить случайный элемент\n" + // описание команды add_random
            "  update <id> - обновить элемент по ID\n" + // описание команды update
            "  remove_by_id <id> - удалить элемент по ID\n" + // описание команды remove_by_id
            "  clear - очистить коллекцию\n" + // описание команды clear
            "  remove_last - удалить последний элемент\n" + // описание команды remove_last
            "  remove_lower - удалить элементы, меньшие заданного\n" + // описание команды remove_lower
            "  sort - отсортировать коллекцию\n" + // описание команды sort
            "  sum_of_capacity - сумма грузоподъёмностей\n" + // описание команды sum_of_capacity
            "  filter_by_capacity <capacity> - фильтр по грузоподъёмности\n" + // описание команды filter_by_capacity
            "  filter_less_than_type <type> - фильтр по типу\n" + // описание команды filter_less_than_type
            "  execute_script <file> - выполнить скрипт\n" + // описание команды execute_script
            "  exit - завершить работу клиента"; // описание команды exit
        return new Response(ResponseStatus.SUCCESS, helpText); // возвращаем успешный ответ с текстом справки
    }
    
    /**
     * Обрабатывает команду info - возвращает информацию о коллекции
     * 
     * @return ответ с информацией о коллекции
     */
    private Response processInfo() { // метод обработки команды info
        return new Response(ResponseStatus.SUCCESS, collectionManager.getInfo()); // возвращаем информацию от менеджера коллекции
    }
    
    /**
     * Обрабатывает команду show - возвращает все элементы коллекции
     * Если коллекция пуста, возвращает соответствующее сообщение
     * 
     * @return ответ со списком элементов
     */
    private Response processShow() { // метод обработки команды show
        if (collectionManager.isEmpty()) { // проверяем, пуста ли коллекция
            return new Response(ResponseStatus.SUCCESS, "Коллекция пуста"); // возвращаем сообщение о пустоте
        }
        List<Vehicle> vehicles = collectionManager.getAllSorted(); // получаем отсортированный список всех элементов
        StringBuilder sb = new StringBuilder(); // создаём строитель строки
        for (Vehicle v : vehicles) { // проходим по всем элементам
            sb.append(v).append("\n"); // добавляем строковое представление элемента и перевод строки
        }
        return new Response(ResponseStatus.SUCCESS, sb.toString(), vehicles); // возвращаем успешный ответ с текстом и списком
    }
    
    /**
     * Обрабатывает команду add - добавляет новый элемент в коллекцию
     * 
     * @param argument объект vehicle для добавления
     * @return ответ с результатом операции
     */
    private Response processAdd(Object argument) { // метод обработки команды add
        try { // начало блока перехвата исключений
            Vehicle vehicle = (Vehicle) argument; // приводим аргумент к типу vehicle
            collectionManager.add(vehicle); // добавляем элемент в коллекцию
            return new Response(ResponseStatus.SUCCESS, "Элемент добавлен с ID: " + vehicle.getId()); // возвращаем успех с id добавленного элемента
        } catch (ClassCastException e) { // обрабатываем ошибку приведения типов
            return new Response(ResponseStatus.ERROR, "Ошибка: неверный тип аргумента"); // возвращаем ошибку о неверном типе
        } catch (SQLException e) { // обрабатываем ошибки бд
            return new Response(ResponseStatus.ERROR, "Ошибка базы данных: " + e.getMessage()); // возвращаем ошибку бд
        } catch (Exception e) { // обрабатываем любые другие исключения
            return new Response(ResponseStatus.ERROR, "Ошибка добавления: " + e.getMessage()); // возвращаем ошибку с деталями
        }
    }

    /**
     * Обрабатывает команду add_random - генерирует случайный элемент и добавляет его в коллекцию
     * Генерация происходит на сервере, без участия клиента
     * 
     * @return ответ с результатом операции
     */
    private Response processAddRandom() { // метод обработки команды add_random
        try { // начало блока перехвата исключений
            Vehicle vehicle = RandomVehicleGenerator.generateRandomVehicle(); // генерируем случайное ТС на сервере
            collectionManager.add(vehicle); // добавляем элемент в коллекцию
            return new Response(ResponseStatus.SUCCESS, "Случайный элемент добавлен с ID: " + vehicle.getId() + "\n" + vehicle); // возвращаем успех
        } catch (SQLException e) { // обрабатываем ошибки бд
            return new Response(ResponseStatus.ERROR, "Ошибка базы данных: " + e.getMessage()); // возвращаем ошибку бд
        } catch (Exception e) { // обрабатываем любые другие исключения
            return new Response(ResponseStatus.ERROR, "Ошибка добавления случайного элемента: " + e.getMessage()); // возвращаем ошибку
        }
    }
    
    /**
     * Обрабатывает команду update - обновляет существующий элемент по id
     * Аргумент представляет собой массив из двух элементов: id и новый vehicle
     * 
     * @param argument массив объектов [id, newVehicle]
     * @return ответ с результатом операции
     */
    private Response processUpdate(Object argument) { // метод обработки команды update
        try { // начало блока перехвата исключений
            Object[] args = (Object[]) argument; // приводим аргумент к массиву объектов
            int id = (int) args[0]; // извлекаем id из первого элемента массива
            Vehicle newVehicle = (Vehicle) args[1]; // извлекаем новый vehicle из второго элемента
            
            if (!collectionManager.containsId(id)) { // проверяем, существует ли элемент с таким id
                return new Response(ResponseStatus.ERROR, "Элемент с ID " + id + " не найден"); // возвращаем ошибку, если элемент не найден
            }
            
            if (!collectionManager.isOwner(id)) { // проверяем, принадлежит ли элемент текущему пользователю
                return new Response(ResponseStatus.ERROR, "Нет прав на изменение этого элемента"); // возвращаем ошибку, если нет прав
            }
            
            boolean updated = collectionManager.updateById(id, newVehicle); // обновляем элемент в коллекции
            if (updated) { // проверяем успешность обновления
                return new Response(ResponseStatus.SUCCESS, "Элемент с ID " + id + " обновлен"); // возвращаем сообщение об успехе
            } else {
                return new Response(ResponseStatus.ERROR, "Не удалось обновить элемент с ID " + id); // возвращаем ошибку
            }
        } catch (ClassCastException e) { // обрабатываем ошибку приведения типов
            return new Response(ResponseStatus.ERROR, "Ошибка: неверный тип аргумента"); // возвращаем ошибку о неверном типе
        } catch (SQLException e) { // обрабатываем ошибки бд
            return new Response(ResponseStatus.ERROR, "Ошибка базы данных: " + e.getMessage()); // возвращаем ошибку бд
        } catch (Exception e) { // обрабатываем любые исключения
            return new Response(ResponseStatus.ERROR, "Ошибка обновления: " + e.getMessage()); // возвращаем ошибку с деталями
        }
    }
    
    /**
     * Обрабатывает команду remove_by_id - удаляет элемент по идентификатору
     * 
     * @param argument целочисленный id удаляемого элемента
     * @return ответ с результатом операции
     */
    private Response processRemoveById(Object argument) { // метод обработки команды remove_by_id
        try { // начало блока перехвата исключений
            int id = (int) argument; // приводим аргумент к целому числу
            
            if (!collectionManager.containsId(id)) { // проверяем, существует ли элемент с таким id
                return new Response(ResponseStatus.ERROR, "Элемент с ID " + id + " не найден"); // возвращаем ошибку, если элемент не найден
            }
            
            if (!collectionManager.isOwner(id)) { // проверяем, принадлежит ли элемент текущему пользователю
                return new Response(ResponseStatus.ERROR, "Нет прав на удаление этого элемента"); // возвращаем ошибку, если нет прав
            }
            
            if (collectionManager.removeById(id)) { // пытаемся удалить элемент и проверяем результат
                return new Response(ResponseStatus.SUCCESS, "Элемент с ID " + id + " удален"); // возвращаем успех, если удаление выполнено
            } else { // если элемент не найден
                return new Response(ResponseStatus.ERROR, "Элемент с ID " + id + " не найден"); // возвращаем ошибку
            }
        } catch (ClassCastException e) { // обрабатываем ошибку приведения типов
            return new Response(ResponseStatus.ERROR, "Ошибка: id должен быть числом"); // возвращаем ошибку о неверном типе
        } catch (SQLException e) { // обрабатываем ошибки бд
            return new Response(ResponseStatus.ERROR, "Ошибка базы данных: " + e.getMessage()); // возвращаем ошибку бд
        } catch (Exception e) { // обрабатываем любые исключения
            return new Response(ResponseStatus.ERROR, "Ошибка удаления: " + e.getMessage()); // возвращаем ошибку с деталями
        }
    }
    
    /**
     * Обрабатывает команду clear - полностью очищает коллекцию текущего пользователя
     * 
     * @return ответ с результатом операции
     */
    private Response processClear() { // метод обработки команды clear
        try { // начало блока перехвата исключений
            collectionManager.clear(); // вызываем очистку коллекции для текущего пользователя
            return new Response(ResponseStatus.SUCCESS, "Коллекция очищена"); // возвращаем сообщение об успехе
        } catch (SQLException e) { // обрабатываем ошибки бд
            return new Response(ResponseStatus.ERROR, "Ошибка базы данных: " + e.getMessage()); // возвращаем ошибку бд
        } catch (Exception e) { // обрабатываем любые исключения
            return new Response(ResponseStatus.ERROR, "Ошибка очистки: " + e.getMessage()); // возвращаем ошибку с деталями
        }
    }
    
    /**
     * Обрабатывает команду remove_last - удаляет последний элемент текущего пользователя из коллекции
     * 
     * @return ответ с результатом операции
     */
    private Response processRemoveLast() { // метод обработки команды remove_last
        try { // начало блока перехвата исключений
            if (collectionManager.isEmpty()) { // проверяем, не пуста ли коллекция
                return new Response(ResponseStatus.ERROR, "Коллекция пуста"); // возвращаем ошибку, если коллекция пуста
            }
            collectionManager.removeLast(); // удаляем последний элемент текущего пользователя
            return new Response(ResponseStatus.SUCCESS, "Последний элемент удален"); // возвращаем сообщение об успехе
        } catch (SQLException e) { // обрабатываем ошибки бд
            return new Response(ResponseStatus.ERROR, "Ошибка базы данных: " + e.getMessage()); // возвращаем ошибку бд
        } catch (Exception e) { // обрабатываем любые исключения
            return new Response(ResponseStatus.ERROR, "Ошибка удаления: " + e.getMessage()); // возвращаем ошибку с деталями
        }
    }
    
    /**
     * Обрабатывает команду remove_lower - удаляет все элементы текущего пользователя, меньшие заданного
     * 
     * @param argument эталонный объект vehicle для сравнения
     * @return ответ с количеством удалённых элементов
     */
    private Response processRemoveLower(Object argument) { // метод обработки команды remove_lower
        try { // начало блока перехвата исключений
            Vehicle vehicle = (Vehicle) argument; // приводим аргумент к типу vehicle
            int removedCount = collectionManager.removeLower(vehicle); // удаляем меньшие элементы и получаем количество
            return new Response(ResponseStatus.SUCCESS, "Удалено элементов: " + removedCount); // возвращаем количество удалённых
        } catch (ClassCastException e) { // обрабатываем ошибку приведения типов
            return new Response(ResponseStatus.ERROR, "Ошибка: неверный тип аргумента"); // возвращаем ошибку о неверном типе
        } catch (SQLException e) { // обрабатываем ошибки бд
            return new Response(ResponseStatus.ERROR, "Ошибка базы данных: " + e.getMessage()); // возвращаем ошибку бд
        } catch (Exception e) { // обрабатываем любые исключения
            return new Response(ResponseStatus.ERROR, "Ошибка удаления: " + e.getMessage()); // возвращаем ошибку с деталями
        }
    }
    
    /**
     * Обрабатывает команду sort - сортирует коллекцию в естественном порядке
     * 
     * @return ответ с результатом операции
     */
    private Response processSort() { // метод обработки команды sort
        try { // начало блока перехвата исключений
            collectionManager.sort(); // вызываем сортировку коллекции
            return new Response(ResponseStatus.SUCCESS, "Коллекция отсортирована"); // возвращаем сообщение об успехе
        } catch (Exception e) { // обрабатываем любые исключения
            return new Response(ResponseStatus.ERROR, "Ошибка сортировки: " + e.getMessage()); // возвращаем ошибку с деталями
        }
    }
    
    /**
     * Обрабатывает команду sum_of_capacity - вычисляет сумму вместимостей всех элементов коллекции
     * 
     * @return ответ с суммой вместимостей
     */
    private Response processSumOfCapacity() { // метод обработки команды sum_of_capacity
        try { // начало блока перехвата исключений
            double sum = collectionManager.getSumOfCapacity(); // получаем сумму вместимостей
            return new Response(ResponseStatus.SUCCESS, "Сумма грузоподъёмностей: " + sum); // возвращаем результат
        } catch (Exception e) { // обрабатываем любые исключения
            return new Response(ResponseStatus.ERROR, "Ошибка вычисления суммы: " + e.getMessage()); // возвращаем ошибку с деталями
        }
    }
    
    /**
     * Обрабатывает команду filter_by_capacity - фильтрует элементы по вместимости
     * 
     * @param argument значение вместимости для фильтрации (тип double)
     * @return ответ с отфильтрованными элементами
     */
    private Response processFilterByCapacity(Object argument) { // метод обработки команды filter_by_capacity
        try { // начало блока перехвата исключений
            double capacity = (double) argument; // приводим аргумент к типу double
            List<Vehicle> filtered = collectionManager.filterByCapacity(capacity); // получаем отфильтрованный список
            List<Vehicle> sorted = filtered.stream().sorted().collect(Collectors.toList()); // сортируем отфильтрованный список
            
            if (sorted.isEmpty()) { // проверяем, пуст ли результат
                return new Response(ResponseStatus.SUCCESS, "Элементы с capacity " + capacity + " не найдены"); // сообщение о пустом результате
            }
            
            StringBuilder sb = new StringBuilder(); // создаём строитель строки
            for (Vehicle v : sorted) { // проходим по всем отфильтрованным элементам
                sb.append(v).append("\n"); // добавляем строковое представление элемента и перевод строки
            }
            return new Response(ResponseStatus.SUCCESS, sb.toString(), sorted); // возвращаем успешный ответ с текстом и списком
        } catch (ClassCastException e) { // обрабатываем ошибку приведения типов
            return new Response(ResponseStatus.ERROR, "Ошибка: capacity должно быть числом"); // возвращаем ошибку о неверном типе
        } catch (Exception e) { // обрабатываем любые исключения
            return new Response(ResponseStatus.ERROR, "Ошибка фильтрации: " + e.getMessage()); // возвращаем ошибку с деталями
        }
    }
    
    /**
     * Обрабатывает команду filter_less_than_type - фильтрует элементы по типу (меньше указанного)
     * 
     * @param argument тип для сравнения (vehicletype)
     * @return ответ с отфильтрованными элементами
     */
    private Response processFilterLessThanType(Object argument) { // метод обработки команды filter_less_than_type
        try { // начало блока перехвата исключений
            VehicleType type = (VehicleType) argument; // приводим аргумент к типу vehicletype
            List<Vehicle> filtered = collectionManager.filterLessThanType(type); // получаем отфильтрованный список
            List<Vehicle> sorted = filtered.stream().sorted().collect(Collectors.toList()); // сортируем отфильтрованный список
            
            if (sorted.isEmpty()) { // проверяем, пуст ли результат
                return new Response(ResponseStatus.SUCCESS, "Элементы с типом меньше " + type + " не найдены"); // сообщение о пустом результате
            }
            
            StringBuilder sb = new StringBuilder(); // создаём строитель строки
            for (Vehicle v : sorted) { // проходим по всем отфильтрованным элементам
                sb.append(v).append("\n"); // добавляем строковое представление элемента и перевод строки
            }
            return new Response(ResponseStatus.SUCCESS, sb.toString(), sorted); // возвращаем успешный ответ с текстом и списком
        } catch (ClassCastException e) { // обрабатываем ошибку приведения типов
            return new Response(ResponseStatus.ERROR, "Ошибка: неверный тип аргумента"); // возвращаем ошибку о неверном типе
        } catch (Exception e) { // обрабатываем любые исключения
            return new Response(ResponseStatus.ERROR, "Ошибка фильтрации: " + e.getMessage()); // возвращаем ошибку с деталями
        }
    }
}