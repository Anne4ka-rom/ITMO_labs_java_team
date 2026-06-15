package server; // класс находится в пакете server

import server.collection.CollectionManager; // импорт менеджера коллекции для управления данными
import server.database.DatabaseManager; // импорт менеджера базы данных

import java.io.IOException; // импорт исключения для ошибок ввода-вывода
import java.net.InetSocketAddress; // импорт класса для создания адреса сокета
import java.nio.channels.*; // импорт всех классов для неблокирующего ввода-вывода
import java.util.Iterator; // импорт итератора для обхода коллекций
import java.util.Scanner; // импорт сканера для чтения ввода с консоли
import java.util.concurrent.*; // импорт всех классов для многопоточности

/**
 * Главный класс сервера с многопоточной обработкой запросов
 * Реализует неблокирующий ввод-вывод с использованием Selector
 * Использует ForkJoinPool для чтения и отправки данных, CachedThreadPool для обработки команд
 * 
 * @author Anni
 * @version 3.0
 */
public class Server { // объявляет главный класс сервера
    private static final int PORT = 1067; // фиксированный порт для прослушивания подключений
    private static final int FORK_JOIN_PARALLELISM = 4; // уровень параллелизма для forkjoinpool (количество потоков)

    private static CollectionManager collectionManager; // менеджер коллекции для управления данными
    private static DatabaseManager databaseManager; // менеджер базы данных для работы с бд
    private static volatile boolean isRunning = true; // флаг работы сервера (volatile для видимости между потоками)

    private static final ForkJoinPool readPool = new ForkJoinPool(FORK_JOIN_PARALLELISM); // пул forkjoinpool для чтения запросов от клиентов
    private static final ExecutorService processingPool = Executors.newCachedThreadPool(); // пул cachedthreadpool для обработки команд
    private static final ForkJoinPool sendPool = new ForkJoinPool(FORK_JOIN_PARALLELISM); // пул forkjoinpool для отправки ответов клиентам

    /**
     * Точка входа в программу сервера
     * Инициализирует базу данных, загружает коллекцию, запускает консольный поток
     * и основной цикл обработки событий Selector
     * 
     * @param args аргументы командной строки (не используются)
     * @throws IOException при ошибках ввода-вывода при открытии каналов или селектора
     */
    public static void main(String[] args) throws IOException { // главный метод, точка входа в программу
        databaseManager = new DatabaseManager(); // создаём менеджер базы данных
        if (!databaseManager.connect()) { // пытаемся подключиться к базе данных и проверяем результат
            System.err.println("Не удалось подключиться к БД. Сервер остановлен."); // выводим сообщение об ошибке подключения
            return; // выходим из метода, завершая работу сервера
        }

        collectionManager = new CollectionManager(databaseManager); // создаём менеджер коллекции с подключением к бд
        collectionManager.loadFromDatabase(); // загружаем коллекцию из базы данных

        Runtime.getRuntime().addShutdownHook(new Thread(() -> { // добавляем хук для завершения виртуальной машины
            System.out.println("Сервер завершает работу..."); // выводим сообщение о завершении работы
            databaseManager.disconnect(); // закрываем соединение с базой данных
        })); // конец добавления хука

        Thread serverConsoleThread = new Thread(() -> { // создаём поток для обработки консольных команд сервера
            Scanner scanner = new Scanner(System.in); // создаём сканер для чтения из стандартного ввода
            while (isRunning) { // цикл, пока сервер работает
                String input = scanner.nextLine().trim(); // читаем строку из консоли и удаляем пробелы по краям
                if (input.isEmpty()) continue; // пропускаем пустые строки

                if (input.equalsIgnoreCase("save")) { // проверяем команду save
                    System.out.println("Данные автоматически сохраняются в БД при каждом изменении"); // выводим сообщение, что сохранение автоматическое
                } else if (input.equalsIgnoreCase("info")) { // проверяем команду info
                    System.out.println(collectionManager.getInfo()); // выводим информацию о коллекции
                } else if (input.equalsIgnoreCase("exit")) { // проверяем команду exit
                    System.out.println("Завершение работы сервера..."); // выводим сообщение о завершении
                    isRunning = false; // устанавливаем флаг остановки
                    readPool.shutdown(); // завершаем пул чтения (новые задачи не принимаются)
                    processingPool.shutdown(); // завершаем пул обработки (новые задачи не принимаются)
                    sendPool.shutdown(); // завершаем пул отправки (новые задачи не принимаются)
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
        System.out.println("Режим: многопоточный (ForkJoinPool для чтения/отправки, CachedThreadPool для обработки)"); // выводим информацию о режиме работы
        System.out.println("Серверные команды: info, exit\n"); // выводим список доступных команд сервера

        CommandProcessor processor = new CommandProcessor(collectionManager, databaseManager); // создаём процессор команд с менеджерами

        while (isRunning) { // основной цикл сервера
            try { // начало блока перехвата исключений
                selector.select(100); // блокируемся на 100 мс до появления событий на зарегистрированных каналах
                Iterator<SelectionKey> keys = selector.selectedKeys().iterator(); // получаем итератор по выбранным ключам

                while (keys.hasNext()) { // обходим все ключи с событиями
                    SelectionKey key = keys.next(); // получаем следующий ключ
                    keys.remove(); // удаляем ключ из выбранного набора (чтобы не обработать повторно)

                    if (!key.isValid()) continue; // пропускаем невалидные ключи

                    if (key.isAcceptable()) { // проверяем событие принятия подключения
                        ConnectionAcceptor.accept(key, selector); // принимаем новое подключение
                    } else if (key.isReadable()) { // проверяем событие готовности к чтению
                        ClientHandler handler = (ClientHandler) key.attachment(); // получаем обработчик клиента из ключа
                        readPool.submit(() -> { // отправляем задачу чтения в пул forkjoinpool
                            try { // начало блока перехвата исключений
                                RequestReader.readRequest(handler); // читаем запрос от клиента
                                if (handler.hasCompleteRequest() && !handler.isProcessing()) { // проверяем, есть ли полный запрос и не обрабатывается ли уже
                                    handler.setProcessing(true); // устанавливаем флаг обработки
                                    processingPool.submit(() -> { // отправляем задачу обработки в cachedthreadpool
                                        processor.processRequest(handler); // обрабатываем запрос
                                        if (handler.hasResponseToSend()) { // проверяем, есть ли ответ для отправки
                                            sendPool.submit(() -> { // отправляем задачу отправки в пул forkjoinpool
                                                try { // начало блока перехвата исключений
                                                    boolean fullySent = ResponseSender.sendResponse(handler); // отправляем ответ и получаем флаг полной отправки
                                                    if (fullySent) { // проверяем, отправлен ли ответ полностью
                                                        handler.setProcessing(false); // снимаем флаг обработки
                                                        handler.clearCompleteRequest(); // очищаем сохранённый запрос
                                                        key.interestOps(SelectionKey.OP_READ); // меняем интерес обратно на чтение
                                                        selector.wakeup(); // пробуждаем селектор
                                                    }
                                                } catch (IOException e) { // обрабатываем ошибки ввода-вывода при отправке
                                                    System.err.println("Ошибка отправки ответа: " + e.getMessage()); // выводим сообщение об ошибке
                                                    closeConnection(key, handler); // закрываем соединение
                                                }
                                            }); // конец отправки задачи в пул отправки
                                        }
                                    }); // конец отправки задачи в пул обработки
                                }
                            } catch (IOException e) { // обрабатываем ошибки ввода-вывода при чтении
                                System.err.println("Ошибка чтения от клиента: " + e.getMessage()); // выводим сообщение об ошибке
                                closeConnection(key, handler); // закрываем соединение
                            }
                        }); // конец отправки задачи в пул чтения
                    }
                }
            } catch (IOException e) { // обрабатываем ошибки в основном цикле селектора
                System.err.println("Ошибка в цикле селектора: " + e.getMessage()); // выводим сообщение об ошибке
            }
        }
    }

    /**
     * Закрывает соединение с клиентом и освобождает ресурсы
     * Отменяет ключ, закрывает канал и очищает обработчик
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
}