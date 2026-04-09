package common;

// Перечисление всех поддерживаемых команд

public enum CommandType {
    HELP("help", "вывести справку по доступным командам"),
    INFO("info", "вывести информацию о коллекции"),
    SHOW("show", "вывести все элементы коллекции"),
    ADD("add", "добавить новый элемент"),
    UPDATE("update", "обновить элемент по ID"),
    REMOVE_BY_ID("remove_by_id", "удалить элемент по ID"),
    CLEAR("clear", "очистить коллекцию"),
    REMOVE_LAST("remove_last", "удалить последний элемент"),
    REMOVE_LOWER("remove_lower", "удалить элементы, меньшие заданного"),
    SORT("sort", "отсортировать коллекцию"),
    SUM_OF_CAPACITY("sum_of_capacity", "вывести сумму грузоподъемностей"),
    FILTER_BY_CAPACITY("filter_by_capacity", "вывести элементы с заданной грузоподъемностью"),
    FILTER_LESS_THAN_TYPE("filter_less_than_type", "вывести элементы, тип которых меньше заданного"),
    EXIT("exit", "завершить работу клиента"),
    SAVE("save", "сохранить коллекцию (только сервер)");

    private final String name;
    private final String description;

    CommandType(String name, String description) {
        this.name = name;
        this.description = description;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public static CommandType fromString(String str) {
        for (CommandType type : values()) {
            if (type.name.equalsIgnoreCase(str)) {
                return type;
            }
        }
        return null;
    }
}