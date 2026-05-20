package server; // класс находится в пакете server

import server.collection.CollectionManager; // импорт менеджера коллекции для управления данными
import server.auth.AuthManager; // импорт менеджера аутентификации
import server.database.DatabaseManager; // импорт менеджера базы данных
import server.database.UserRepository; // импорт репозитория пользователей
import server.database.VehicleRepository; // импорт репозитория транспортных средств
import server.threads.ThreadPoolManager; // импорт менеджера пулов потоков
import common.Request; // импорт класса запроса из общей модели
import common.Response; // импорт класса ответа из общей модели
import common.ResponseStatus; // импорт перечисления статусов ответа
import common.AuthData; // импорт данных аутентификации из общей модели
import java.sql.SQLException; // импорт исключения для ошибок sql
import java.nio.ByteBuffer; // импорт буфера байтов для nio
import java.io.ByteArrayInputStream; // импорт потокового ввода из массива байтов
import java.io.DataInputStream; // импорт потока для чтения примитивных типов
import java.io.IOException; // импорт исключения для ошибок ввода-вывода
import java.util.concurrent.RecursiveAction; // импорт рекурсивного действия для forkjoinpool
import java.net.InetSocketAddress; // импорт класса для создания адреса сокета
import java.nio.channels.*; // импорт всех классов для неблокирующего ввода-вывода
import java.util.Iterator; // импорт итератора для обхода коллекций
import java.util.Scanner; // импорт сканера для чтения ввода с консоли

/**
 * главный класс сервера, реализующий неблокирующий ввод-вывод с использованием selector
 * обрабатывает входящие подключения, читает запросы и отправляет ответы клиентам
 * поддерживает управление через консольные команды
 * 
 * @author anni
 * @version 3.0
 */
public class Server { // объявляет главный класс сервера
    private static final int PORT = 1067; // фиксированный порт для прослушивания подключений
    private static DatabaseManager dbManager; // менеджер базы данных для работы с бд
    private static AuthManager authManager; // менеджер аутентификации пользователей
    private static ThreadPoolManager threadPoolManager; // менеджер пулов потоков
    private static volatile boolean isRunning = true; // флаг работы сервера (volatile для видимости между потоками)

    /**
     * точка входа в программу сервера
     * инициализирует менеджеры, запускает консольный поток обработки команд
     * и основной цикл обработки событий selector
     * 
     * @param args аргументы командной строки (первый аргумент - имя пользователя бд, второй - пароль)
     * @throws IOException при ошибках ввода-вывода при открытии каналов или селектора
     */
    public static void main(String[] args) throws IOException { // главный метод, точка входа в программу
        String dbUser = System.getenv().getOrDefault("LAB_USER", "studs"); // получаем пользователя бд из переменной окружения или значение по умолчанию
        String dbPassword = System.getenv().getOrDefault("LAB_PASSWORD", "studs"); // получаем пароль бд из переменной окружения или значение по умолчанию
        
        if (args.length >= 2) { // проверяем, переданы ли аргументы в командной строке
            dbUser = args[0]; // берём пользователя бд из первого аргумента
            dbPassword = args[1]; // берём пароль бд из второго аргумента
        }

        try { // начало блока перехвата исключений
            dbManager = new DatabaseManager(dbUser, dbPassword); // создаём менеджер базы данных
            dbManager.connect(); // подключаемся к базе данных
            dbManager.initSchema(); // инициализируем схему базы данных

            UserRepository userRepository = new UserRepository(dbManager); // создаём репозиторий пользователей
            authManager = new AuthManager(userRepository); // создаём менеджер аутентификации
            
            threadPoolManager = new ThreadPoolManager(); // создаём менеджер пулов потоков
            System.out.println("Инициализация БД и пулов потоков завершена"); // выводим сообщение об успешной инициализации
        } catch (SQLException | ClassNotFoundException e) { // обрабатываем ошибки бд или отсутствие драйвера
            System.err.println("Ошибка инициализации БД: " + e.getMessage()); // выводим сообщение об ошибке
            System.exit(1); // завершаем программу с кодом ошибки
        }

        Runtime.getRuntime().addShutdownHook(new Thread(() -> { // добавляем хук для завершения виртуальной машины
            try { // начало блока перехвата исключений
                System.out.println("Завершение работы сервера..."); // выводим сообщение о завершении
                if (threadPoolManager != null) threadPoolManager.shutdown(); // завершаем пулы потоков, если они существуют
                if (dbManager != null) dbManager.close(); // закрываем соединение с бд, если оно существует
                System.out.println("Ресурсы освобождены"); // выводим сообщение об освобождении ресурсов
            } catch (Exception e) { // обрабатываем ошибки при сохранении
                System.err.println("Ошибка при завершении: " + e.getMessage()); // выводим сообщение об ошибке
            }
        })); // конец добавления хука

        Thread serverConsoleThread = new Thread(() -> { // создаём поток для обработки консольных команд сервера
            Scanner scanner = new Scanner(System.in); // создаём сканер для чтения из стандартного ввода
            while (isRunning) { // цикл, пока сервер работает
                String input = scanner.nextLine().trim(); // читаем строку из консоли и удаляем пробелы по краям
                if (input.isEmpty()) continue; // пропускаем пустые строки
                
                if (input.equalsIgnoreCase("info")) { // проверяем команду info
                    System.out.println("Сервер работает. Используется БД PostgreSQL"); // выводим информацию о сервере
                    System.out.println("Активных клиентов: информация недоступна в консоли"); // выводим сообщение о клиентах
                } else if (input.equalsIgnoreCase("exit") || input.equalsIgnoreCase("quit")) { // проверяем команды выхода
                    System.out.println("Завершение работы сервера..."); // выводим сообщение о завершении
                    isRunning = false; // устанавливаем флаг остановки
                    System.exit(0); // принудительно завершаем виртуальную машину
                } else { // если команда не распознана
                    System.out.println("Неизвестная серверная команда. Доступные: info, exit"); // выводим подсказку
                }
            }
            scanner.close(); // закрываем сканер при выходе из цикла
        }); // конец создания консольного потока
        serverConsoleThread.setDaemon(true); // устанавливаем поток как демон (будет завершен при выходе из основного потока)
        serverConsoleThread.start(); // запускаем поток обработки консольных команд

        Selector selector = Selector.open(); // открываем мультиплексор для неблокирующего ввода-вывода
        ServerSocketChannel serverChannel = ServerSocketChannel.open(); // открываем серверный канал
        serverChannel.bind(new InetSocketAddress(PORT)); // привязываем канал к порту
        serverChannel.configureBlocking(false); // переводим канал в неблокирующий режим
        serverChannel.register(selector, SelectionKey.OP_ACCEPT); // регистрируем канал с интересом к принятию подключений

        System.out.println("Сервер запущен на порту " + PORT); // выводим информацию о запуске
        System.out.println("База данных: PostgreSQL (pg/studs)"); // выводим информацию о бд
        System.out.println("Режим: многопоточный (ForkJoinPool + CachedThreadPool)"); // выводим информацию о режиме работы
        System.out.println("Серверные команды: info, exit\n"); // выводим список доступных команд сервера

        while (isRunning) { // основной цикл сервера
            selector.select(); // блокируемся до появления событий на зарегистрированных каналах
            Iterator<SelectionKey> keys = selector.selectedKeys().iterator(); // получаем итератор по выбранным ключам

            while (keys.hasNext()) { // обходим все ключи с событиями
                SelectionKey key = keys.next(); // получаем следующий ключ
                keys.remove(); // удаляем ключ из выбранного набора (чтобы не обработать повторно)

                if (!key.isValid()) continue; // пропускаем невалидные ключи

                if (key.isAcceptable()) { // проверяем событие принятия подключения
                    ConnectionAcceptor.accept(key, selector); // принимаем новое подключение
                } else if (key.isReadable()) { // проверяем событие готовности к чтению
                    ClientHandler handler = (ClientHandler) key.attachment(); // получаем обработчик клиента из ключа
                    threadPoolManager.getReadPool().execute(new ReadTask(handler, key, selector)); // запускаем задачу чтения в пуле forkjoinpool
                } else if (key.isWritable()) { // проверяем событие готовности к записи
                    ClientHandler handler = (ClientHandler) key.attachment(); // получаем обработчик клиента из ключа
                    threadPoolManager.getWritePool().execute(new WriteTask(handler, key)); // запускаем задачу записи в пуле forkjoinpool
                }
            }
        }
    }
    
    /**
     * закрывает соединение с клиентом и освобождает ресурсы
     * отменяет ключ, закрывает канал и очищает обработчик
     * 
     * @param key ключ селектора для данного соединения
     * @param handler обработчик клиента
     */
    private static void closeConnection(SelectionKey key, ClientHandler handler) { // метод закрытия соединения
        if (key != null) key.cancel(); // отменяем ключ селектора если он существует
        if (handler != null) { // проверяем существование обработчика
            try { // начало блока перехвата исключений
                if (handler.getChannel() != null && handler.getChannel().isOpen()) { // проверяем, что канал существует и открыт
                    handler.getChannel().close(); // закрываем канал
                }
            } catch (IOException e) { // обрабатываем ошибки закрытия
                System.err.println("Ошибка при закрытии канала: " + e.getMessage()); // выводим сообщение об ошибке
            }
            handler.close(); // закрываем обработчик и очищаем его данные
        }
    }

    /**
     * задача для чтения данных от клиента в отдельном потоке (forkjoinpool)
     */
    static class ReadTask extends RecursiveAction { // внутренний класс задачи чтения
        private final ClientHandler handler; // обработчик клиента
        private final SelectionKey key; // ключ селектора
        private final Selector selector; // селектор

        ReadTask(ClientHandler handler, SelectionKey key, Selector selector) { // конструктор задачи чтения
            this.handler = handler; // сохраняем обработчик
            this.key = key; // сохраняем ключ
            this.selector = selector; // сохраняем селектор
        }

        @Override
        protected void compute() { // метод вычисления задачи (основная логика)
            try { // начало блока перехвата исключений
                ByteBuffer buffer = ByteBuffer.allocate(65536); // создаём буфер для чтения данных размером 64кб
                int bytesRead = handler.getChannel().read(buffer); // читаем данные из канала в буфер
                
                if (bytesRead == -1) { // проверяем, закрыт ли канал (конец потока)
                    closeConnection(key, handler); // закрываем соединение
                    return; // выходим из метода
                }
                
                if (bytesRead > 0) { // проверяем, есть ли прочитанные данные
                    buffer.flip(); // переключаем буфер из режима записи в режим чтения
                    byte[] data = new byte[bytesRead]; // создаём массив для хранения прочитанных данных
                    buffer.get(data); // копируем данные из буфера в массив
                    handler.getPendingData().write(data); // записываем данные в буфер ожидающих данных
                    extractCompleteMessages(handler); // извлекаем полные сообщения из буфера
                    
                    if (handler.hasCompleteRequest() && !handler.isProcessing()) { // проверяем, есть ли полный запрос и не обрабатывается ли уже
                        handler.setProcessing(true); // устанавливаем флаг обработки
                        threadPoolManager.getProcessingPool().execute(() -> { // запускаем обработку в cachedthreadpool
                            try { // начало блока перехвата исключений
                                processRequestWithAuth(handler); // обрабатываем запрос с аутентификацией
                                key.interestOps(SelectionKey.OP_WRITE); // меняем интерес на запись
                                selector.wakeup(); // пробуждаем селектор
                            } catch (Exception e) { // обрабатываем любые исключения при обработке
                                System.err.println("Ошибка обработки: " + e.getMessage()); // выводим сообщение об ошибке
                                Response errorResponse = new Response(ResponseStatus.ERROR, "Ошибка обработки: " + e.getMessage()); // создаём ответ с ошибкой
                                handler.setResponseToSend(errorResponse); // сохраняем ответ для отправки
                                try { // начало блока перехвата исключений
                                    key.interestOps(SelectionKey.OP_WRITE); // меняем интерес на запись
                                    selector.wakeup(); // пробуждаем селектор
                                } catch (Exception ex) { // обрабатываем ошибки при смене интереса
                                    closeConnection(key, handler); // закрываем соединение
                                }
                            }
                        }); // конец выполнения задачи в пуле
                    }
                }
            } catch (IOException e) { // обрабатываем ошибки ввода-вывода
                System.err.println("Ошибка чтения: " + e.getMessage()); // выводим сообщение об ошибке
                closeConnection(key, handler); // закрываем соединение
            }
        }

        /**
         * извлекает полные сообщения из потока накопленных байтов
         * формат сообщения: 4 байта длины + тело сообщения
         * 
         * @param handler обработчик клиента с накопленными данными
         */
        private void extractCompleteMessages(ClientHandler handler) { // метод извлечения полных сообщений
            byte[] fullData = handler.getPendingData().toByteArray(); // получаем все накопленные данные из буфера
            ByteArrayInputStream bais = new ByteArrayInputStream(fullData); // создаём поток для чтения из массива байтов
            DataInputStream dis = new DataInputStream(bais); // создаём поток для чтения примитивных типов
            
            try { // начало блока перехвата исключений
                bais.mark(fullData.length); // отмечаем позицию для возможного возврата
                
                while (true) { // бесконечный цикл извлечения сообщений
                    int messageLength; // переменная для длины сообщения
                    try { // начало блока перехвата исключений
                        messageLength = dis.readInt(); // читаем длину сообщения (первые 4 байта)
                    } catch (IOException e) { // обрабатываем ошибку чтения (недостаточно данных)
                        bais.reset(); // возвращаемся к отмеченной позиции
                        break; // выходим из цикла
                    }
                    
                    if (messageLength <= 0 || messageLength > 10 * 1024 * 1024) { // проверяем валидность длины (до 10мб)
                        handler.getPendingData().reset(); // сбрасываем буфер ожидающих данных
                        return; // выходим из метода
                    }
                    
                    byte[] messageData = new byte[messageLength]; // создаём массив для сообщения
                    try { // начало блока перехвата исключений
                        dis.readFully(messageData); // читаем ровно messageLength байт
                    } catch (IOException e) { // обрабатываем ошибку чтения (недостаточно данных)
                        bais.reset(); // возвращаемся к отмеченной позиции
                        break; // выходим из цикла
                    }
                    
                    handler.setCompleteRequest(messageData); // сохраняем полное сообщение в обработчике
                    
                    byte[] remaining = new byte[bais.available()]; // создаём массив для оставшихся данных
                    bais.read(remaining); // читаем оставшиеся данные
                    handler.getPendingData().reset(); // сбрасываем буфер ожидающих данных
                    handler.getPendingData().write(remaining); // записываем оставшиеся данные обратно
                    
                    bais = new ByteArrayInputStream(remaining); // создаём новый поток с оставшимися данными
                    dis = new DataInputStream(bais); // создаём новый поток чтения
                    bais.mark(remaining.length); // отмечаем позицию в новом потоке
                }
            } catch (IOException e) { // обрабатываем любые исключения
                handler.getPendingData().reset(); // сбрасываем буфер в случае ошибки
            }
        }
    }

    /**
     * задача для отправки ответа клиенту в отдельном потоке (forkjoinpool)
     */
    static class WriteTask extends RecursiveAction { // внутренний класс задачи записи
        private final ClientHandler handler; // обработчик клиента
        private final SelectionKey key; // ключ селектора

        WriteTask(ClientHandler handler, SelectionKey key) { // конструктор задачи записи
            this.handler = handler; // сохраняем обработчик
            this.key = key; // сохраняем ключ
        }

        @Override
        protected void compute() { // метод вычисления задачи (основная логика)
            try { // начало блока перехвата исключений
                if (handler.hasResponseToSend()) { // проверяем, есть ли ответ для отправки
                    boolean fullySent = ResponseSender.sendResponse(handler); // отправляем ответ и получаем флаг полной отправки
                    if (fullySent) { // проверяем, отправлен ли ответ полностью
                        handler.setProcessing(false); // снимаем флаг обработки
                        handler.clearCompleteRequest(); // очищаем сохранённый запрос
                        key.interestOps(SelectionKey.OP_READ); // меняем интерес обратно на чтение
                    }
                } else if (!handler.hasCompleteRequest()) { // если нет полного запроса
                    key.interestOps(SelectionKey.OP_READ); // меняем интерес на чтение
                }
            } catch (IOException e) { // обрабатываем ошибки ввода-вывода
                System.err.println("Ошибка отправки: " + e.getMessage()); // выводим сообщение об ошибке
                closeConnection(key, handler); // закрываем соединение
            }
        }
    }

    /**
     * обрабатывает запрос с аутентификацией
     * 
     * @param handler обработчик клиента
     * @throws IOException если ошибка ввода-вывода
     * @throws ClassNotFoundException если класс не найден при десериализации
     * @throws SQLException если ошибка при работе с бд
     */
    private static void processRequestWithAuth(ClientHandler handler) throws IOException, ClassNotFoundException, SQLException { // метод обработки запроса с аутентификацией
        byte[] requestData = handler.getCompleteRequest(); // получаем массив байтов запроса
        Request request = RequestReader.deserializeRequest(requestData); // десериализуем запрос
        AuthData auth = request.getAuth(); // получаем данные аутентификации из запроса
        
        if (auth == null) { // проверяем, переданы ли данные аутентификации
            Response response = new Response(ResponseStatus.ERROR, "Требуется аутентификация"); // создаём ответ с ошибкой
            handler.setResponseToSend(response); // сохраняем ответ
            return; // выходим из метода
        }
        
        boolean authenticated = authManager.authenticate(auth); // проверяем аутентификацию
        if (!authenticated) { // если аутентификация не пройдена
            Response response = new Response(ResponseStatus.ERROR, "Неверный логин или пароль"); // создаём ответ с ошибкой
            handler.setResponseToSend(response); // сохраняем ответ
            return; // выходим из метода
        }
        
        String username = auth.getUsername(); // получаем имя пользователя из данных аутентификации
        
        CollectionManager collectionManager = (CollectionManager) handler.getAttachment(); // получаем менеджер коллекции из обработчика
        if (collectionManager == null || !collectionManager.getCurrentUser().equals(username)) { // проверяем, нужно ли создать новый менеджер
            VehicleRepository vehicleRepository = new VehicleRepository(dbManager); // создаём репозиторий транспортных средств
            collectionManager = new CollectionManager(vehicleRepository, username); // создаём менеджер коллекции для пользователя
            handler.setAttachment(collectionManager); // сохраняем менеджер в обработчике
        }
        
        CommandProcessor processor = new CommandProcessor(collectionManager); // создаём процессор команд с менеджером коллекции
        Response response = processor.process(request.getCommand()); // обрабатываем команду
        handler.setResponseToSend(response); // сохраняем ответ
    }
}