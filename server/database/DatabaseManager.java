package server.database; // класс находится в пакете database серверной части

import common.model.*; // импорт всех классов модели из общей части

import java.security.MessageDigest; // импорт класса для вычисления криптографических хэшей
import java.security.NoSuchAlgorithmException; // импорт исключения при отсутствии алгоритма хэширования
import java.sql.*; // импорт всех классов для работы с jdbc и sql
import java.time.LocalDate; // импорт класса для работы с датами
import java.util.Stack; // импорт класса стека для хранения коллекции

/**
 * Управляет подключением к PostgreSQL и выполнением операций с БД
 * Отвечает за подключение, регистрацию пользователей, аутентификацию,
 * а также за операции CRUD с транспортными средствами
 * 
 * @author Anni
 * @version 1.0
 */
public class DatabaseManager { // объявляет класс менеджера базы данных
    private static final String URL = "jdbc:postgresql://pg/studs"; // url для подключения к учебной базе данных postgresql
    private static final String USER = "s504713"; // имя пользователя для подключения к бд
    private static final String PASSWORD = "JeblGfTQi5QYrH65"; // пароль для подключения к бд
    private static final String SCHEMA = "s504713"; // имя схемы в базе данных

    private Connection connection; // объект соединения с базой данных

    /**
     * Устанавливает соединение с базой данных
     * Загружает драйвер, создаёт подключение и устанавливает схему по умолчанию
     * 
     * @return true если подключение успешно, false в противном случае
     */
    public boolean connect() { // метод подключения к базе данных
        try { // начало блока перехвата исключений
            Class.forName("org.postgresql.Driver"); // загружаем класс драйвера postgresql
            connection = DriverManager.getConnection(URL, USER, PASSWORD); // создаём соединение с бд по url, логину и паролю
            System.out.println("Подключение к БД установлено"); // выводим сообщение об успешном подключении
            
            try (Statement stmt = connection.createStatement()) { // создаём statement и автоматически закрываем его в конце
                stmt.execute("SET search_path TO " + SCHEMA + ", public"); // устанавливаем схему по умолчанию для поиска таблиц
            } // конец блока try-with-resources
            
            return true; // возвращаем успех
        } catch (ClassNotFoundException e) { // обрабатываем ошибку - драйвер не найден
            System.err.println("Драйвер PostgreSQL не найден: " + e.getMessage()); // выводим сообщение об ошибке
            return false; // возвращаем неудачу
        } catch (SQLException e) { // обрабатываем ошибку подключения
            System.err.println("Ошибка подключения к БД: " + e.getMessage()); // выводим сообщение об ошибке
            return false; // возвращаем неудачу
        }
    }

    /**
     * Закрывает соединение с базой данных
     * Вызывается при завершении работы сервера
     */
    public void disconnect() { // метод закрытия соединения
        try { // начало блока перехвата исключений
            if (connection != null && !connection.isClosed()) { // проверяем, что соединение существует и не закрыто
                connection.close(); // закрываем соединение с бд
            }
        } catch (SQLException e) { // обрабатываем ошибку при закрытии
            System.err.println("Ошибка при закрытии соединения: " + e.getMessage()); // выводим сообщение об ошибке
        }
    }

    /**
     * Вычисляет MD5 хэш переданного пароля
     * Используется для безопасного хранения паролей в базе данных
     * 
     * @param password пароль в открытом виде
     * @return строка с MD5 хэшем в шестнадцатеричном формате
     * @throws RuntimeException если алгоритм MD5 не поддерживается
     */
    private String hashPassword(String password) { // приватный метод хэширования пароля
        try { // начало блока перехвата исключений
            MessageDigest md = MessageDigest.getInstance("MD5"); // получаем экземпляр алгоритма md5
            byte[] hash = md.digest(password.getBytes()); // вычисляем хэш от массива байтов пароля
            StringBuilder sb = new StringBuilder(); // создаём строитель строки для формирования hex-строки
            for (byte b : hash) { // проходим по каждому байту хэша
                sb.append(String.format("%02x", b)); // добавляем двухсимвольное hex-представление байта
            }
            return sb.toString(); // возвращаем полученную hex-строку (32 символа)
        } catch (NoSuchAlgorithmException e) { // обрабатываем ошибку отсутствия алгоритма md5
            throw new RuntimeException("MD5 алгоритм не найден", e); // выбрасываем runtime исключение с причиной
        }
    }

    /**
     * Регистрирует нового пользователя в базе данных
     * Проверяет, не существует ли пользователь с таким логином
     * 
     * @param login логин нового пользователя
     * @param password пароль нового пользователя (в открытом виде)
     * @return true если регистрация успешна, false если пользователь уже существует или ошибка
     */
    public boolean registerUser(String login, String password) { // метод регистрации пользователя
        String checkSql = "SELECT login FROM " + SCHEMA + ".users WHERE login = ?"; // sql запрос для проверки существования пользователя
        try (PreparedStatement stmt = connection.prepareStatement(checkSql)) { // создаём подготовленный запрос и автоматически закрываем его в конце
            stmt.setString(1, login); // подставляем логин в первый параметр
            ResultSet rs = stmt.executeQuery(); // выполняем запрос и получаем результат
            if (rs.next()) { // проверяем, найден ли пользователь
                return false; // возвращаем false - пользователь уже существует
            }
        } catch (SQLException e) { // обрабатываем ошибку выполнения sql
            System.err.println("Ошибка проверки: " + e.getMessage()); // выводим сообщение об ошибке
            return false; // возвращаем false
        }
        
        String sql = "INSERT INTO " + SCHEMA + ".users (login, password_hash) VALUES (?, ?)"; // sql запрос для вставки нового пользователя
        try (PreparedStatement stmt = connection.prepareStatement(sql)) { // создаём подготовленный запрос и автоматически закрываем его в конце
            stmt.setString(1, login); // подставляем логин в первый параметр
            stmt.setString(2, hashPassword(password)); // подставляем хэш пароля во второй параметр
            stmt.executeUpdate(); // выполняем вставку
            return true; // возвращаем успех
        } catch (SQLException e) { // обрабатываем ошибку выполнения sql
            System.err.println("Ошибка регистрации: " + e.getMessage()); // выводим сообщение об ошибке
            return false; // возвращаем false
        }
    }

    /**
     * Проверяет подлинность пользователя по логину и паролю
     * Сравнивает хэш переданного пароля с сохранённым в базе данных
     * 
     * @param login логин пользователя
     * @param password пароль для проверки (в открытом виде)
     * @return true если аутентификация успешна, false в противном случае
     */
    public boolean authenticateUser(String login, String password) { // метод аутентификации пользователя
        String sql = "SELECT password_hash FROM " + SCHEMA + ".users WHERE login = ?"; // sql запрос для получения хэша пароля по логину
        try (PreparedStatement stmt = connection.prepareStatement(sql)) { // создаём подготовленный запрос и автоматически закрываем его в конце
            stmt.setString(1, login); // подставляем логин в первый параметр
            ResultSet rs = stmt.executeQuery(); // выполняем запрос и получаем результат
            if (rs.next()) { // проверяем, найден ли пользователь
                String storedHash = rs.getString("password_hash"); // получаем сохранённый хэш пароля
                String inputHash = hashPassword(password); // вычисляем хэш переданного пароля
                return storedHash.equals(inputHash); // сравниваем хэши и возвращаем результат
            }
            return false; // пользователь не найден - возвращаем false
        } catch (SQLException e) { // обрабатываем ошибку выполнения sql
            System.err.println("Ошибка аутентификации: " + e.getMessage()); // выводим сообщение об ошибке
            return false; // возвращаем false
        }
    }

    /**
     * Добавляет новое транспортное средство в базу данных
     * 
     * @param vehicle объект vehicle для добавления
     * @return сгенерированный id транспортного средства, или -1 в случае ошибки
     */
    public int addVehicle(Vehicle vehicle) { // метод добавления транспортного средства
        String sql = "INSERT INTO " + SCHEMA + ".vehicles (name, coordinate_x, coordinate_y, creation_date, " + // sql запрос для вставки новой записи
                "engine_power, capacity, type, fuel_type, owner_login) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?) RETURNING id"; // с возвращением id
        try (PreparedStatement stmt = connection.prepareStatement(sql)) { // создаём подготовленный запрос и автоматически закрываем его в конце
            stmt.setString(1, vehicle.getName()); // подставляем имя в первый параметр
            stmt.setDouble(2, vehicle.getCoordinates().getX()); // подставляем координату x во второй параметр
            stmt.setInt(3, vehicle.getCoordinates().getY()); // подставляем координату y в третий параметр
            stmt.setDate(4, Date.valueOf(vehicle.getCreationDate())); // подставляем дату создания в четвёртый параметр
            stmt.setDouble(5, vehicle.getEnginePower()); // подставляем мощность двигателя в пятый параметр
            stmt.setDouble(6, vehicle.getCapacity()); // подставляем вместимость в шестой параметр
            stmt.setString(7, vehicle.getType().name()); // подставляем тип транспортного средства в седьмой параметр
            stmt.setString(8, vehicle.getFuelType() != null ? vehicle.getFuelType().name() : null); // подставляем тип топлива (или null) в восьмой параметр
            stmt.setString(9, vehicle.getOwnerLogin()); // подставляем логин владельца в девятый параметр

            ResultSet rs = stmt.executeQuery(); // выполняем запрос и получаем результат (сгенерированный id)
            if (rs.next()) { // проверяем, есть ли результат
                return rs.getInt(1); // возвращаем сгенерированный id
            }
            return -1; // возвращаем -1, если id не получен
        } catch (SQLException e) { // обрабатываем ошибку выполнения sql
            System.err.println("Ошибка добавления Vehicle: " + e.getMessage()); // выводим сообщение об ошибке
            return -1; // возвращаем -1
        }
    }

    /**
     * Обновляет существующее транспортное средство в базе данных
     * Проверяет, что владелец совпадает с owner_login в записи
     * 
     * @param vehicle объект с обновлёнными данными (должен содержать id и owner_login)
     * @return true если обновление выполнено успешно, false в противном случае
     */
    public boolean updateVehicle(Vehicle vehicle) { // метод обновления транспортного средства
        String sql = "UPDATE " + SCHEMA + ".vehicles SET name = ?, coordinate_x = ?, coordinate_y = ?, " + // sql запрос для обновления записи
                "engine_power = ?, capacity = ?, type = ?, fuel_type = ? WHERE id = ? AND owner_login = ?"; // проверяем владельца
        try (PreparedStatement stmt = connection.prepareStatement(sql)) { // создаём подготовленный запрос и автоматически закрываем его в конце
            stmt.setString(1, vehicle.getName()); // подставляем новое имя в первый параметр
            stmt.setDouble(2, vehicle.getCoordinates().getX()); // подставляем новую координату x во второй параметр
            stmt.setInt(3, vehicle.getCoordinates().getY()); // подставляем новую координату y в третий параметр
            stmt.setDouble(4, vehicle.getEnginePower()); // подставляем новую мощность в четвёртый параметр
            stmt.setDouble(5, vehicle.getCapacity()); // подставляем новую вместимость в пятый параметр
            stmt.setString(6, vehicle.getType().name()); // подставляем новый тип в шестой параметр
            stmt.setString(7, vehicle.getFuelType() != null ? vehicle.getFuelType().name() : null); // подставляем тип топлива (или null) в седьмой параметр
            stmt.setInt(8, vehicle.getId()); // подставляем id в восьмой параметр
            stmt.setString(9, vehicle.getOwnerLogin()); // подставляем логин владельца в девятый параметр

            int affected = stmt.executeUpdate(); // выполняем обновление и получаем количество затронутых строк
            return affected > 0; // возвращаем true, если была обновлена хотя бы одна строка
        } catch (SQLException e) { // обрабатываем ошибку выполнения sql
            System.err.println("Ошибка обновления Vehicle: " + e.getMessage()); // выводим сообщение об ошибке
            return false; // возвращаем false
        }
    }

    /**
     * Удаляет транспортное средство из базы данных по id
     * 
     * @param id идентификатор удаляемого транспортного средства
     * @return true если удаление выполнено успешно, false в противном случае
     */
    public boolean deleteVehicle(int id) { // метод удаления транспортного средства
        String sql = "DELETE FROM " + SCHEMA + ".vehicles WHERE id = ?"; // sql запрос для удаления записи по id
        try (PreparedStatement stmt = connection.prepareStatement(sql)) { // создаём подготовленный запрос и автоматически закрываем его в конце
            stmt.setInt(1, id); // подставляем id в первый параметр
            int affected = stmt.executeUpdate(); // выполняем удаление и получаем количество затронутых строк
            return affected > 0; // возвращаем true, если была удалена хотя бы одна строка
        } catch (SQLException e) { // обрабатываем ошибку выполнения sql
            System.err.println("Ошибка удаления Vehicle: " + e.getMessage()); // выводим сообщение об ошибке
            return false; // возвращаем false
        }
    }

    /**
     * Загружает все транспортные средства из базы данных
     * 
     * @return стек (Stack) со всеми транспортными средствами, отсортированными по id
     */
    public Stack<Vehicle> loadAllVehicles() { // метод загрузки всех транспортных средств
        Stack<Vehicle> vehicles = new Stack<>(); // создаём новый стек для хранения транспортных средств
        String sql = "SELECT id, name, coordinate_x, coordinate_y, creation_date, " + // sql запрос для получения всех записей
                     "engine_power, capacity, type, fuel_type, owner_login FROM " + SCHEMA + ".vehicles ORDER BY id"; // сортируем по id
        try (Statement stmt = connection.createStatement(); // создаём statement и автоматически закрываем его в конце
             ResultSet rs = stmt.executeQuery(sql)) { // выполняем запрос и получаем результат

            while (rs.next()) { // проходим по всем строкам результата
                Vehicle v = new Vehicle(); // создаём новый объект транспортного средства
                v.setId(rs.getInt("id")); // устанавливаем id из столбца id
                v.setName(rs.getString("name")); // устанавливаем имя из столбца name

                Coordinates coords = new Coordinates(); // создаём новый объект координат
                coords.setX(rs.getDouble("coordinate_x")); // устанавливаем координату x из столбца coordinate_x
                coords.setY(rs.getInt("coordinate_y")); // устанавливаем координату y из столбца coordinate_y
                v.setCoordinates(coords); // устанавливаем координаты в транспортное средство

                v.setCreationDate(rs.getDate("creation_date").toLocalDate()); // получаем дату из столбца creation_date и преобразуем в localdate
                v.setEnginePower(rs.getDouble("engine_power")); // устанавливаем мощность двигателя из столбца engine_power
                v.setCapacity(rs.getDouble("capacity")); // устанавливаем вместимость из столбца capacity
                v.setType(VehicleType.valueOf(rs.getString("type"))); // устанавливаем тип транспортного средства из столбца type

                String fuelTypeStr = rs.getString("fuel_type"); // получаем строку с типом топлива (может быть null)
                if (fuelTypeStr != null && !fuelTypeStr.isEmpty()) { // проверяем, что тип топлива не null и не пустой
                    v.setFuelType(FuelType.valueOf(fuelTypeStr)); // устанавливаем тип топлива из столбца fuel_type
                }

                v.setOwnerLogin(rs.getString("owner_login")); // устанавливаем логин владельца из столбца owner_login

                vehicles.push(v); // добавляем транспортное средство в стек
            }
        } catch (SQLException e) { // обрабатываем ошибку выполнения sql
            System.err.println("Ошибка загрузки Vehicles: " + e.getMessage()); // выводим сообщение об ошибке
        }
        return vehicles; // возвращаем заполненный стек
    }
}