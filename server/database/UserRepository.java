package server.database; // класс находится в пакете database серверной части

import java.security.MessageDigest; // импорт класса для вычисления криптографических хэшей
import java.security.NoSuchAlgorithmException; // импорт исключения при отсутствии алгоритма хэширования
import java.sql.*; // импорт всех классов для работы с jdbc и sql

/**
 * Управляет операциями с пользователями в базе данных
 * Отвечает за регистрацию, аутентификацию, проверку существования и хэширование паролей
 * 
 * @author Anni
 * @version 1.0
 */
public class UserRepository { // объявляет класс репозитория пользователей
    private final DatabaseManager dbManager; // менеджер базы данных для получения соединения

    /**
     * Создаёт новый репозиторий пользователей с указанным менеджером базы данных
     * 
     * @param dbManager менеджер для управления подключением к бд
     */
    public UserRepository(DatabaseManager dbManager) { // конструктор класса
        this.dbManager = dbManager; // сохраняем ссылку на менеджер базы данных
    }

    /**
     * Вычисляет MD5 хэш переданного пароля
     * Используется для безопасного хранения паролей в базе данных
     * 
     * @param password пароль в открытом виде
     * @return строка с MD5 хэшем в шестнадцатеричном формате
     * @throws RuntimeException если алгоритм MD5 не поддерживается (не должно происходить)
     */
    public static String hashPassword(String password) { // статический метод хэширования пароля
        try { // начало блока перехвата исключений
            MessageDigest md = MessageDigest.getInstance("MD5"); // получаем экземпляр алгоритма md5
            byte[] digest = md.digest(password.getBytes()); // вычисляем хэш от массива байтов пароля
            StringBuilder sb = new StringBuilder(); // создаём строитель строки для формирования hex-строки
            for (byte b : digest) { // проходим по каждому байту хэша
                sb.append(String.format("%02x", b)); // добавляем двухсимвольное hex-представление байта
            }
            return sb.toString(); // возвращаем полученную hex-строку (32 символа)
        } catch (NoSuchAlgorithmException e) { // обрабатываем ошибку отсутствия алгоритма md5
            throw new RuntimeException("MD5 не поддерживается", e); // выбрасываем runtime исключение с причиной
        }
    }

    /**
     * Регистрирует нового пользователя в базе данных
     * Сохраняет логин и хэш пароля в таблицу users
     * 
     * @param username логин нового пользователя
     * @param passwordHash хэш пароля (уже вычисленный)
     * @return true если регистрация успешна, false если пользователь с таким логином уже существует
     * @throws SQLException если ошибка при выполнении sql запроса (кроме дубликата ключа)
     */
    public boolean register(String username, String passwordHash) throws SQLException { // метод регистрации пользователя
        String sql = "INSERT INTO users (username, password_hash) VALUES (?, ?)"; // sql запрос для вставки нового пользователя
        try (PreparedStatement stmt = dbManager.getConnection().prepareStatement(sql)) { // создаём подготовленный запрос и автоматически закрываем его в конце
            stmt.setString(1, username); // подставляем логин в первый параметр
            stmt.setString(2, passwordHash); // подставляем хэш пароля во второй параметр
            int affected = stmt.executeUpdate(); // выполняем вставку и получаем количество затронутых строк
            dbManager.getConnection().commit(); // фиксируем транзакцию
            return affected > 0; // возвращаем true, если строка была добавлена (affected > 0)
        } catch (SQLException e) { // обрабатываем ошибку выполнения sql
            dbManager.getConnection().rollback(); // откатываем транзакцию при ошибке
            if (e.getSQLState().equals("23505")) { // проверяем код ошибки postgresql - нарушение уникальности (duplicate key)
                return false; // возвращаем false - пользователь уже существует
            }
            throw e; // пробрасываем остальные sql исключения дальше
        }
    }

    /**
     * Проверяет подлинность пользователя по логину и хэшу пароля
     * Сравнивает переданный хэш с сохранённым в базе данных
     * 
     * @param username логин пользователя
     * @param passwordHash хэш пароля для проверки
     * @return true если пользователь существует и хэши совпадают, false в противном случае
     * @throws SQLException если ошибка при выполнении sql запроса
     */
    public boolean authenticate(String username, String passwordHash) throws SQLException { // метод аутентификации пользователя
        String sql = "SELECT password_hash FROM users WHERE username = ?"; // sql запрос для получения хэша пароля по логину
        try (PreparedStatement stmt = dbManager.getConnection().prepareStatement(sql)) { // создаём подготовленный запрос и автоматически закрываем его в конце
            stmt.setString(1, username); // подставляем логин в первый параметр
            ResultSet rs = stmt.executeQuery(); // выполняем запрос и получаем результат
            if (rs.next()) { // проверяем, найден ли пользователь (есть ли хотя бы одна строка)
                String storedHash = rs.getString("password_hash"); // получаем сохранённый хэш пароля из результата
                return storedHash.equals(passwordHash); // сравниваем сохранённый хэш с переданным и возвращаем результат
            }
            return false; // пользователь не найден - возвращаем false
        }
    }

    /**
     * Проверяет, существует ли пользователь с указанным логином
     * 
     * @param username логин для проверки
     * @return true если пользователь существует, false в противном случае
     * @throws SQLException если ошибка при выполнении sql запроса
     */
    public boolean userExists(String username) throws SQLException { // метод проверки существования пользователя
        String sql = "SELECT 1 FROM users WHERE username = ?"; // sql запрос для проверки существования (возвращает 1 если есть)
        try (PreparedStatement stmt = dbManager.getConnection().prepareStatement(sql)) { // создаём подготовленный запрос и автоматически закрываем его в конце
            stmt.setString(1, username); // подставляем логин в первый параметр
            ResultSet rs = stmt.executeQuery(); // выполняем запрос и получаем результат
            return rs.next(); // возвращаем true, если результат содержит хотя бы одну строку
        }
    }
}