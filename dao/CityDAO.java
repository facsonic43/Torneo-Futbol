package dao;

import db.DatabaseConnection;
import model.match.City;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class CityDAO {

    public void add(City city) throws SQLException {
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

    public void delete(int id) throws SQLException {
        String sql = "DELETE FROM ciudad WHERE id_ciudad = ?";
        try (PreparedStatement ps = DatabaseConnection.getConnection().prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
        // Nota: si la ciudad tiene estadios asociados, Postgres va a tirar
        // una SQLException por la FK (ON DELETE RESTRICT). Conviene capturarla
        // más arriba y lanzar una excepción propia del dominio (carpeta "exceptions").
    }
}
