package server.database; // класс находится в пакете database серверной части

import java.io.Serializable; // импорт маркерного интерфейса для сериализации

/**
 * DTO для пользователя
 * Используется для передачи данных пользователя между слоями приложения
 * Реализует Serializable для возможности передачи по сети
 * 
 * @author Anni
 * @version 1.0
 */
public class User implements Serializable { // объявляет класс пользователя, реализующий сериализацию
    private static final long serialVersionUID = 1L; // уникальный идентификатор версии класса для сериализации

    private final String login; // логин пользователя (неизменяемое поле)
    private final String passwordHash; // хэш пароля пользователя (неизменяемое поле)

    /**
     * Создаёт нового пользователя с указанным логином и хэшем пароля
     * 
     * @param login логин пользователя
     * @param passwordHash хэш пароля пользователя (обычно md5)
     */
    public User(String login, String passwordHash) { // конструктор класса
        this.login = login; // сохраняем логин
        this.passwordHash = passwordHash; // сохраняем хэш пароля
    }

    /**
     * Возвращает логин пользователя
     * 
     * @return логин пользователя
     */
    public String getLogin() { // геттер для поля login
        return login; // возвращаем логин пользователя
    }

    /**
     * Возвращает хэш пароля пользователя
     * 
     * @return хэш пароля в строковом представлении
     */
    public String getPasswordHash() { // геттер для поля passwordHash
        return passwordHash; // возвращаем хэш пароля пользователя
    }

    /**
     * Возвращает строковое представление пользователя
     * Для безопасности пароль не выводится
     * 
     * @return строка с логином пользователя
     */
    @Override
    public String toString() { // переопределённый метод строкового представления
        return "User{login='" + login + "'}"; // возвращаем строку только с логином (без пароля для безопасности)
    }
}