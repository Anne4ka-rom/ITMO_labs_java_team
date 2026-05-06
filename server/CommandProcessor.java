package server; // класс находится в пакете server

import common.*; // импорт всех общих классов и утилит
import common.model.Vehicle; // импорт класса транспортного средства из общей модели
import common.model.VehicleType; // импорт перечисления типов транспортных средств
import server.collection.CollectionManager; // импорт менеджера коллекции из подпакета collection

import java.util.List; // импорт интерфейса списка
import java.util.stream.Collectors; // импорт коллектора для stream api

/**
 * обрабатывает команды от клиента
 * преобразует запросы в операции над коллекцией и формирует ответы
 * 
 * @author anni
 * @version 1.0
 */
public class CommandProcessor { // объявляет класс для обработки команд
    private final CollectionManager collectionManager; // менеджер коллекции для выполнения операций
    
    /**
     * создает новый процессор команд с указанным менеджером коллекции
     * 
     * @param collectionManager менеджер для управления коллекцией
     */
    public CommandProcessor(CollectionManager collectionManager) { // конструктор класса
        this.collectionManager = collectionManager; // сохраняем ссылку на менеджер коллекции
    }
    
    /**
     * обрабатывает запрос от конкретного клиента
     * проверяет состояние обработки, десериализует запрос и вызывает соответствующую команду
     * 
     * @param handler обработчик клиента, содержащий запрос
     */
    public void processRequest(ClientHandler handler) { // метод обработки запроса от клиента
        if (handler.isProcessing() && handler.hasResponseToSend()) { // проверяем, не обрабатывается ли уже запрос и есть ли ответ
            return; // выходим, чтобы не обрабатывать повторно
        } // конец проверки состояния
        
        try { // начало блока перехвата исключений
            byte[] requestData = handler.getCompleteRequest(); // получаем массив байтов запроса из обработчика
            Request request = RequestReader.deserializeRequest(requestData); // десериализуем запрос в объект
            Command command = request.getCommand(); // извлекаем команду из запроса
            
            System.out.println("Обработка команды: " + command.getType()); // выводим в консоль тип обрабатываемой команды
            
            if (command.getType() == CommandType.SAVE) { // проверяем, не пытается ли клиент выполнить команду save
                Response response = new Response( // создаем ответ с ошибкой
                    ResponseStatus.ERROR, // устанавливаем статус ошибки
                    "Команда save недоступна для клиентов. Только сервер может сохранять коллекцию." // сообщение об ошибке
                ); // конец создания ответа
                handler.setResponseToSend(response); // сохраняем ответ в обработчике клиента
                handler.clearCompleteRequest(); // очищаем сохраненный запрос
                return; // выходим из метода
            } // конец проверки команды save
            
            Response response = process(command); // обрабатываем команду и получаем ответ
            handler.setResponseToSend(response); // сохраняем ответ в обработчике
            handler.clearCompleteRequest(); // очищаем запрос после обработки
            
        } catch (Exception e) { // обрабатываем любые исключения при обработке
            Response errorResponse = new Response( // создаем ответ с ошибкой
                ResponseStatus.ERROR, // статус ошибки
                "Ошибка обработки запроса: " + e.getMessage() // сообщение с деталями исключения
            ); // конец создания ответа
            handler.setResponseToSend(errorResponse); // сохраняем ответ об ошибке
            handler.clearCompleteRequest(); // очищаем запрос
        }
    }
    
    /**
     * обрабатывает команду и возвращает соответствующий ответ
     * использует switch для выбора нужного метода обработки в зависимости от типа команды
     * 
     * @param command команда для выполнения
     * @return ответ с результатом выполнения команды
     */
    public Response process(Command command) { // метод обработки команды
        CommandType type = command.getType(); // получаем тип команды
        Object argument = command.getArgument(); // получаем аргумент команды (может быть null)
        
        switch (type) { // выбираем обработчик в зависимости от типа команды
            case HELP: return processHelp(); // вызов метода обработки help
            case INFO: return processInfo(); // вызов метода обработки info
            case SHOW: return processShow(); // вызов метода обработки show
            case ADD: return processAdd(argument); // вызов метода add с аргументом
            case UPDATE: return processUpdate(argument); // вызов метода update с аргументом
            case REMOVE_BY_ID: return processRemoveById(argument); // вызов метода удаления по id
            case CLEAR: return processClear(); // вызов метода очистки коллекции
            case REMOVE_LAST: return processRemoveLast(); // вызов метода удаления последнего элемента
            case REMOVE_LOWER: return processRemoveLower(argument); // вызов метода удаления меньших элементов
            case SORT: return processSort(); // вызов метода сортировки
            case SUM_OF_CAPACITY: return processSumOfCapacity(); // вызов метода подсчета суммы вместимостей
            case FILTER_BY_CAPACITY: return processFilterByCapacity(argument); // вызов метода фильтрации по вместимости
            case FILTER_LESS_THAN_TYPE: return processFilterLessThanType(argument); // вызов метода фильтрации по типу
            case EXECUTE_SCRIPT: // команда выполнения скрипта
                return new Response(ResponseStatus.SUCCESS, "Скрипт выполняется на клиенте"); // ответ о выполнении на клиенте
            case EXIT: // команда выхода
                return new Response(ResponseStatus.SUCCESS, "До свидания!"); // прощальное сообщение
            default: // если тип команды не распознан
                return new Response(ResponseStatus.ERROR, "Неизвестная команда: " + type); // сообщение об ошибке
        }
    }
    
    /**
     * обрабатывает команду help - выводит список всех доступных команд с описанием
     * 
     * @return ответ со списком команд
     */
    private Response processHelp() { // метод обработки команды help
        String helpText = "Доступные команды:\n" + // заголовок справки
            "  help - вывести справку\n" + // описание команды help
            "  info - информация о коллекции\n" + // описание команды info
            "  show - вывести все элементы\n" + // описание команды show
            "  add - добавить новый элемент\n" + // описание команды add
            "  update <id> - обновить элемент по ID\n" + // описание команды update
            "  remove_by_id <id> - удалить элемент по ID\n" + // описание команды remove_by_id
            "  clear - очистить коллекцию\n" + // описание команды clear
            "  remove_last - удалить последний элемент\n" + // описание команды remove_last
            "  remove_lower - удалить элементы, меньшие заданного\n" + // описание команды remove_lower
            "  sort - отсортировать коллекцию\n" + // описание команды sort
            "  sum_of_capacity - сумма грузоподъемностей\n" + // описание команды sum_of_capacity
            "  filter_by_capacity <capacity> - фильтр по грузоподъемности\n" + // описание команды filter_by_capacity
            "  filter_less_than_type <type> - фильтр по типу\n" + // описание команды filter_less_than_type
            "  execute_script <file> - выполнить скрипт\n" + // описание команды execute_script
            "  exit - завершить работу клиента"; // описание команды exit
        return new Response(ResponseStatus.SUCCESS, helpText); // возвращаем успешный ответ с текстом справки
    }
    
    /**
     * обрабатывает команду info - возвращает информацию о коллекции
     * 
     * @return ответ с информацией о коллекции
     */
    private Response processInfo() { // метод обработки команды info
        return new Response(ResponseStatus.SUCCESS, collectionManager.getInfo()); // возвращаем информацию от менеджера коллекции
    }
    
    /**
     * обрабатывает команду show - возвращает все элементы коллекции
     * если коллекция пуста, возвращает соответствующее сообщение
     * 
     * @return ответ со списком элементов
     */
    private Response processShow() { // метод обработки команды show
        if (collectionManager.isEmpty()) { // проверяем, пуста ли коллекция
            return new Response(ResponseStatus.SUCCESS, "Коллекция пуста"); // возвращаем сообщение о пустоте
        }
        List<Vehicle> vehicles = collectionManager.getAllSorted(); // получаем отсортированный список всех элементов
        StringBuilder sb = new StringBuilder(); // создаем строитель строки
        for (Vehicle v : vehicles) { // проходим по всем элементам
            sb.append(v).append("\n"); // добавляем строковое представление элемента и перевод строки
        }
        return new Response(ResponseStatus.SUCCESS, sb.toString(), vehicles); // возвращаем успешный ответ с текстом и списком
    }
    
    /**
     * обрабатывает команду add - добавляет новый элемент в коллекцию
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
        } catch (Exception e) { // обрабатываем любые другие исключения
            return new Response(ResponseStatus.ERROR, "Ошибка добавления: " + e.getMessage()); // возвращаем ошибку с деталями
        }
    }
    
    /**
     * обрабатывает команду update - обновляет существующий элемент по id
     * аргумент представляет собой массив из двух элементов: id и новый vehicle
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
            } // конец проверки
            
            collectionManager.updateById(id, newVehicle); // обновляем элемент в коллекции
            return new Response(ResponseStatus.SUCCESS, "Элемент с ID " + id + " обновлен"); // возвращаем сообщение об успехе
        } catch (Exception e) { // обрабатываем любые исключения
            return new Response(ResponseStatus.ERROR, "Ошибка обновления: " + e.getMessage()); // возвращаем ошибку с деталями
        }
    }
    
    /**
     * обрабатывает команду remove_by_id - удаляет элемент по идентификатору
     * 
     * @param argument целочисленный id удаляемого элемента
     * @return ответ с результатом операции
     */
    private Response processRemoveById(Object argument) { // метод обработки команды remove_by_id
        try { // начало блока перехвата исключений
            int id = (int) argument; // приводим аргумент к целому числу
            if (collectionManager.removeById(id)) { // пытаемся удалить элемент и проверяем результат
                return new Response(ResponseStatus.SUCCESS, "Элемент с ID " + id + " удален"); // возвращаем успех, если удаление выполнено
            } else { // если элемент не найден
                return new Response(ResponseStatus.ERROR, "Элемент с ID " + id + " не найден"); // возвращаем ошибку
            } // конец проверки результата удаления
        } catch (Exception e) { // обрабатываем любые исключения
            return new Response(ResponseStatus.ERROR, "Ошибка удаления: " + e.getMessage()); // возвращаем ошибку с деталями
        }
    }
    
    /**
     * обрабатывает команду clear - полностью очищает коллекцию
     * 
     * @return ответ с результатом операции
     */
    private Response processClear() { // метод обработки команды clear
        collectionManager.clear(); // вызываем очистку коллекции
        return new Response(ResponseStatus.SUCCESS, "Коллекция очищена"); // возвращаем сообщение об успехе
    }
    
    /**
     * обрабатывает команду remove_last - удаляет последний элемент из коллекции
     * 
     * @return ответ с результатом операции
     */
    private Response processRemoveLast() { // метод обработки команды remove_last
        if (collectionManager.isEmpty()) { // проверяем, не пуста ли коллекция
            return new Response(ResponseStatus.ERROR, "Коллекция пуста"); // возвращаем ошибку, если коллекция пуста
        } // конец проверки
        collectionManager.removeLast(); // удаляем последний элемент
        return new Response(ResponseStatus.SUCCESS, "Последний элемент удален"); // возвращаем сообщение об успехе
    }
    
    /**
     * обрабатывает команду remove_lower - удаляет все элементы, меньшие заданного
     * 
     * @param argument эталонный объект vehicle для сравнения
     * @return ответ с количеством удаленных элементов
     */
    private Response processRemoveLower(Object argument) { // метод обработки команды remove_lower
        try { // начало блока перехвата исключений
            Vehicle vehicle = (Vehicle) argument; // приводим аргумент к типу vehicle
            int removedCount = collectionManager.removeLower(vehicle); // удаляем меньшие элементы и получаем количество
            return new Response(ResponseStatus.SUCCESS, "Удалено элементов: " + removedCount); // возвращаем количество удаленных
        } catch (Exception e) { // обрабатываем любые исключения
            return new Response(ResponseStatus.ERROR, "Ошибка удаления: " + e.getMessage()); // возвращаем ошибку с деталями
        } // конец блока catch
    }
    
    /**
     * обрабатывает команду sort - сортирует коллекцию в естественном порядке
     * 
     * @return ответ с результатом операции
     */
    private Response processSort() { // метод обработки команды sort
        collectionManager.sort(); // вызываем сортировку коллекции
        return new Response(ResponseStatus.SUCCESS, "Коллекция отсортирована"); // возвращаем сообщение об успехе
    } // конец метода processSort
    
    /**
     * обрабатывает команду sum_of_capacity - вычисляет сумму вместимостей всех элементов
     * 
     * @return ответ с суммой вместимостей
     */
    private Response processSumOfCapacity() { // метод обработки команды sum_of_capacity
        double sum = collectionManager.getSumOfCapacity(); // получаем сумму вместимостей
        return new Response(ResponseStatus.SUCCESS, "Сумма грузоподъемностей: " + sum); // возвращаем результат
    }
    
    /**
     * обрабатывает команду filter_by_capacity - фильтрует элементы по вместимости
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
            } // конец проверки
            
            StringBuilder sb = new StringBuilder(); // создаем строитель строки
            for (Vehicle v : sorted) { // проходим по всем отфильтрованным элементам
                sb.append(v).append("\n"); // добавляем строковое представление элемента и перевод строки
            } // конец цикла
            return new Response(ResponseStatus.SUCCESS, sb.toString(), sorted); // возвращаем успешный ответ с текстом и списком
        } catch (Exception e) { // обрабатываем любые исключения
            return new Response(ResponseStatus.ERROR, "Ошибка фильтрации: " + e.getMessage()); // возвращаем ошибку с деталями
        } // конец блока catch
    }
    
    /**
     * обрабатывает команду filter_less_than_type - фильтрует элементы по типу (меньше указанного)
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
            } // конец проверки
            
            StringBuilder sb = new StringBuilder(); // создаем строитель строки
            for (Vehicle v : sorted) { // проходим по всем отфильтрованным элементам
                sb.append(v).append("\n"); // добавляем строковое представление элемента и перевод строки
            } // конец цикла
            return new Response(ResponseStatus.SUCCESS, sb.toString(), sorted); // возвращаем успешный ответ с текстом и списком
        } catch (Exception e) { // обрабатываем любые исключения
            return new Response(ResponseStatus.ERROR, "Ошибка фильтрации: " + e.getMessage()); // возвращаем ошибку с деталями
        } // конец блока catch
    }
}