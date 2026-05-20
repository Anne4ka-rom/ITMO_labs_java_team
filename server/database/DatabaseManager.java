package server.database; // класс находится в пакете database серверной части

import java.sql.*; // импорт всех классов для работы с jdbc и sql

/**
 * Управляет подключением к базе данных PostgreSQL
 * Отвечает за подключение, закрытие соединения и инициализацию схемы
 * 
 * @author Anni
 * @version 1.0
 */
public class DatabaseManager { // объявляет класс менеджера базы данных
    private static final String URL = "jdbc:postgresql://pg/studs"; // url для подключения к учебной базе данных postgresql
    private static final String DRIVER = "org.postgresql.Driver"; // имя класса драйвера postgresql

    private final String username; // имя пользователя для подключения к бд
    private final String password; // пароль для подключения к бд
    private Connection connection; // объект соединения с базой данных

    /**
     * Создаёт новый менеджер базы данных с указанными учётными данными
     * 
     * @param username имя пользователя для подключения к бд
     * @param password пароль для подключения к бд
     */
    public DatabaseManager(String username, String password) { // конструктор класса
        this.username = username; // сохраняем имя пользователя
        this.password = password; // сохраняем пароль
    }

    /**
     * Устанавливает соединение с базой данных
     * Загружает драйвер, создаёт подключение и отключает автокоммит
     * 
     * @throws SQLException если ошибка при подключении к бд
     * @throws ClassNotFoundException если драйвер postgresql не найден в classpath
     */
    public void connect() throws SQLException, ClassNotFoundException { // метод подключения к базе данных
        Class.forName(DRIVER); // загружаем класс драйвера postgresql (регистрирует себя автоматически)
        connection = DriverManager.getConnection(URL, username, password); // создаём соединение с бд по url, логину и паролю
        connection.setAutoCommit(false); // отключаем автоматический коммит (управляем транзакциями вручную)
        System.out.println("Подключение к БД установлено"); // выводим сообщение об успешном подключении
    }

    /**
     * Возвращает текущее соединение с базой данных
     * 
     * @return объект Connection для работы с бд
     */
    public Connection getConnection() { // геттер для соединения с бд
        return connection; // возвращаем объект соединения
    }

    /**
     * Закрывает соединение с базой данных
     * Вызывается при завершении работы сервера
     */
    public void close() { // метод закрытия соединения
        if (connection != null) { // проверяем, существует ли соединение
            try { // начало блока перехвата исключений
                connection.close(); // закрываем соединение с бд
                System.out.println("Соединение с БД закрыто"); // выводим сообщение об успешном закрытии
            } catch (SQLException e) { // обрабатываем ошибку при закрытии
                System.err.println("Ошибка закрытия соединения: " + e.getMessage()); // выводим сообщение об ошибке
            }
        }
    }

    /**
     * Инициализирует схему базы данных
     * Создаёт таблицы users и vehicles, а также последовательность для id
     * Если таблицы уже существуют - не пересоздаёт их
     * 
     * @throws SQLException если ошибка при выполнении sql запросов
     */
    public void initSchema() throws SQLException { // метод инициализации схемы бд
        String createUsersTable = """ // sql запрос для создания таблицы пользователей
            CREATE TABLE IF NOT EXISTS users ( // создаём таблицу users, если её нет
                username VARCHAR(50) PRIMARY KEY, // логин пользователя - первичный ключ
                password_hash VARCHAR(32) NOT NULL // хэш пароля (32 символа для md5)
            )
        """;

        String createVehiclesTable = """ // sql запрос для создания таблицы транспортных средств
            CREATE TABLE IF NOT EXISTS vehicles ( // создаём таблицу vehicles, если её нет
                id SERIAL PRIMARY KEY, // уникальный идентификатор с автоинкрементом
                name VARCHAR(255) NOT NULL, // название транспортного средства
                coord_x DOUBLE PRECISION NOT NULL, // координата x (дробное число)
                coord_y INTEGER NOT NULL, // координата y (целое число)
                creation_date DATE NOT NULL, // дата создания
                engine_power DOUBLE PRECISION NOT NULL, // мощность двигателя
                capacity DOUBLE PRECISION NOT NULL, // вместимость
                type VARCHAR(50) NOT NULL, // тип транспортного средства
                fuel_type VARCHAR(50), // тип топлива (может быть null)
                owner_username VARCHAR(50) NOT NULL REFERENCES users(username) ON DELETE CASCADE // владелец - внешний ключ на users
            )
        """;

        String createSequence = """ // sql запрос для создания последовательности id
            CREATE SEQUENCE IF NOT EXISTS vehicles_id_seq START 1 // создаём последовательность для id, если её нет
        """;

        try (Statement stmt = connection.createStatement()) { // создаём statement и автоматически закрываем его в конце
            stmt.execute(createUsersTable); // выполняем создание таблицы users
            stmt.execute(createVehiclesTable); // выполняем создание таблицы vehicles
            stmt.execute(createSequence); // выполняем создание последовательности
            connection.commit(); // фиксируем все изменения (автокоммит отключён)
            System.out.println("Схема БД инициализирована"); // выводим сообщение об успешной инициализации
        } catch (SQLException e) { // обрабатываем ошибку выполнения sql
            connection.rollback(); // откатываем все изменения, сделанные в этой транзакции
            throw e; // пробрасываем исключение дальше
        }
    }
}