package server; // класс находится в пакете server

import common.*; // импорт всех общих классов и утилит
import common.model.Coordinates; // импорт класса координат из общей модели
import common.model.FuelType; // импорт перечисления типов топлива из общей модели
import common.model.Vehicle; // импорт класса транспортного средства из общей модели
import common.model.VehicleType; // импорт перечисления типов транспортных средств из общей модели
import server.collection.CollectionManager; // импорт менеджера коллекции из подпакета collection
import server.database.DatabaseManager; // импорт менеджера базы данных

import java.util.List; // импорт интерфейса списка
import java.util.Random; // импорт класса для генерации случайных чисел
import java.util.stream.Collectors; // импорт коллектора для stream api

/**
 * Обрабатывает команды от клиента с проверкой авторизации и прав доступа
 * Преобразует запросы в операции над коллекцией и формирует ответы
 * Поддерживает регистрацию и авторизацию пользователей
 * 
 * @author Anni
 * @version 3.0
 */
public class CommandProcessor { // объявляет класс для обработки команд
    private final CollectionManager collectionManager; // менеджер коллекции для выполнения операций
    private final DatabaseManager databaseManager; // менеджер базы данных для аутентификации и работы с бд

    /**
     * Создаёт новый процессор команд с указанными менеджерами
     * 
     * @param collectionManager менеджер для управления коллекцией
     * @param databaseManager менеджер для работы с базой данных
     */
    public CommandProcessor(CollectionManager collectionManager, DatabaseManager databaseManager) { // конструктор класса
        this.collectionManager = collectionManager; // сохраняем ссылку на менеджер коллекции
        this.databaseManager = databaseManager; // сохраняем ссылку на менеджер базы данных
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

            System.out.println("Обработка команды: " + command.getType() + " от пользователя: " + command.getLogin()); // выводим в консоль тип команды и логин пользователя

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
     * Команды REGISTER и LOGIN не требуют предварительной авторизации
     * 
     * @param command команда для выполнения
     * @return ответ с результатом выполнения команды
     */
    public Response process(Command command) { // метод обработки команды
        CommandType type = command.getType(); // получаем тип команды
        Object argument = command.getArgument(); // получаем аргумент команды (может быть null)
        String login = command.getLogin(); // получаем логин пользователя из команды
        String password = command.getPassword(); // получаем пароль пользователя из команды

        if (type == CommandType.REGISTER) { // проверяем команду регистрации
            return processRegister(argument); // вызываем метод обработки регистрации
        }
        if (type == CommandType.LOGIN) { // проверяем команду авторизации
            return processLogin(argument); // вызываем метод обработки авторизации
        }

        if (login == null || password == null || login.isEmpty() || password.isEmpty()) { // проверяем наличие логина и пароля
            return new Response(ResponseStatus.ERROR, "Необходимо авторизоваться. Используйте: login <login> <password>"); // возвращаем ошибку - требуется авторизация
        }
        
        if (!databaseManager.authenticateUser(login, password)) { // проверяем подлинность пользователя в бд
            return new Response(ResponseStatus.ERROR, "Неверный логин или пароль. Выполните команду login"); // возвращаем ошибку - неверные учётные данные
        }

        switch (type) { // выбираем обработчик в зависимости от типа команды
            case HELP: return processHelp(); // вызов метода обработки help
            case INFO: return processInfo(); // вызов метода обработки info
            case SHOW: return processShow(); // вызов метода обработки show
            case ADD: return processAdd(argument, login); // вызов метода add с аргументом и логином
            case ADD_RANDOM: return processAddRandom(login); // вызов метода add_random с логином
            case UPDATE: return processUpdate(argument, login); // вызов метода update с аргументом и логином
            case REMOVE_BY_ID: return processRemoveById(argument, login); // вызов метода удаления по id с аргументом и логином
            case CLEAR: return processClear(login); // вызов метода очистки с логином
            case REMOVE_LAST: return processRemoveLast(login); // вызов метода удаления последнего элемента с логином
            case REMOVE_LOWER: return processRemoveLower(argument, login); // вызов метода удаления меньших элементов с аргументом и логином
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
    }

    /**
     * Обрабатывает команду register - регистрация нового пользователя
     * Сохраняет логин и хэш пароля в базу данных
     * 
     * @param argument массив из двух строк [login, password]
     * @return ответ с результатом регистрации
     */
    private Response processRegister(Object argument) { // метод обработки команды регистрации
        try { // начало блока перехвата исключений
            String[] credentials = (String[]) argument; // приводим аргумент к массиву строк
            String login = credentials[0]; // извлекаем логин из первого элемента
            String password = credentials[1]; // извлекаем пароль из второго элемента

            if (databaseManager.registerUser(login, password)) { // пытаемся зарегистрировать пользователя в бд
                return new Response(ResponseStatus.SUCCESS, "Регистрация успешна! Выполните команду login"); // возвращаем успех с приглашением авторизоваться
            } else { // если регистрация не удалась (пользователь уже существует)
                return new Response(ResponseStatus.ERROR, "Пользователь с таким логином уже существует"); // возвращаем ошибку
            }
        } catch (Exception e) { // обрабатываем любые исключения
            return new Response(ResponseStatus.ERROR, "Ошибка регистрации: " + e.getMessage()); // возвращаем ошибку с деталями
        }
    }

    /**
     * Обрабатывает команду login - авторизация пользователя
     * Проверяет логин и пароль в базе данных
     * 
     * @param argument массив из двух строк [login, password]
     * @return ответ с результатом авторизации
     */
    private Response processLogin(Object argument) { // метод обработки команды авторизации
        try { // начало блока перехвата исключений
            String[] credentials = (String[]) argument; // приводим аргумент к массиву строк
            String login = credentials[0]; // извлекаем логин из первого элемента
            String password = credentials[1]; // извлекаем пароль из второго элемента

            if (databaseManager.authenticateUser(login, password)) { // проверяем подлинность пользователя в бд
                return new Response(ResponseStatus.SUCCESS, "Авторизация успешна! Добро пожаловать, " + login); // возвращаем успех с приветствием
            } else { // если аутентификация не пройдена
                return new Response(ResponseStatus.ERROR, "Неверный логин или пароль"); // возвращаем ошибку
            }
        } catch (Exception e) { // обрабатываем любые исключения
            return new Response(ResponseStatus.ERROR, "Ошибка авторизации: " + e.getMessage()); // возвращаем ошибку с деталями
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
            "  add_random - добавить случайное транспортное средство\n" + // описание команды add_random
            "  update <id> - обновить элемент по ID (только свои)\n" + // описание команды update
            "  remove_by_id <id> - удалить элемент по ID (только свои)\n" + // описание команды remove_by_id
            "  clear - очистить коллекцию (только свои)\n" + // описание команды clear
            "  remove_last - удалить последний элемент (только свои)\n" + // описание команды remove_last
            "  remove_lower - удалить элементы, меньшие заданного (только свои)\n" + // описание команды remove_lower
            "  sort - отсортировать коллекцию\n" + // описание команды sort
            "  sum_of_capacity - сумма грузоподъёмностей\n" + // описание команды sum_of_capacity
            "  filter_by_capacity <capacity> - фильтр по грузоподъёмности\n" + // описание команды filter_by_capacity
            "  filter_less_than_type <type> - фильтр по типу\n" + // описание команды filter_less_than_type
            "  execute_script <file> - выполнить скрипт\n" + // описание команды execute_script
            "  register <login> <password> - регистрация\n" + // описание команды register
            "  login <login> <password> - авторизация\n" + // описание команды login
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
     * Устанавливает владельца и сохраняет в базу данных
     * 
     * @param argument объект vehicle для добавления
     * @param login логин владельца
     * @return ответ с результатом операции
     */
    private Response processAdd(Object argument, String login) { // метод обработки команды add
        try { // начало блока перехвата исключений
            Vehicle vehicle = (Vehicle) argument; // приводим аргумент к типу vehicle
            vehicle.setOwnerLogin(login); // устанавливаем логин владельца

            int generatedId = databaseManager.addVehicle(vehicle); // сохраняем транспортное средство в бд и получаем сгенерированный id
            if (generatedId == -1) { // проверяем, успешно ли сохранение
                return new Response(ResponseStatus.ERROR, "Ошибка добавления в БД"); // возвращаем ошибку
            }

            vehicle.setId(generatedId); // устанавливаем полученный id
            collectionManager.add(vehicle); // добавляем элемент в коллекцию

            return new Response(ResponseStatus.SUCCESS, "Элемент добавлен с ID: " + generatedId); // возвращаем успех с id добавленного элемента
        } catch (ClassCastException e) { // обрабатываем ошибку приведения типов
            return new Response(ResponseStatus.ERROR, "Ошибка: неверный тип аргумента"); // возвращаем ошибку о неверном типе
        } catch (Exception e) { // обрабатываем любые другие исключения
            return new Response(ResponseStatus.ERROR, "Ошибка добавления: " + e.getMessage()); // возвращаем ошибку с деталями
        }
    }

    /**
     * Обрабатывает команду add_random - добавляет случайный элемент в коллекцию
     * Генерирует случайное транспортное средство и сохраняет его
     * 
     * @param login логин владельца
     * @return ответ с результатом операции
     */
    private Response processAddRandom(String login) { // метод обработки команды add_random
        try { // начало блока перехвата исключений
            Vehicle randomVehicle = generateRandomVehicle(login); // генерируем случайное транспортное средство
            
            int generatedId = databaseManager.addVehicle(randomVehicle); // сохраняем в бд и получаем id
            if (generatedId == -1) { // проверяем, успешно ли сохранение
                return new Response(ResponseStatus.ERROR, "Ошибка добавления в БД"); // возвращаем ошибку
            }
            
            randomVehicle.setId(generatedId); // устанавливаем полученный id
            collectionManager.add(randomVehicle); // добавляем элемент в коллекцию
            
            return new Response(ResponseStatus.SUCCESS, "Случайный элемент добавлен с ID: " + generatedId + "\n" + randomVehicle); // возвращаем успех с id и данными добавленного элемента
        } catch (Exception e) { // обрабатываем любые исключения
            return new Response(ResponseStatus.ERROR, "Ошибка добавления случайного элемента: " + e.getMessage()); // возвращаем ошибку с деталями
        }
    }

    /**
     * Генерирует случайное транспортное средство с заполненными полями
     * Используется для команды add_random
     * 
     * @param login логин владельца
     * @return сгенерированный объект Vehicle со случайными параметрами
     */
    private Vehicle generateRandomVehicle(String login) { // метод генерации случайного транспортного средства
        Vehicle vehicle = new Vehicle(); // создаём новый пустой объект транспортного средства
        
        String[] names = {"Car", "Truck", "Bus", "Motorcycle", "Bicycle", "Van", "SUV", "Sedan", "Hatchback", "Convertible"}; // массив возможных имён
        String randomName = names[new Random().nextInt(names.length)] + "_" + (System.currentTimeMillis() % 1000); // генерируем имя из случайного слова и числа из времени
        vehicle.setName(randomName); // устанавливаем случайное имя
        
        Coordinates coords = new Coordinates(); // создаём новый объект координат
        coords.setX(1 + new Random().nextDouble() * 635); // генерируем случайную координату x от 1 до 636
        coords.setY(new Random().nextInt(1000) - 500); // генерируем случайную координату y от -500 до 499
        vehicle.setCoordinates(coords); // устанавливаем координаты
        
        vehicle.setEnginePower(50 + new Random().nextDouble() * 450); // генерируем случайную мощность двигателя от 50 до 500
        
        vehicle.setCapacity(1 + new Random().nextDouble() * 1000); // генерируем случайную вместимость от 1 до 1001
        
        VehicleType[] types = VehicleType.values(); // получаем массив всех типов транспортных средств
        vehicle.setType(types[new Random().nextInt(types.length)]); // выбираем случайный тип
        
        FuelType[] fuelTypes = FuelType.values(); // получаем массив всех типов топлива
        if (new Random().nextBoolean()) { // с вероятностью 50% устанавливаем тип топлива
            vehicle.setFuelType(fuelTypes[new Random().nextInt(fuelTypes.length)]); // устанавливаем случайный тип топлива
        } else { // иначе
            vehicle.setFuelType(null); // устанавливаем null (топливо не указано)
        }
        
        vehicle.setOwnerLogin(login); // устанавливаем логин владельца
        
        return vehicle; // возвращаем сгенерированное транспортное средство
    }

    /**
     * Обрабатывает команду update - обновляет существующий элемент по id
     * Проверяет права владения перед обновлением
     * 
     * @param argument массив объектов [id, newVehicle]
     * @param login логин пользователя для проверки прав
     * @return ответ с результатом операции
     */
    private Response processUpdate(Object argument, String login) { // метод обработки команды update
        try { // начало блока перехвата исключений
            Object[] args = (Object[]) argument; // приводим аргумент к массиву объектов
            int id = (int) args[0]; // извлекаем id из первого элемента массива
            Vehicle newVehicle = (Vehicle) args[1]; // извлекаем новый vehicle из второго элемента

            Vehicle existing = collectionManager.getById(id); // получаем существующий элемент по id
            if (existing == null) { // проверяем, существует ли элемент
                return new Response(ResponseStatus.ERROR, "Элемент с ID " + id + " не найден"); // возвращаем ошибку, если элемент не найден
            }
            if (!existing.getOwnerLogin().equals(login)) { // проверяем, принадлежит ли элемент пользователю
                return new Response(ResponseStatus.ERROR, "У вас нет прав на изменение этого элемента"); // возвращаем ошибку, если нет прав
            }

            newVehicle.setId(id); // устанавливаем id
            newVehicle.setOwnerLogin(login); // устанавливаем владельца
            newVehicle.setCreationDate(existing.getCreationDate()); // сохраняем оригинальную дату создания

            if (!databaseManager.updateVehicle(newVehicle)) { // обновляем запись в бд и проверяем успех
                return new Response(ResponseStatus.ERROR, "Ошибка обновления в БД"); // возвращаем ошибку
            }

            collectionManager.updateById(id, newVehicle); // обновляем элемент в коллекции
            return new Response(ResponseStatus.SUCCESS, "Элемент с ID " + id + " обновлен"); // возвращаем сообщение об успехе
        } catch (Exception e) { // обрабатываем любые исключения
            return new Response(ResponseStatus.ERROR, "Ошибка обновления: " + e.getMessage()); // возвращаем ошибку с деталями
        }
    }

    /**
     * Обрабатывает команду remove_by_id - удаляет элемент по идентификатору
     * Проверяет права владения перед удалением
     * 
     * @param argument целочисленный id удаляемого элемента
     * @param login логин пользователя для проверки прав
     * @return ответ с результатом операции
     */
    private Response processRemoveById(Object argument, String login) { // метод обработки команды remove_by_id
        try { // начало блока перехвата исключений
            int id = (int) argument; // приводим аргумент к целому числу

            Vehicle existing = collectionManager.getById(id); // получаем существующий элемент по id
            if (existing == null) { // проверяем, существует ли элемент
                return new Response(ResponseStatus.ERROR, "Элемент с ID " + id + " не найден"); // возвращаем ошибку
            }
            if (!existing.getOwnerLogin().equals(login)) { // проверяем, принадлежит ли элемент пользователю
                return new Response(ResponseStatus.ERROR, "У вас нет прав на удаление этого элемента"); // возвращаем ошибку
            }

            if (!databaseManager.deleteVehicle(id)) { // удаляем из бд и проверяем успех
                return new Response(ResponseStatus.ERROR, "Ошибка удаления из БД"); // возвращаем ошибку
            }

            collectionManager.removeById(id); // удаляем из коллекции
            return new Response(ResponseStatus.SUCCESS, "Элемент с ID " + id + " удален"); // возвращаем сообщение об успехе
        } catch (Exception e) { // обрабатываем любые исключения
            return new Response(ResponseStatus.ERROR, "Ошибка удаления: " + e.getMessage()); // возвращаем ошибку с деталями
        }
    }

    /**
     * Обрабатывает команду clear - удаляет все элементы текущего пользователя
     * Удаляет из базы данных и коллекции только элементы, принадлежащие пользователю
     * 
     * @param login логин пользователя
     * @return ответ с результатом операции
     */
    private Response processClear(String login) { // метод обработки команды clear
        List<Vehicle> userVehicles = collectionManager.getByOwner(login); // получаем все элементы пользователя
        for (Vehicle v : userVehicles) { // проходим по всем элементам пользователя
            databaseManager.deleteVehicle(v.getId()); // удаляем каждый элемент из бд
        }
        collectionManager.clearByOwner(login); // удаляем все элементы пользователя из коллекции
        return new Response(ResponseStatus.SUCCESS, "Ваши элементы удалены из коллекции"); // возвращаем сообщение об успехе
    }

    /**
     * Обрабатывает команду remove_last - удаляет последний элемент из коллекции
     * Проверяет, принадлежит ли последний элемент пользователю
     * 
     * @param login логин пользователя для проверки прав
     * @return ответ с результатом операции
     */
    private Response processRemoveLast(String login) { // метод обработки команды remove_last
        Vehicle last = collectionManager.getLast(); // получаем последний элемент коллекции
        if (last == null) { // проверяем, пуста ли коллекция
            return new Response(ResponseStatus.ERROR, "Коллекция пуста"); // возвращаем ошибку
        }
        if (!last.getOwnerLogin().equals(login)) { // проверяем, принадлежит ли последний элемент пользователю
            return new Response(ResponseStatus.ERROR, "Последний элемент принадлежит другому пользователю"); // возвращаем ошибку
        }

        databaseManager.deleteVehicle(last.getId()); // удаляем элемент из бд
        collectionManager.removeLast(); // удаляем последний элемент из коллекции
        return new Response(ResponseStatus.SUCCESS, "Последний элемент удален"); // возвращаем сообщение об успехе
    }

    /**
     * Обрабатывает команду remove_lower - удаляет все элементы, меньшие заданного
     * Удаляет только те элементы, которые принадлежат пользователю
     * 
     * @param argument эталонный объект vehicle для сравнения
     * @param login логин пользователя для проверки прав
     * @return ответ с количеством удалённых элементов
     */
    private Response processRemoveLower(Object argument, String login) { // метод обработки команды remove_lower
        try { // начало блока перехвата исключений
            Vehicle vehicle = (Vehicle) argument; // приводим аргумент к типу vehicle
            List<Vehicle> toRemove = collectionManager.getLowerThan(vehicle); // получаем список элементов, меньших заданного
            int removedCount = 0; // счётчик удалённых элементов

            for (Vehicle v : toRemove) { // проходим по всем меньшим элементам
                if (v.getOwnerLogin().equals(login)) { // проверяем, принадлежит ли элемент пользователю
                    if (databaseManager.deleteVehicle(v.getId())) { // удаляем из бд и проверяем успех
                        collectionManager.removeById(v.getId()); // удаляем из коллекции
                        removedCount++; // увеличиваем счётчик
                    }
                }
            }
            return new Response(ResponseStatus.SUCCESS, "Удалено элементов: " + removedCount); // возвращаем количество удалённых
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
        collectionManager.sort(); // вызываем сортировку коллекции
        return new Response(ResponseStatus.SUCCESS, "Коллекция отсортирована"); // возвращаем сообщение об успехе
    }

    /**
     * Обрабатывает команду sum_of_capacity - вычисляет сумму вместимостей всех элементов коллекции
     * 
     * @return ответ с суммой вместимостей
     */
    private Response processSumOfCapacity() { // метод обработки команды sum_of_capacity
        double sum = collectionManager.getSumOfCapacity(); // получаем сумму вместимостей
        return new Response(ResponseStatus.SUCCESS, "Сумма грузоподъёмностей: " + sum); // возвращаем результат
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
        } catch (Exception e) { // обрабатываем любые исключения
            return new Response(ResponseStatus.ERROR, "Ошибка фильтрации: " + e.getMessage()); // возвращаем ошибку с деталями
        }
    }

    /**
     * Обрабатывает команду filter_less_than_type - фильтрует элементы по типу (меньше указанного)
     * 
     * @param argument тип для сравнения (VehicleType)
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
        } catch (Exception e) { // обрабатываем любые исключения
            return new Response(ResponseStatus.ERROR, "Ошибка фильтрации: " + e.getMessage()); // возвращаем ошибку с деталями
        }
    }
}