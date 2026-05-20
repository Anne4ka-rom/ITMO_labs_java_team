package server.auth; // класс находится в пакете auth серверной части

import common.AuthData; // импорт класса с данными аутентификации из общей модели
import server.database.UserRepository; // импорт репозитория пользователей для работы с бд
import java.sql.SQLException; // импорт исключения для ошибок sql

/**
 * Управляет аутентификацией и регистрацией пользователей
 * Проверяет логин и пароль, хэширует пароли, взаимодействует с репозиторием пользователей
 * 
 * @author Anni
 * @version 1.0
 */
public class AuthManager { // объявляет класс менеджера аутентификации
    private final UserRepository userRepository; // репозиторий пользователей для доступа к бд

    /**
     * Создаёт новый менеджер аутентификации с указанным репозиторием пользователей
     * 
     * @param userRepository репозиторий для работы с пользователями в базе данных
     */
    public AuthManager(UserRepository userRepository) { // конструктор класса
        this.userRepository = userRepository; // сохраняем ссылку на репозиторий пользователей
    }

    /**
     * Проверяет подлинность пользователя по логину и хэшу пароля
     * Возвращает true если пользователь существует и пароль совпадает
     * 
     * @param auth объект с данными аутентификации (логин и хэш пароля)
     * @return true если аутентификация успешна, false в противном случае
     * @throws SQLException если ошибка при работе с базой данных
     */
    public boolean authenticate(AuthData auth) throws SQLException { // метод аутентификации пользователя
        if (auth == null || auth.getUsername() == null || auth.getPasswordHash() == null) { // проверяем, что данные аутентификации не null и содержат логин с хэшем
            return false; // возвращаем false, если данные невалидны
        }
        return userRepository.authenticate(auth.getUsername(), auth.getPasswordHash()); // проверяем в репозитории существование пользователя с таким логином и хэшем пароля
    }

    /**
     * Регистрирует нового пользователя в системе
     * Хэширует пароль перед сохранением в базу данных
     * 
     * @param username логин нового пользователя
     * @param password пароль нового пользователя (в открытом виде)
     * @return true если регистрация успешна, false если пользователь уже существует
     * @throws SQLException если ошибка при работе с базой данных
     */
    public boolean register(String username, String password) throws SQLException { // метод регистрации нового пользователя
        String hash = UserRepository.hashPassword(password); // вычисляем хэш пароля с помощью статического метода репозитория
        return userRepository.register(username, hash); // передаём репозиторию логин и хэш пароля для сохранения в бд
    }
}