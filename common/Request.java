package common;

import java.io.Serializable;

/**
 * Класс запроса от клиента к серверу.
 * Содержит команду, которую нужно выполнить на сервере.
 *
 * @author Polina
 * @version 1.0
 */
public class Request implements Serializable {
    private static final long serialVersionUID = 1L; // идентификатор версии для совместимости сериализации

    private final Command command; // команда, которую клиент просит выполнить на сервере

    // конструктор: создает запрос с указанной командой
    public Request(Command command) {
        this.command = command; // сохраняем команду
    }

    // возвращает команду, содержащуюся в запросе
    public Command getCommand() {
        return command;
    }

    // строковое представление запроса (для отладки и логирования)
    @Override
    public String toString() {
        return "Request{" +
                "command=" + command +
                '}';
    }
}
    @Override
    public String toString() { // возвращает строковое представление запроса
        return "Request{" +
                "command=" + command +
                ", auth=" + auth +
                '}';
    }
}
