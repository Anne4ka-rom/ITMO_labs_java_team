package common;

import java.io.Serializable;

/**
 * Класс команды для передачи от клиента к серверу.
 * Содержит тип команды, аргументы, логин и пароль пользователя.
 * Реализует Serializable, чтобы объект можно было передать по сети.
 *
 * @author Polina
 * @version 2.0
 */
public class Command implements Serializable {
    private static final long serialVersionUID = 2L; // идентификатор версии для совместимости сериализации

    private final CommandType type; // тип команды (ADD, REMOVE_BY_ID и т.д.)
    private final Object argument; // аргумент команды (может быть Integer, String, Vehicle, массив и т.д.)
    private final String login; // логин пользователя, отправившего команду
    private final String password; // пароль пользователя

    // конструктор для команд без аргументов (только логин и пароль)
    public Command(CommandType type, String login, String password) {
        this(type, null, login, password); // вызываем основной конструктор с argument = null
    }

    // основной конструктор для всех команд
    public Command(CommandType type, Object argument, String login, String password) {
        this.type = type; // сохраняем тип команды
        this.argument = argument; // сохраняем аргумент (может быть null)
        this.login = login; // сохраняем логин пользователя
        this.password = password; // сохраняем пароль пользователя
    }

    // возвращает тип команды (нужен серверу, чтобы понять, что делать)
    public CommandType getType() {
        return type;
    }

    // возвращает аргумент команды (сервер приводит его к нужному типу в зависимости от type)
    public Object getArgument() {
        return argument;
    }

    // возвращает логин пользователя (для авторизации и проверки прав)
    public String getLogin() {
        return login;
    }

    // возвращает пароль пользователя (для авторизации)
    public String getPassword() {
        return password;
    }

    // строковое представление команды (пароль не выводим в целях безопасности)
    @Override
    public String toString() {
        return "Command{" +
                "type=" + type +
                ", argument=" + argument +
                ", login='" + login + '\'' +
                '}'; // пароль не включаем в toString
    }
}
