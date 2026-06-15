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
 * @version 1.1
 */
public class Client {
    private static final String DEFAULT_HOST = "localhost"; // хост сервера по умолчанию
    private static final int DEFAULT_PORT = 1067; // порт сервера по умолчанию
    private static final int RECONNECT_DELAY = 5000; // задержка перед повторным подключением (5 секунд)

    private final String host; // хост сервера
    private final int port; // порт сервера
    private boolean isRunning; // флаг работы клиента
    private Scanner scanner; // сканер для чтения ввода пользователя
    private Socket socket; // сокет для соединения с сервером
    private DataOutputStream out; // поток для отправки данных с префиксом длины
    private DataInputStream in; // поток для приема данных с префиксом длины

    // вспомогательные компоненты — создаются один раз и переиспользуются при переподключении
    private final VehicleReader vehicleReader;
    private final ScriptExecutor scriptExecutor;

    public Client(String host, int port) { // конструктор клиента
        this.host = host;
        this.port = port;
        this.isRunning = true;
        this.scanner = new Scanner(System.in);

        // инициализируем компоненты (commandSender создаётся после connect, т.к. зависит от сокета)
        this.vehicleReader = new VehicleReader(scanner);
        this.scriptExecutor = new ScriptExecutor(vehicleReader);
    }

    // запуск клиента
    public void start() { // подключение → ввод команд → отправка → получение ответа
        System.out.println("Клиент запущен. Подключение к серверу " + host + ":" + port);

        while (isRunning) { // основной цикл
            try {
                connect(); // подключаемся к серверу
                System.out.println("Подключение установлено!");

                // создаём CommandSender и CommandBuilder после установки соединения
                CommandBuilder commandBuilder = new CommandBuilder(vehicleReader);
                CommandSender commandSender = new CommandSender(out, in, commandBuilder, scriptExecutor);
                scriptExecutor.setCommandSender(commandSender); // замыкаем зависимость

                while (isRunning) { // цикл ввода команд
                    System.out.print("> ");
                    String input = scanner.nextLine().trim();

                    if (input.isEmpty()) continue; // пропускаем пустые строки

                    if (input.equalsIgnoreCase("exit")) { // команда exit - завершаем клиент, обрабатывается локально
                        isRunning = false;
                        System.out.println("Клиент завершен");
                        break;
                    }

                    commandSender.sendCommand(input); // отправляем команду на сервер
                }

            } catch (IOException e) { // сервер недоступен или соединение разорвано
                System.out.println("Сервер недоступен. Повторное подключение через " +
                        RECONNECT_DELAY / 1000 + " секунд...");
                try {
                    Thread.sleep(RECONNECT_DELAY); // пауза перед повторной попыткой
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }
        close(); // закрываем ресурсы: сначала потоки, затем сокет, затем сканер
    }

    private void connect() throws IOException { // устанавливает соединение с сервером
        socket = new Socket(host, port); // создаем сокет
        out = new DataOutputStream(socket.getOutputStream()); // поток на отправку (с префиксом длины)
        in = new DataInputStream(socket.getInputStream()); // поток на чтение (с префиксом длины)
    }

    // закрывает все ресурсы клиента
    private void close() {
        try {
            if (in != null) in.close();
            if (out != null) out.close();
            if (socket != null) socket.close();
            if (scanner != null) scanner.close();
        } catch (IOException e) {
            System.err.println("Ошибка закрытия ресурсов: " + e.getMessage());
        }
    }

    // точка входа в клиентское приложение
    public static void main(String[] args) {
        String host = DEFAULT_HOST;
        int port = DEFAULT_PORT;

        if (args.length >= 1) host = args[0];
        if (args.length >= 2) {
            try {
                port = Integer.parseInt(args[1]);
            } catch (NumberFormatException e) {
                System.err.println("Порт должен быть числом. Используется порт по умолчанию");
            }
        }

        new Client(host, port).start();
    }
}