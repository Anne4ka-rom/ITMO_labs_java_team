package common;

import java.io.Serializable;

/**
 * Класс ответа от сервера клиенту.
 * Содержит статус выполнения, сообщение и опционально данные.
 *
 * @author Polina
 * @version 1.0
 */
public class Response implements Serializable {
    private static final long serialVersionUID = 1L; // идентификатор версии для совместимости сериализации

    private final ResponseStatus status; // статус выполнения команды (SUCCESS или ERROR)
    private final String message; // текстовое сообщение (результат или описание ошибки)
    private final Object data; // дополнительные данные (например, коллекция для команды SHOW)

    // конструктор для ответов без дополнительных данных
    public Response(ResponseStatus status, String message) {
        this(status, message, null); // вызываем основной конструктор с data = null
    }

    // основной конструктор для всех ответов
    public Response(ResponseStatus status, String message, Object data) {
        this.status = status; // сохраняем статус
        this.message = message; // сохраняем сообщение
        this.data = data; // сохраняем данные (могут быть null)
    }

    // возвращает статус выполнения команды
    public ResponseStatus getStatus() {
        return status;
    }

    // возвращает текстовое сообщение
    public String getMessage() {
        return message;
    }

    // возвращает дополнительные данные (клиент приводит к нужному типу)
    public Object getData() {
        return data;
    }
}    }

    public Object getData() { // возвращает дополнительные данные
        return data;
    }
}
