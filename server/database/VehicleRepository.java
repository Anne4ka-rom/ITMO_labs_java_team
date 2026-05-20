package server.database; // класс находится в пакете database серверной части

import common.model.*; // импорт всех классов модели из общей части
import java.sql.*; // импорт всех классов для работы с jdbc и sql
import java.time.LocalDate; // импорт класса для работы с датами
import java.util.*; // импорт всех утилитных классов

/**
 * Управляет операциями с транспортными средствами в базе данных
 * Отвечает за загрузку, добавление, обновление, удаление и проверку владения
 * 
 * @author Anni
 * @version 1.0
 */
public class VehicleRepository { // объявляет класс репозитория транспортных средств
    private final DatabaseManager dbManager; // менеджер базы данных для получения соединения

    /**
     * Создаёт новый репозиторий транспортных средств с указанным менеджером базы данных
     * 
     * @param dbManager менеджер для управления подключением к бд
     */
    public VehicleRepository(DatabaseManager dbManager) { // конструктор класса
        this.dbManager = dbManager; // сохраняем ссылку на менеджер базы данных
    }

    /**
     * Загружает все транспортные средства из базы данных
     * Результат сортируется по id
     * 
     * @return стек (Stack) со всеми транспортными средствами из бд
     * @throws SQLException если ошибка при выполнении sql запроса
     */
    public Stack<Vehicle> loadAllVehicles() throws SQLException { // метод загрузки всех транспортных средств
        Stack<Vehicle> vehicles = new Stack<>(); // создаём новый стек для хранения транспортных средств
        String sql = "SELECT * FROM vehicles ORDER BY id"; // sql запрос для получения всех записей, отсортированных по id
        try (Statement stmt = dbManager.getConnection().createStatement(); // создаём statement и автоматически закрываем его в конце
             ResultSet rs = stmt.executeQuery(sql)) { // выполняем запрос и получаем результат
            while (rs.next()) { // проходим по всем строкам результата
                Vehicle v = mapRowToVehicle(rs); // преобразуем текущую строку в объект vehicle
                vehicles.push(v); // добавляем транспортное средство в стек
            }
        }
        return vehicles; // возвращаем заполненный стек
    }

    /**
     * Загружает транспортные средства только указанного пользователя
     * Результат сортируется по id
     * 
     * @param username имя владельца для фильтрации
     * @return стек (Stack) с транспортными средствами пользователя
     * @throws SQLException если ошибка при выполнении sql запроса
     */
    public Stack<Vehicle> loadUserVehicles(String username) throws SQLException { // метод загрузки транспортных средств пользователя
        Stack<Vehicle> vehicles = new Stack<>(); // создаём новый стек для хранения транспортных средств
        String sql = "SELECT * FROM vehicles WHERE owner_username = ? ORDER BY id"; // sql запрос для получения записей по владельцу
        try (PreparedStatement stmt = dbManager.getConnection().prepareStatement(sql)) { // создаём подготовленный запрос и автоматически закрываем его в конце
            stmt.setString(1, username); // подставляем имя пользователя в первый параметр
            ResultSet rs = stmt.executeQuery(); // выполняем запрос и получаем результат
            while (rs.next()) { // проходим по всем строкам результата
                Vehicle v = mapRowToVehicle(rs); // преобразуем текущую строку в объект vehicle
                vehicles.push(v); // добавляем транспортное средство в стек
            }
        }
        return vehicles; // возвращаем заполненный стек
    }

    /**
     * Добавляет новое транспортное средство в базу данных
     * Генерируется новый id с помощью sequence
     * 
     * @param vehicle объект vehicle для добавления
     * @param ownerUsername имя владельца транспортного средства
     * @return сгенерированный базой данных id нового транспортного средства
     * @throws SQLException если ошибка при выполнении sql запроса
     */
    public int addVehicle(Vehicle vehicle, String ownerUsername) throws SQLException { // метод добавления транспортного средства
        String sql = """ // sql запрос для вставки новой записи с возвращением id
            INSERT INTO vehicles (name, coord_x, coord_y, creation_date, 
                                  engine_power, capacity, type, fuel_type, owner_username)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
            RETURNING id
        """;
        try (PreparedStatement stmt = dbManager.getConnection().prepareStatement(sql)) { // создаём подготовленный запрос и автоматически закрываем его в конце
            stmt.setString(1, vehicle.getName()); // подставляем имя в первый параметр
            stmt.setDouble(2, vehicle.getCoordinates().getX()); // подставляем координату x во второй параметр
            stmt.setInt(3, vehicle.getCoordinates().getY()); // подставляем координату y в третий параметр
            stmt.setDate(4, Date.valueOf(vehicle.getCreationDate())); // подставляем дату создания в четвёртый параметр
            stmt.setDouble(5, vehicle.getEnginePower()); // подставляем мощность двигателя в пятый параметр
            stmt.setDouble(6, vehicle.getCapacity()); // подставляем вместимость в шестой параметр
            stmt.setString(7, vehicle.getType().name()); // подставляем тип транспортного средства в седьмой параметр
            stmt.setString(8, vehicle.getFuelType() != null ? vehicle.getFuelType().name() : null); // подставляем тип топлива (или null) в восьмой параметр
            stmt.setString(9, ownerUsername); // подставляем имя владельца в девятый параметр

            ResultSet rs = stmt.executeQuery(); // выполняем запрос и получаем результат (сгенерированный id)
            dbManager.getConnection().commit(); // фиксируем транзакцию
            if (rs.next()) { // проверяем, есть ли результат (должен быть)
                return rs.getInt(1); // возвращаем сгенерированный id из первого столбца
            }
            throw new SQLException("Не удалось получить сгенерированный ID"); // выбрасываем исключение, если id не получен
        } catch (SQLException e) { // обрабатываем ошибку выполнения sql
            dbManager.getConnection().rollback(); // откатываем транзакцию при ошибке
            throw e; // пробрасываем исключение дальше
        }
    }

    /**
     * Обновляет существующее транспортное средство в базе данных
     * Предварительно проверяет, принадлежит ли элемент пользователю
     * 
     * @param id идентификатор обновляемого транспортного средства
     * @param newVehicle объект с новыми данными
     * @param ownerUsername имя владельца для проверки прав
     * @return true если обновление выполнено успешно, false если элемент не принадлежит пользователю
     * @throws SQLException если ошибка при выполнении sql запроса
     */
    public boolean updateVehicle(int id, Vehicle newVehicle, String ownerUsername) throws SQLException { // метод обновления транспортного средства
        if (!checkOwnership(id, ownerUsername)) { // проверяем, принадлежит ли элемент пользователю
            return false; // возвращаем false, если владелец не совпадает
        }

        String sql = """ // sql запрос для обновления записи по id
            UPDATE vehicles SET 
                name = ?, coord_x = ?, coord_y = ?, creation_date = ?,
                engine_power = ?, capacity = ?, type = ?, fuel_type = ?
            WHERE id = ?
        """;
        try (PreparedStatement stmt = dbManager.getConnection().prepareStatement(sql)) { // создаём подготовленный запрос и автоматически закрываем его в конце
            stmt.setString(1, newVehicle.getName()); // подставляем новое имя в первый параметр
            stmt.setDouble(2, newVehicle.getCoordinates().getX()); // подставляем новую координату x во второй параметр
            stmt.setInt(3, newVehicle.getCoordinates().getY()); // подставляем новую координату y в третий параметр
            stmt.setDate(4, Date.valueOf(newVehicle.getCreationDate())); // подставляем дату создания в четвёртый параметр
            stmt.setDouble(5, newVehicle.getEnginePower()); // подставляем новую мощность в пятый параметр
            stmt.setDouble(6, newVehicle.getCapacity()); // подставляем новую вместимость в шестой параметр
            stmt.setString(7, newVehicle.getType().name()); // подставляем новый тип в седьмой параметр
            stmt.setString(8, newVehicle.getFuelType() != null ? newVehicle.getFuelType().name() : null); // подставляем тип топлива (или null) в восьмой параметр
            stmt.setInt(9, id); // подставляем id записи в девятый параметр

            int affected = stmt.executeUpdate(); // выполняем обновление и получаем количество затронутых строк
            dbManager.getConnection().commit(); // фиксируем транзакцию
            return affected > 0; // возвращаем true, если была обновлена хотя бы одна строка
        } catch (SQLException e) { // обрабатываем ошибку выполнения sql
            dbManager.getConnection().rollback(); // откатываем транзакцию при ошибке
            throw e; // пробрасываем исключение дальше
        }
    }

    /**
     * Удаляет транспортное средство из базы данных по id
     * Предварительно проверяет, принадлежит ли элемент пользователю
     * 
     * @param id идентификатор удаляемого транспортного средства
     * @param ownerUsername имя владельца для проверки прав
     * @return true если удаление выполнено успешно, false если элемент не найден или не принадлежит пользователю
     * @throws SQLException если ошибка при выполнении sql запроса
     */
    public boolean deleteVehicle(int id, String ownerUsername) throws SQLException { // метод удаления транспортного средства
        if (!checkOwnership(id, ownerUsername)) { // проверяем, принадлежит ли элемент пользователю
            return false; // возвращаем false, если владелец не совпадает
        }

        String sql = "DELETE FROM vehicles WHERE id = ?"; // sql запрос для удаления записи по id
        try (PreparedStatement stmt = dbManager.getConnection().prepareStatement(sql)) { // создаём подготовленный запрос и автоматически закрываем его в конце
            stmt.setInt(1, id); // подставляем id записи в первый параметр
            int affected = stmt.executeUpdate(); // выполняем удаление и получаем количество затронутых строк
            dbManager.getConnection().commit(); // фиксируем транзакцию
            return affected > 0; // возвращаем true, если была удалена хотя бы одна строка
        } catch (SQLException e) { // обрабатываем ошибку выполнения sql
            dbManager.getConnection().rollback(); // откатываем транзакцию при ошибке
            throw e; // пробрасываем исключение дальше
        }
    }

    /**
     * Удаляет все транспортные средства указанного пользователя
     * 
     * @param ownerUsername имя владельца, чьи транспортные средства нужно удалить
     * @return количество удалённых записей
     * @throws SQLException если ошибка при выполнении sql запроса
     */
    public int clearUserVehicles(String ownerUsername) throws SQLException { // метод удаления всех транспортных средств пользователя
        String sql = "DELETE FROM vehicles WHERE owner_username = ?"; // sql запрос для удаления всех записей владельца
        try (PreparedStatement stmt = dbManager.getConnection().prepareStatement(sql)) { // создаём подготовленный запрос и автоматически закрываем его в конце
            stmt.setString(1, ownerUsername); // подставляем имя владельца в первый параметр
            int deleted = stmt.executeUpdate(); // выполняем удаление и получаем количество удалённых строк
            dbManager.getConnection().commit(); // фиксируем транзакцию
            return deleted; // возвращаем количество удалённых записей
        } catch (SQLException e) { // обрабатываем ошибку выполнения sql
            dbManager.getConnection().rollback(); // откатываем транзакцию при ошибке
            throw e; // пробрасываем исключение дальше
        }
    }

    /**
     * Проверяет, принадлежит ли транспортное средство с указанным id указанному пользователю
     * 
     * @param vehicleId идентификатор транспортного средства
     * @param username имя пользователя для проверки
     * @return true если транспортное средство существует и принадлежит пользователю, false в противном случае
     * @throws SQLException если ошибка при выполнении sql запроса
     */
    public boolean checkOwnership(int vehicleId, String username) throws SQLException { // метод проверки владения
        String sql = "SELECT owner_username FROM vehicles WHERE id = ?"; // sql запрос для получения владельца по id
        try (PreparedStatement stmt = dbManager.getConnection().prepareStatement(sql)) { // создаём подготовленный запрос и автоматически закрываем его в конце
            stmt.setInt(1, vehicleId); // подставляем id транспортного средства в первый параметр
            ResultSet rs = stmt.executeQuery(); // выполняем запрос и получаем результат
            if (rs.next()) { // проверяем, найдена ли запись
                return rs.getString("owner_username").equals(username); // возвращаем результат сравнения владельца из бд с переданным именем
            }
            return false; // запись не найдена - возвращаем false
        }
    }

    /**
     * Преобразует текущую строку ResultSet в объект Vehicle
     * Извлекает все поля из результирующего набора и создаёт объект
     * 
     * @param rs ResultSet, указывающий на строку с данными транспортного средства
     * @return сконструированный объект Vehicle с заполненными полями
     * @throws SQLException если ошибка при чтении данных из ResultSet
     */
    private Vehicle mapRowToVehicle(ResultSet rs) throws SQLException { // приватный метод преобразования строки бд в объект
        Vehicle v = new Vehicle(); // создаём новый пустой объект транспортного средства
        
        v.setId(rs.getInt("id")); // устанавливаем id из столбца id
        
        v.setName(rs.getString("name")); // устанавливаем имя из столбца name

        Coordinates coords = new Coordinates(); // создаём новый объект координат
        coords.setX(rs.getDouble("coord_x")); // устанавливаем координату x из столбца coord_x
        coords.setY(rs.getInt("coord_y")); // устанавливаем координату y из столбца coord_y
        v.setCoordinates(coords); // устанавливаем координаты в транспортное средство

        v.setCreationDate(rs.getDate("creation_date").toLocalDate()); // получаем дату из столбца creation_date и преобразуем в localdate
        v.setEnginePower(rs.getDouble("engine_power")); // устанавливаем мощность двигателя из столбца engine_power
        v.setCapacity(rs.getDouble("capacity")); // устанавливаем вместимость из столбца capacity
        v.setType(VehicleType.valueOf(rs.getString("type"))); // устанавливаем тип транспортного средства из столбца type

        String fuelTypeStr = rs.getString("fuel_type"); // получаем строку с типом топлива (может быть null)
        v.setFuelType(fuelTypeStr != null ? FuelType.valueOf(fuelTypeStr) : null); // устанавливаем тип топлива (или null) из столбца fuel_type

        v.setOwnerUsername(rs.getString("owner_username")); // устанавливаем имя владельца из столбца owner_username

        return v; // возвращаем сконструированное транспортное средство
    }
}