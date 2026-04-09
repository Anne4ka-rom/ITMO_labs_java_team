package common;

import java.io.Serializable;

// Ответ от сервера клиенту
public class Response implements Serializable {
    private static final long serialVersionUID = 1L;

    private final ResponseStatus status;
    private final String message;
    private final Object data;

    public Response(ResponseStatus status, String message) {
        this(status, message, null);
    }

    public Response(ResponseStatus status, String message, Object data) {
        this.status = status;
        this.message = message;
        this.data = data;
    }

    public ResponseStatus getStatus() {
        return status;
    }

    public String getMessage() {
        return message;
    }

    public Object getData() {
        return data;
    }
}