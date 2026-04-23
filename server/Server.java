package server; // объявление пакета server, куда входят все классы серверной части

import server.collection.CollectionManager; // импорт менеджера коллекции для управления данными
import server.file.FileManager; // импорт менеджера файлов для работы с xml

import java.io.IOException; // импорт исключения для ошибок ввода-вывода
import java.net.InetSocketAddress; // импорт класса для адреса сокета с портом
import java.net.SocketAddress; // импорт абстрактного адреса сокета
import java.nio.channels.ServerSocketChannel; // импорт неблокирующего канала серверного сокета
import java.nio.channels.SocketChannel; // импорт неблокирующего канала клиентского сокета

/**
 * Главный класс серверного приложения, реализующего неблокирующий прием подключений
 * 
 * Сервер работает на фиксированном порту (1067) и использует неблокирующий режим дляя принятия входящих соединений
 * Каждый клиент обрабатывается в отдельном потоке
 * 
 * @author Anni
 * @version 1.0
 * @see ServerSocketChannel
 * @see ConnectionHandler
 */
public class Server { // объявление класса с именем Server
    private static final int PORT = 1067;  // статическая константа номера порта для сервера
    private static final int ACCEPT_TIMEOUT_MS = 100; // таймаут ожидания подключения (в миллисекундах)
    
    private ServerSocketChannel serverSocketChannel; // канал серверного сокета в неблокирующем режиме
    private CollectionManager collectionManager; // менеджер для управления коллекцией
    private CommandProcessor commandProcessor; // процессор для обработки команд клиентов
    private volatile boolean running; // флаг работы сервера (volatile для потокобезопасности)
    
    /**
     * Точка входа в серверное приложение
     * 
     * Ожидает один аргумент командной строки - путь к XML файлу с данными коллекции
     * При отсутствии аргумента выводит сообщение об ошибке и завершает работу
     * 
     * @param args аргументы командной строки, где args[0] - путь к файлу с данными
     * @throws IllegalArgumentException если аргументы не переданы
     */
    public static void main(String[] args) { // точка входа в программу (с этого метода начинается выполнение любого java-приложения)
        if (args.length == 0) { // проверка, передан ли аргумент с именем файла
            System.err.println("Ошибка: не указано имя файла с данными"); // вывод ошибки в stderr
            System.err.println("Использование: java server.Server <filename>"); // подсказка по использованию
            System.exit(1); // аварийное завершение программы с кодом 1
        }
        
        Server server = new Server(); // создание экземпляра сервера
        server.start(args[0]); // запуск сервера с переданным именем файла
    }
    
    /**
     * Запускает сервер с указанным файлом данных
     * 
     * Метод выполняет инициализацию всех компонентов сервера:
     * - Создает {@link FileManager} для работы с XML файлом
     * - Создает {@link CollectionManager} для управления коллекцией
     * - Создает {@link CommandProcessor} для обработки команд
     * - Настраивает {@link ServerSocketChannel} в неблокирующем режиме
     * - Запускает основной цикл приема подключений
     * 
     * Основной цикл работает до тех пор, пока флаг {@code running} истинен
     * При каждом проходе цикла выполняется неблокирующий вызов {@code accept()}
     * При появлении нового клиента создается отдельный поток с {@link ConnectionHandler}
     * 
     * @param filename путь к XML файлу, содержащему начальные данные коллекции
     * @throws IOException если произошла ошибка при открытии или привязке сокета
     * @see #stop()
     */
    public void start(String filename) { // запускает сервер с указанным файлом данных
        try { // начало блока перехвата исключений
            // инициализация менеджеров
            FileManager fileManager = new FileManager(filename); // создание менеджера файлов с путём к xml
            collectionManager = new CollectionManager(fileManager); // создание менеджера коллекции с файловым менеджером
            commandProcessor = new CommandProcessor(collectionManager); // создание процессора команд с менеджером коллекции
            
            // настройка серверного канала в неблокирующем режиме
            serverSocketChannel = ServerSocketChannel.open(); // открытие канала серверного сокета
            serverSocketChannel.bind(new InetSocketAddress(PORT)); // привязка к локальному адресу с указанным портом
            serverSocketChannel.configureBlocking(false); // установка неблокирующего режима
            
            running = true; // установка флага работы сервера
            System.out.println("Сервер успешно запущен на порту " + PORT); // вывод сообщения о запуске
            System.out.println("Режим: неблокирующий"); // информирование о режиме работы
            System.out.println("Файл данных: " + filename); // вывод пути к файлу данных
            System.out.println("Ожидание подключений..."); // ожидание клиентов
            
            // основной цикл сервера
            while (running) { // цикл продолжается, пока флаг running истинен
                SocketChannel clientChannel = serverSocketChannel.accept(); // попытка принять подключение (возвращает null если нет подключений)
                
                if (clientChannel != null) { // если есть новый клиент
                    // новое подключение
                    SocketAddress clientAddress = clientChannel.getRemoteAddress(); // получение адреса удалённого клиента
                    System.out.println("Новое подключение от: " + clientAddress); // вывод информации о подключении
                    
                    // обработка клиента в отдельном потоке
                    ConnectionHandler handler = new ConnectionHandler( // создание обработчика подключения
                        clientChannel.socket(), // преобразование SocketChannel в обычный Socket
                        commandProcessor // передача процессора команд
                    );
                    new Thread(handler).start(); // запуск обработчика в новом потоке
                }
                
                // небольшая задержка для снижения нагрузки на cpu
                try { // начало блока перехвата исключений
                    Thread.sleep(ACCEPT_TIMEOUT_MS); // приостановка цикла на заданное время
                } catch (InterruptedException e) { // обработка прерывания потока
                    Thread.currentThread().interrupt(); // восстановление статуса прерывания
                    break; // выход из цикла
                }
            }
            
        } catch (IOException e) { // обработка ошибок ввода-вывода
            System.err.println("Критическая ошибка сервера: " + e.getMessage()); // вывод сообщения об ошибке
            e.printStackTrace(); // печать стека вызовов для отладки
        } finally { // блок кода, который выполнится обязательно (независимо от того, произошла ошибка в блоке try или нет)
            stop(); // гарантированная остановка сервера при любом исходе
        }
    }
    
    /**
     * Останавливает работу сервера
     * 
     * Устанавливает флаг {@code running} в false, что приводит к завершению
     * основного цикла в методе {@link #start(String)}. Закрывает канал
     * серверного сокета, освобождая занятый порт
     * 
     * Метод безопасно обрабатывает возможные исключения при закрытии канала
     */
    public void stop() { // объявление метода stop
        running = false; // сброс флага работы для выхода из основного цикла
        try { // начало блока перехвата исключений
            if (serverSocketChannel != null && serverSocketChannel.isOpen()) { // проверка существования и открытости канала
                serverSocketChannel.close(); // закрытие канала серверного сокета
                System.out.println("Сервер остановлен"); // подтверждение остановки
            }
        } catch (IOException e) { // обработка ошибки при закрытии
            System.err.println("Ошибка при остановке сервера: " + e.getMessage()); // вывод сообщения об ошибке
        }
    }
}