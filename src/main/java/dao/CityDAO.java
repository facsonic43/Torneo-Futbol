package dao;

import db.DatabaseConnection;
import exceptions.CityHasStadiumsException;
import exceptions.DuplicateLocationException;
import model.match.City;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class CityDAO {

    public void add(City city) throws SQLException {
        if (exists(city.getName(), city.getCountry(), 0)) {
            throw new DuplicateLocationException("Ya existe una ciudad con ese nombre y país.");
        }

        String sql = "INSERT INTO ciudad (nombre, pais) VALUES (?, ?)";
        try (PreparedStatement ps = DatabaseConnection.getConnection()
                .prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, city.getName());
            ps.setString(2, city.getCountry());
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) city.setId(rs.getInt(1));
            }
        }
    }

    public List<City> findAll() throws SQLException {
        List<City> cities = new ArrayList<>();
        String sql = "SELECT id_ciudad, nombre, pais FROM ciudad ORDER BY nombre";
        try (Statement st = DatabaseConnection.getConnection().createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                cities.add(new City(
                        rs.getInt("id_ciudad"),
                        rs.getString("nombre"),
                        rs.getString("pais")
                ));
            }
        }
        return cities;
    }

    public void update(City city) throws SQLException {
        if (exists(city.getName(), city.getCountry(), city.getId())) {
            throw new DuplicateLocationException("Ya existe otra ciudad con ese nombre y país.");
        }

        String sql = "UPDATE ciudad SET nombre = ?, pais = ? WHERE id_ciudad = ?";
        try (PreparedStatement ps = DatabaseConnection.getConnection().prepareStatement(sql)) {
            ps.setString(1, city.getName());
            ps.setString(2, city.getCountry());
            ps.setInt(3, city.getId());
            ps.executeUpdate();
        }
    }

    public void delete(int id) throws SQLException {
        String sql = "DELETE FROM ciudad WHERE id_ciudad = ?";
        try (PreparedStatement ps = DatabaseConnection.getConnection().prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        } catch (SQLException exception) {
            if ("23503".equals(exception.getSQLState())) {
                throw new CityHasStadiumsException(id);
            }
            throw exception;
        }
    }

    private boolean exists(String name, String country, int excludedId) throws SQLException {
        String sql = "SELECT 1 FROM ciudad "
                + "WHERE LOWER(TRIM(nombre)) = LOWER(TRIM(?)) "
                + "AND LOWER(TRIM(pais)) = LOWER(TRIM(?)) "
                + "AND id_ciudad <> ? LIMIT 1";
        try (PreparedStatement ps = DatabaseConnection.getConnection().prepareStatement(sql)) {
            ps.setString(1, name);
            ps.setString(2, country);
            ps.setInt(3, excludedId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }
}
