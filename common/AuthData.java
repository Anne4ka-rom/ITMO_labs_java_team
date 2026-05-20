package common;

import java.io.Serializable;

/**
 * Класс данных аутентификации пользователя.
 * Содержит имя пользователя и хеш пароля для проверки на сервере.
 * Пароль не хранится в открытом виде — только его хеш.
 *
 * @author Polina
 * @version 1.0
 */
public class AuthData implements Serializable {
    private static final long serialVersionUID = 1L; // уникальный идентификатор версии класса для сериализации

    private final String username; // имя пользователя
    private final String passwordHash; // хеш пароля (не сам пароль, для безопасности)

    public AuthData(String username, String passwordHash) { // конструктор данных аутентификации.
        this.username = username;
        this.passwordHash = passwordHash;
    }

    public String getUsername() { // возвращает имя пользователя
        return username;
    }

    public String getPasswordHash() { // возвращает хеш пароля для проверки на сервере
        return passwordHash;
    }

    @Override // возвращает строковое представление данных аутентификации
    public String toString() {
        return "AuthData{username='" + username + "'}";
    }
}