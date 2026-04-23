package common;

/**
 * Перечисление статусов ответа сервера.
 * Определяет, успешно ли выполнена команда или произошла ошибка.
 *
 * @author Polina
 * @version 1.0
 * @see Response
 */
public enum ResponseStatus {
    SUCCESS, // команда выполнена успешно
    ERROR; // при выполнении команды произошла ошибка

    public boolean isSuccess() { // проверяет, является ли статус успешным
        return this == SUCCESS;
    }

    public boolean isError() { // проверяет, является ли статус ошибочным
        return this == ERROR;
    }
}
