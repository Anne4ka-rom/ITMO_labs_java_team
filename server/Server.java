package server; // класс находится в пакете server

import server.collection.CollectionManager; // импорт менеджера коллекции для управления данными
import server.file.FileManager; // импорт менеджера для работы с файлом коллекции

import java.io.IOException; // импорт исключения для ошибок ввода-вывода
import java.net.InetSocketAddress; // импорт класса для создания адреса сокета
import java.nio.channels.*; // импорт всех классов для неблокирующего ввода-вывода
import java.util.Iterator; // импорт итератора для обхода коллекций
import java.util.Scanner; // импорт сканера для чтения ввода с консоли

/**
 * Главный класс сервера, реализующий неблокирующий ввод-вывод с использованием Selector
 * Обрабатывает входящие подключения, читает запросы и отправляет ответы клиентам
 * Поддерживает управление через консольные команды
 * 
 * @author Anni
 * @version 2.3
 */
public class Server { // объявляет главный класс сервера
    private static final int PORT = 1067; // фиксированный порт для прослушивания подключений
    private static CollectionManager collectionManager; // менеджер коллекции для хранения и обработки данных
    private static volatile boolean isRunning = true; // флаг работы сервера (volatile для видимости между потоками)

    /**
     * Точка входа в программу сервера
     * Инициализирует менеджеры, запускает консольный поток обработки команд
     * и основной цикл обработки событий Selector
     * 
     * @param args аргументы командной строки (первый аргумент - имя файла коллекции)
     * @throws IOException при ошибках ввода-вывода при открытии каналов или селектора
     */
    public static void main(String[] args) throws IOException { // главный метод, точка входа в программу
        String filename = "vehicles.xml"; // имя файла коллекции по умолчанию
        if (args.length > 0) filename = args[0]; // если передан аргумент командной строки, используем его как имя файла
        
        FileManager fileManager = new FileManager(filename); // создаем менеджер для работы с файлом
        collectionManager = new CollectionManager(fileManager); // создаем менеджер коллекции с привязкой к файлу

        Runtime.getRuntime().addShutdownHook(new Thread(() -> { // добавляем хук для завершения виртуальной машины
            try { // начало блока перехвата исключений
                collectionManager.save(); // сохраняем коллекцию при завершении работы
                System.out.println("Коллекция сохранена в файл при завершении работы сервера"); // выводим сообщение о сохранении
            } catch (Exception e) { // обрабатываем ошибки при сохранении
                System.err.println("Ошибка сохранения коллекции: " + e.getMessage()); // выводим сообщение об ошибке
            }
        }));

        Thread serverConsoleThread = new Thread(() -> { // создаем поток для обработки консольных команд сервера
            Scanner scanner = new Scanner(System.in); // создаем сканер для чтения из стандартного ввода
            while (isRunning) { // цикл, пока сервер работает
                String input = scanner.nextLine().trim(); // читаем строку из консоли и удаляем пробелы по краям
                if (input.isEmpty()) continue; // пропускаем пустые строки
                
                if (input.equalsIgnoreCase("save")) { // проверяем команду save (без учета регистра)
                    try { // начало блока перехвата исключений
                        collectionManager.save(); // сохраняем коллекцию
                        System.out.println("Коллекция сохранена в файл по команде сервера"); // выводим сообщение об успехе
                    } catch (Exception e) { // обрабатываем ошибки при сохранении
                        System.err.println("Ошибка сохранения: " + e.getMessage()); // выводим сообщение об ошибке
                    } // конец блока catch
                } else if (input.equalsIgnoreCase("info")) { // проверяем команду info
                    System.out.println(collectionManager.getInfo()); // выводим информацию о коллекции
                } else if (input.equalsIgnoreCase("exit") || input.equalsIgnoreCase("quit")) { // проверяем команды выхода
                    System.out.println("Завершение работы сервера..."); // выводим сообщение о завершении
                    isRunning = false; // устанавливаем флаг остановки
                    System.exit(0); // принудительно завершаем виртуальную машину
                } else { // если команда не распознана
                    System.out.println("Неизвестная серверная команда. Доступные: save, info, exit"); // выводим подсказку
                }
            }
            scanner.close(); // закрываем сканер при выходе из цикла
        });
        serverConsoleThread.setDaemon(true); // устанавливаем поток как демон (будет завершен при выходе из основного потока)
        serverConsoleThread.start(); // запускаем поток обработки консольных команд

        Selector selector = Selector.open(); // открываем мультиплексор для неблокирующего ввода-вывода
        ServerSocketChannel serverChannel = ServerSocketChannel.open(); // открываем серверный канал
        serverChannel.bind(new InetSocketAddress(PORT)); // привязываем канал к порту
        serverChannel.configureBlocking(false); // переводим канал в неблокирующий режим
        serverChannel.register(selector, SelectionKey.OP_ACCEPT); // регистрируем канал с интересом к принятию подключений

        System.out.println("Сервер запущен на порту " + PORT); // выводим информацию о запуске
        System.out.println("Файл коллекции: " + filename); // выводим имя файла коллекции
        System.out.println("Режим: неблокирующий (Selector), однопоточный"); // выводим информацию о режиме работы
        System.out.println("Серверные команды: save, info, exit\n"); // выводим список доступных команд сервера

        CommandProcessor processor = new CommandProcessor(collectionManager); // создаем процессор команд с менеджером коллекции

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
                    try { // начало блока перехвата исключений
                        RequestReader.readRequest(handler); // читаем запрос от клиента
                        if (handler.hasCompleteRequest() && !handler.isProcessing()) { // проверяем наличие полного запроса и отсутствие обработки
                            handler.setProcessing(true); // устанавливаем флаг обработки
                            key.interestOps(SelectionKey.OP_WRITE); // меняем интерес на запись (отправка ответа)
                        }
                    } catch (IOException e) { // обрабатываем ошибки чтения
                        System.err.println("Ошибка чтения от клиента: " + e.getMessage()); // выводим сообщение об ошибке
                        closeConnection(key, handler); // закрываем соединение с клиентом
                    }
                } else if (key.isWritable()) { // проверяем событие готовности к записи
                    ClientHandler handler = (ClientHandler) key.attachment(); // получаем обработчик клиента из ключа
                    
                    if (handler.hasCompleteRequest() && !handler.hasResponseToSend()) { // проверяем наличие запроса и отсутствие ответа
                        processor.processRequest(handler); // обрабатываем запрос и формируем ответ
                    }
                    
                    if (handler.hasResponseToSend()) { // проверяем, есть ли ответ для отправки
                        try { // начало блока перехвата исключений
                            boolean fullySent = ResponseSender.sendResponse(handler); // отправляем ответ клиенту
                            if (fullySent) { // если ответ полностью отправлен
                                handler.setProcessing(false); // сбрасываем флаг обработки
                                handler.clearCompleteRequest(); // очищаем запрос
                                key.interestOps(SelectionKey.OP_READ); // меняем интерес обратно на чтение
                            }
                        } catch (IOException e) { // обрабатываем ошибки отправки
                            System.err.println("Ошибка отправки ответа: " + e.getMessage()); // выводим сообщение об ошибке
                            closeConnection(key, handler); // закрываем соединение с клиентом
                        }
                    } else if (!handler.hasCompleteRequest()) { // если нет ни запроса, ни ответа
                        key.interestOps(SelectionKey.OP_READ); // возвращаем интерес к чтению
                    }
                }
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