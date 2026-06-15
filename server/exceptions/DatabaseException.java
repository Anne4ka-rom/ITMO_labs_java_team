package server.exceptions; // класс находится в пакете exceptions серверной части

/**
 * Исключение, выбрасываемое при ошибках работы с базой данных
 * Обёртка для SQLException и других ошибок, связанных с БД
 * Позволяет унифицировать обработку ошибок базы данных на сервере
 * 
 * @author Anni
 * @version 1.0
 */
public class DatabaseException extends Exception { // объявляет класс исключения для ошибок бд, наследуется от Exception (проверяемое исключение)
    
    /**
     * Создаёт новое исключение с указанным сообщением
     * 
     * @param message текст сообщения об ошибке
     */
    public DatabaseException(String message) { // конструктор с сообщением
        super(message); // передаём сообщение родительскому классу Exception
    }

    /**
     * Создаёт новое исключение с указанным сообщением и причиной
     * 
     * @param message текст сообщения об ошибке
     * @param cause оригинальная причина исключения (обычно SQLException)
     */
    public DatabaseException(String message, Throwable cause) { // конструктор с сообщением и причиной
        super(message, cause); // передаём сообщение и причину родительскому классу Exception
    }
}