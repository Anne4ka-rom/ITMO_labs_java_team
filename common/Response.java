package common;

import java.io.Serializable;

/**
 * Класс ответа от сервера клиенту.
 * Содержит статус выполнения, сообщение и опционально данные.
 *
 * @author Polina
 * @version 1.0
 * @see ResponseStatus
 * @see Request
 */
public class Response implements Serializable {
    private static final long serialVersionUID = 1L; // уникальный идентификатор версии класса для сериализации

    private final ResponseStatus status; // статус выполнения команды (SUCCESS или ERROR)
    private final String message; // сообщение для пользователя (описание результата или ошибки)
    private final Object data; // дополнительные данные (например, список Vehicle для команды SHOW)

    public Response(ResponseStatus status, String message) { // конструктор ответа без дополнительных данных
        this(status, message, null); // вызываем конструктор с тремя параметрами
    }

    public Response(ResponseStatus status, String message, Object data) { // конструктор ответа с дополнительными данными
        this.status = status;  // сохраняем статус
        this.message = message;  // сохраняем сообщение
        this.data = data; // сохраняем дополнительные данные
    }

    public ResponseStatus getStatus() { // возвращает статус выполнения команды
        return status;
    }

    public String getMessage() { // возвращает сообщение для пользователя
        return message;
    }

    public Object getData() { // возвращает дополнительные данные
        return data;
    }
}