package common;

import java.io.Serializable;

/**
 * Класс запроса от клиента к серверу.
 * Содержит команду, которую нужно выполнить на сервере.
 *
 * @author Polina
 * @version 1.0
 * @see Command
 * @see Response
 */
public class Request implements Serializable {
    private static final long serialVersionUID = 1L; // уникальный идентификатор версии класса для сериализации

    private final Command command; // команда, которую нужно выполнить на сервере

    public Request(Command command) { // конструктор запроса
        this.command = command;
    }

    public Command getCommand() { // возвращает команду, содержащуюся в запросе
        return command;
    }

    @Override
    public String toString() { // возвращает строковое представление запроса
        return "Request{" +
                "command=" + command +
                '}';
    }
}