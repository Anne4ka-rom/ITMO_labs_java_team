package client;

import common.*;
import java.io.*;

/**
 * Класс для отправки команд на сервер
 *
 * @author Polina
 * @version 1.0
 */
public class CommandSender {
    private final ObjectOutputStream out;

    public CommandSender(ObjectOutputStream out) {
        this.out = out;
    }

    public void sendCommand(Command command) throws IOException {
        Request request = new Request(command);
        out.writeObject(request);
        out.flush();
    }
}