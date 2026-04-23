package common;

import java.io.Serializable;

/**
 * Класс команды для передачи от клиента к серверу.
 * Содержит тип команды и аргументы.
 * Реализует Serializable, чтобы объект можно было передать по сети.
 *
 * @author Polina
 * @version 1.0
 * @see CommandType
 * @see Request
 */
public class Command implements Serializable {
    private static final long serialVersionUID = 1L; // уникальный идентификатор версии класса для сериализации

    private final CommandType type; // тип команды (определяет, что нужно сделать)
    private final Object argument; // аргумент команды

    public Command(CommandType type) { // конструктор для команд, которые не требуют аргументов
        this(type, null); // вызываем конструктор с двумя параметрами, передавая null как аргумент
    }

    public Command(CommandType type, Object argument) { // конструктор для команд, которые требуют аргумент
        this.type = type; // сохраняем тип команды
        this.argument = argument;  // сохраняем аргумент
    }

    public CommandType getType() { // возвращает тип команды
        return type;
    }

    public Object getArgument() { // возвращает аргумент команды
        return argument;
    }

    @Override
    public String toString() { // возвращает строковое представление команды
        return "Command{" +
                "type=" + type +
                ", argument=" + argument +
                '}';
    }
}