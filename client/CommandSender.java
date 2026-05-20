package client;

import common.*;

import java.io.*;
import java.net.*;

/**
 * Отвечает за сериализацию, отправку команд на сервер и обработку ответов.
 * Изолирует транспортный слой (prefixed-length протокол) от остальной логики.
 *
 * @author Polina
 * @version 1.1
 */
public class CommandSender {

    private final DataOutputStream out; // поток для отправки данных с префиксом длины
    private final DataInputStream in;   // поток для приема данных с префиксом длины
    private final CommandBuilder commandBuilder; // используется для создания объектов Command из строки
    private final ScriptExecutor scriptExecutor; // используется для выполнения execute_script локально

    public CommandSender(DataOutputStream out, DataInputStream in,
                         CommandBuilder commandBuilder, ScriptExecutor scriptExecutor) {
        this.out = out;
        this.in = in;
        this.commandBuilder = commandBuilder;
        this.scriptExecutor = scriptExecutor;
    }

    // отправляет команду на сервер и выводит ответ
    public void sendCommand(String input) throws IOException {
        // разбираем введенную строку
        String[] parts = input.split("\\s+", 2);
        String commandName = parts[0].toLowerCase();
        String argument = parts.length > 1 ? parts[1] : null;

        Command command = commandBuilder.createCommand(commandName, argument); // создаем объект Command

        // если команда не распознана - выходим
        if (command == null) {
            System.out.println("Неизвестная команда. Введите 'help' для справки.");
            return;
        }

        // execute_script выполняется локально, не отправляется на сервер
        if (command.getType() == CommandType.EXECUTE_SCRIPT) {
            scriptExecutor.executeScript((String) command.getArgument());
            return;
        }

        // отправка с префиксом длины (чтобы сервер знал, сколько байт читать)
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(bos);
        oos.writeObject(new Request(command));
        oos.flush();
        byte[] data = bos.toByteArray();

        out.writeInt(data.length); // сначала отправляем длину
        out.write(data); // потом сами данные
        out.flush();

        // чтение ответа с префиксом длины
        int responseLength = in.readInt(); // читаем длину ответа
        byte[] responseData = new byte[responseLength];
        in.readFully(responseData); // читаем данные

        try (ByteArrayInputStream bis = new ByteArrayInputStream(responseData);
             ObjectInputStream ois = new ObjectInputStream(bis)) {
            Response response = (Response) ois.readObject(); // десериализуем ответ

            // вывод результата
            if (response.getStatus() == ResponseStatus.SUCCESS) {
                // данные из response.getData() не выводим, чтобы избежать дублирования
                System.out.println(response.getMessage());
            } else {
                System.err.println("Ошибка: " + response.getMessage());
            }
        } catch (ClassNotFoundException e) {
            System.err.println("Ошибка десериализации ответа");
        }
    }
}