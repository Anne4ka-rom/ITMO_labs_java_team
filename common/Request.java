package common;

import java.io.Serializable;

/**
 * Класс запроса от клиента к серверу.
 * Содержит команду для выполнения и опциональные данные авторизации.
 *
 * @author Polina
 * @version 2.0
 * @see Command
 * @see Response
 * @see AuthData
 */
public class Request implements Serializable {
    private static final long serialVersionUID = 2L; // уникальный идентификатор версии класса для сериализации

    private final Command command; // команда, которую нужно выполнить на сервере
    private final AuthData auth; // данные авторизации пользователя (может быть null для неавторизованных запросов)

    public Request(Command command) { // конструктор запроса без авторизации
        this(command, null);
    }

    public Request(Command command, AuthData auth) { // конструктор запроса с авторизацией
        this.command = command;
        this.auth = auth;
    }

    public Command getCommand() { // возвращает команду, содержащуюся в запросе
        return command;
    }

    public AuthData getAuth() { // возвращает данные авторизации пользователя
        return auth;
    }

    @Override
    public String toString() { // возвращает строковое представление запроса
        return "Request{" +
                "command=" + command +
                ", auth=" + auth +
                '}';
    }
}
