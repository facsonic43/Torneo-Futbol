package dao;

import db.DatabaseConnection;
import exceptions.CityHasStadiumsException;
import exceptions.DuplicateLocationException;
import model.match.City;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/*
 * Administra las operaciones SQL de las ciudades.
 */
public class CityDAO {

    public void add(City city) throws SQLException {
        if (exists(city.getName(), 0)) {
            throw new DuplicateLocationException("A city with this name already exists.");
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

    public List<City> findAll()
            throws SQLException {

        List<City> cities =
                new ArrayList<>();

        String sql =
                "SELECT id_ciudad, nombre, pais "
                        + "FROM ciudad "
                        + "ORDER BY nombre";

        try (Statement statement =
                     DatabaseConnection
                             .getConnection()
                             .createStatement();

             ResultSet result =
                     statement.executeQuery(
                             sql
                     )) {

            while (result.next()) {

                City city =
                        new City(
                                result.getInt(
                                        "id_ciudad"
                                ),
                                result.getString(
                                        "nombre"
                                ),
                                result.getString(
                                        "pais"
                                )
                        );

                cities.add(
                        city
                );
            }
        }

        return cities;
    }
    public void update(City city) throws SQLException {
        if (exists(city.getName(), city.getId())) {
            throw new DuplicateLocationException("A city with this name already exists.");
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
            if (isCityReferencedByStadium(exception)) {
                throw new CityHasStadiumsException(id);
            }
            throw exception;
        }
    }

    private boolean isCityReferencedByStadium(SQLException exception) {
        for (SQLException current = exception;
             current != null;
             current = current.getNextException()) {
            if ("23503".equals(current.getSQLState())
                    || (current.getMessage() != null
                    && current.getMessage().toLowerCase().contains("fk_estadio_ciudad"))) {
                return true;
            }
        }
        return false;
    }

    private boolean exists(String name, int excludedId) throws SQLException {
        String sql = "SELECT 1 FROM ciudad "
                + "WHERE LOWER(TRIM(nombre)) = LOWER(TRIM(?)) "
                + "AND id_ciudad <> ? LIMIT 1";
        try (PreparedStatement ps = DatabaseConnection.getConnection().prepareStatement(sql)) {
            ps.setString(1, name);
            ps.setInt(2, excludedId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }
}