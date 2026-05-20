package server.database;

import common.model.*;
import java.sql.*;
import java.time.LocalDate;
import java.util.*;

public class VehicleRepository {
    private final DatabaseManager dbManager;

    public VehicleRepository(DatabaseManager dbManager) {
        this.dbManager = dbManager;
    }

    public Stack<Vehicle> loadAllVehicles() throws SQLException {
        Stack<Vehicle> vehicles = new Stack<>();
        String sql = "SELECT * FROM vehicles ORDER BY id";
        try (Statement stmt = dbManager.getConnection().createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                Vehicle v = mapRowToVehicle(rs);
                vehicles.push(v);
            }
        }
        return vehicles;
    }

    public Stack<Vehicle> loadUserVehicles(String username) throws SQLException {
        Stack<Vehicle> vehicles = new Stack<>();
        String sql = "SELECT * FROM vehicles WHERE owner_username = ? ORDER BY id";
        try (PreparedStatement stmt = dbManager.getConnection().prepareStatement(sql)) {
            stmt.setString(1, username);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                Vehicle v = mapRowToVehicle(rs);
                vehicles.push(v);
            }
        }
        return vehicles;
    }

    public int addVehicle(Vehicle vehicle, String ownerUsername) throws SQLException {
        String sql = """
            INSERT INTO vehicles (name, coord_x, coord_y, creation_date, 
                                  engine_power, capacity, type, fuel_type, owner_username)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
            RETURNING id
        """;
        try (PreparedStatement stmt = dbManager.getConnection().prepareStatement(sql)) {
            stmt.setString(1, vehicle.getName());
            stmt.setDouble(2, vehicle.getCoordinates().getX());
            stmt.setInt(3, vehicle.getCoordinates().getY());
            stmt.setDate(4, Date.valueOf(vehicle.getCreationDate()));
            stmt.setDouble(5, vehicle.getEnginePower());
            stmt.setDouble(6, vehicle.getCapacity());
            stmt.setString(7, vehicle.getType().name());
            stmt.setString(8, vehicle.getFuelType() != null ? vehicle.getFuelType().name() : null);
            stmt.setString(9, ownerUsername);

            ResultSet rs = stmt.executeQuery();
            dbManager.getConnection().commit();
            if (rs.next()) {
                return rs.getInt(1);
            }
            throw new SQLException("Не удалось получить сгенерированный ID");
        } catch (SQLException e) {
            dbManager.getConnection().rollback();
            throw e;
        }
    }

    public boolean updateVehicle(int id, Vehicle newVehicle, String ownerUsername) throws SQLException {
        if (!checkOwnership(id, ownerUsername)) {
            return false;
        }

        String sql = """
            UPDATE vehicles SET 
                name = ?, coord_x = ?, coord_y = ?, creation_date = ?,
                engine_power = ?, capacity = ?, type = ?, fuel_type = ?
            WHERE id = ?
        """;
        try (PreparedStatement stmt = dbManager.getConnection().prepareStatement(sql)) {
            stmt.setString(1, newVehicle.getName());
            stmt.setDouble(2, newVehicle.getCoordinates().getX());
            stmt.setInt(3, newVehicle.getCoordinates().getY());
            stmt.setDate(4, Date.valueOf(newVehicle.getCreationDate()));
            stmt.setDouble(5, newVehicle.getEnginePower());
            stmt.setDouble(6, newVehicle.getCapacity());
            stmt.setString(7, newVehicle.getType().name());
            stmt.setString(8, newVehicle.getFuelType() != null ? newVehicle.getFuelType().name() : null);
            stmt.setInt(9, id);

            int affected = stmt.executeUpdate();
            dbManager.getConnection().commit();
            return affected > 0;
        } catch (SQLException e) {
            dbManager.getConnection().rollback();
            throw e;
        }
    }

    public boolean deleteVehicle(int id, String ownerUsername) throws SQLException {
        if (!checkOwnership(id, ownerUsername)) {
            return false;
        }

        String sql = "DELETE FROM vehicles WHERE id = ?";
        try (PreparedStatement stmt = dbManager.getConnection().prepareStatement(sql)) {
            stmt.setInt(1, id);
            int affected = stmt.executeUpdate();
            dbManager.getConnection().commit();
            return affected > 0;
        } catch (SQLException e) {
            dbManager.getConnection().rollback();
            throw e;
        }
    }

    public int clearUserVehicles(String ownerUsername) throws SQLException {
        String sql = "DELETE FROM vehicles WHERE owner_username = ?";
        try (PreparedStatement stmt = dbManager.getConnection().prepareStatement(sql)) {
            stmt.setString(1, ownerUsername);
            int deleted = stmt.executeUpdate();
            dbManager.getConnection().commit();
            return deleted;
        } catch (SQLException e) {
            dbManager.getConnection().rollback();
            throw e;
        }
    }

    public boolean checkOwnership(int vehicleId, String username) throws SQLException {
        String sql = "SELECT owner_username FROM vehicles WHERE id = ?";
        try (PreparedStatement stmt = dbManager.getConnection().prepareStatement(sql)) {
            stmt.setInt(1, vehicleId);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getString("owner_username").equals(username);
            }
            return false;
        }
    }

    private Vehicle mapRowToVehicle(ResultSet rs) throws SQLException {
        Vehicle v = new Vehicle();
        v.setId(rs.getInt("id"));
        v.setName(rs.getString("name"));

        Coordinates coords = new Coordinates();
        coords.setX(rs.getDouble("coord_x"));
        coords.setY(rs.getInt("coord_y"));
        v.setCoordinates(coords);

        v.setCreationDate(rs.getDate("creation_date").toLocalDate());
        v.setEnginePower(rs.getDouble("engine_power"));
        v.setCapacity(rs.getDouble("capacity"));
        v.setType(VehicleType.valueOf(rs.getString("type")));

        String fuelTypeStr = rs.getString("fuel_type");
        v.setFuelType(fuelTypeStr != null ? FuelType.valueOf(fuelTypeStr) : null);

        v.setOwnerUsername(rs.getString("owner_username"));

        return v;
    }
}