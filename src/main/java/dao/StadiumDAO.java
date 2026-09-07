package dao;

import db.DatabaseConnection;
import model.match.Stadium;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class StadiumDAO {

    public void add(Stadium stadium) throws SQLException {
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

    public void delete(int id) throws SQLException {
        String sql = "DELETE FROM estadio WHERE id_estadio = ?";
        try (PreparedStatement ps = DatabaseConnection.getConnection().prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }
}
