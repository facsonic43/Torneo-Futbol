package dao;

import db.DatabaseConnection;
import exceptions.DuplicateLocationException;
import model.match.Stadium;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class StadiumDAO {

    public void add(Stadium stadium) throws SQLException {
        if (exists(stadium.getName(), 0)) {
            throw new DuplicateLocationException("Ya existe un estadio con ese nombre.");
        }

        String sql = "INSERT INTO estadio (nombre, id_ciudad) VALUES (?, ?)";
        try (PreparedStatement ps = DatabaseConnection.getConnection()
                .prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, stadium.getName());
            ps.setInt(2, stadium.getCityId());
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) stadium.setId(rs.getInt(1));
            }
        }
    }

    public List<Stadium> findAll() throws SQLException {
        List<Stadium> stadiums = new ArrayList<>();
        String sql = "SELECT id_estadio, nombre, id_ciudad FROM estadio ORDER BY nombre";
        try (Statement st = DatabaseConnection.getConnection().createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                stadiums.add(new Stadium(
                        rs.getInt("id_estadio"),
                        rs.getString("nombre"),
                        rs.getInt("id_ciudad")
                ));
            }
        }
        return stadiums;
    }

    public void update(Stadium stadium) throws SQLException {
        if (exists(stadium.getName(), stadium.getId())) {
            throw new DuplicateLocationException("Ya existe otro estadio con ese nombre.");
        }

        String sql = "UPDATE estadio SET nombre = ?, id_ciudad = ? WHERE id_estadio = ?";
        try (PreparedStatement ps = DatabaseConnection.getConnection().prepareStatement(sql)) {
            ps.setString(1, stadium.getName());
            ps.setInt(2, stadium.getCityId());
            ps.setInt(3, stadium.getId());
            ps.executeUpdate();
        }
    }

    public void delete(int id) throws SQLException {
        String sql = "DELETE FROM estadio WHERE id_estadio = ?";
        try (PreparedStatement ps = DatabaseConnection.getConnection().prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }

    private boolean exists(String name, int excludedId) throws SQLException {
        String sql = "SELECT 1 FROM estadio "
                + "WHERE LOWER(TRIM(nombre)) = LOWER(TRIM(?)) "
                + "AND id_estadio <> ? LIMIT 1";
        try (PreparedStatement ps = DatabaseConnection.getConnection().prepareStatement(sql)) {
            ps.setString(1, name);
            ps.setInt(2, excludedId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }
}
