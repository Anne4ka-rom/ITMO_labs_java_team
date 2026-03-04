package exceptions;

// Проверяемое исключение для неверных данных в файле
public class InvalidDataException extends Exception {
    public InvalidDataException(String message) {
        super(message); // вызов конструктора родительского класса Exception с сообщением
    }

    public InvalidDataException(String message, Throwable cause) {
        super(message, cause); // вызов конструктора Exception с сообщением и причиной
    }
}