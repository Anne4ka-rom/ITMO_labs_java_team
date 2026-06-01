package client;

import java.io.*;
import java.net.*;
import java.util.Scanner;

/**
 * Главный класс клиентского приложения.
 * Обеспечивает взаимодействие с сервером: чтение команд пользователя,
 * валидацию ввода, сериализацию и отправку команд, обработку ответов.
 *
 * @author Polina
 * @version 2.0
 */
public class Client {
    private static final String DEFAULT_HOST = "localhost"; // хост сервера по умолчанию
    private static final int DEFAULT_PORT = 1067; // порт сервера по умолчанию
    private static final int RECONNECT_DELAY = 5000; // задержка перед повторным подключением (5 секунд)

    private final String host; // хост сервера (может быть передан через аргументы командной строки)
    private final int port; // порт сервера (может быть передан через аргументы командной строки)
    private boolean isRunning; // флаг работы клиента (true - работает, false - завершение)
    private Scanner scanner; // сканер для чтения ввода пользователя из консоли
    private Socket socket; // сокет для TCP-соединения с сервером
    private DataOutputStream out; // поток для отправки данных с префиксом длины
    private DataInputStream in; // поток для приема данных с префиксом длины
    private String currentLogin; // логин текущего авторизованного пользователя (null если не авторизован)
    private String currentPassword; // пароль текущего авторизованного пользователя

    private final VehicleReader vehicleReader; // компонент для чтения Vehicle из консоли
    private final ScriptExecutor scriptExecutor; // компонент для выполнения скриптов

    // конструктор: инициализация клиента с указанными хостом и портом
    public Client(String host, int port) {
        this.host = host;
        this.port = port;
        this.isRunning = true; // клиент запущен
        this.scanner = new Scanner(System.in); // создаем сканер для ввода
        this.vehicleReader = new VehicleReader(scanner); // создаем читатель Vehicle
        this.scriptExecutor = new ScriptExecutor(vehicleReader); // создаем исполнитель скриптов
        this.currentLogin = null; // изначально пользователь не авторизован
        this.currentPassword = null;
    }

    // запуск основного цикла работы клиента
    public void start() {
        // выводим приветственное сообщение и инструкцию
        System.out.println("Клиент запущен. Подключение к серверу " + host + ":" + port);
        System.out.println("Для начала работы выполните команды:");
        System.out.println("  register <login> <password> - регистрация нового пользователя");
        System.out.println("  login <login> <password> - авторизация");
        System.out.println();

        // основной цикл клиента (работает до вызова exit или ошибки)
        while (isRunning) {
            try {
                connect(); // устанавливаем соединение с сервером
                System.out.println("Подключение установлено!");

                // создаем построитель команд (передаем текущие логин/пароль)
                CommandBuilder commandBuilder = new CommandBuilder(vehicleReader, currentLogin, currentPassword);
                // создаем отправитель команд (передает команды на сервер)
                CommandSender commandSender = new CommandSender(out, in, commandBuilder, scriptExecutor);
                // передаем отправитель в исполнитель скриптов (для вложенных вызовов)
                scriptExecutor.setCommandSender(commandSender);

                // цикл чтения и обработки команд пользователя
                while (isRunning) {
                    System.out.print("> "); // приглашение к вводу
                    String input = scanner.nextLine().trim(); // читаем строку и обрезаем пробелы

                    if (input.isEmpty()) continue; // пустую строку игнорируем

                    // команда exit обрабатывается локально (завершает клиент)
                    if (input.equalsIgnoreCase("exit")) {
                        isRunning = false;
                        System.out.println("Клиент завершен");
                        break;
                    }

                    // отправляем команду на сервер
                    commandSender.sendCommand(input);
                }

            } catch (IOException e) {
                // соединение потеряно или сервер недоступен
                System.out.println("Сервер недоступен. Повторное подключение через " +
                        RECONNECT_DELAY / 1000 + " секунд...");
                try {
                    Thread.sleep(RECONNECT_DELAY); // ждем перед повторной попыткой
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt(); // восстанавливаем статус прерывания
                    break;
                }
            }
        }
        close(); // закрываем все ресурсы перед выходом
    }

    // устанавливает TCP-соединение с сервером и создает потоки ввода/вывода
    private void connect() throws IOException {
        socket = new Socket(host, port); // открываем сокет
        out = new DataOutputStream(socket.getOutputStream()); // поток на отправку (с префиксом длины)
        in = new DataInputStream(socket.getInputStream()); // поток на чтение (с префиксом длины)
    }

    // закрывает все ресурсы клиента в правильном порядке
    private void close() {
        try {
            if (in != null) in.close(); // закрываем входной поток
            if (out != null) out.close(); // закрываем выходной поток
            if (socket != null) socket.close(); // закрываем сокет
            if (scanner != null) scanner.close(); // закрываем сканер
        } catch (IOException e) {
            System.err.println("Ошибка закрытия ресурсов: " + e.getMessage());
        }
    }

    // обновляет учетные данные после успешной авторизации (вызывается из CommandSender)
    public void setCredentials(String login, String password) {
        this.currentLogin = login;
        this.currentPassword = password;
    }

    // точка входа в программу
    public static void main(String[] args) {
        String host = DEFAULT_HOST; // хост по умолчанию
        int port = DEFAULT_PORT; // порт по умолчанию

        // парсим аргументы командной строки
        if (args.length >= 1) host = args[0]; // первый аргумент - хост
        if (args.length >= 2) {
            try {
                port = Integer.parseInt(args[1]); // второй аргумент - порт
            } catch (NumberFormatException e) {
                System.err.println("Порт должен быть числом. Используется порт по умолчанию");
            }
        }

        // создаем и запускаем клиент
        new Client(host, port).start();
    }
}
