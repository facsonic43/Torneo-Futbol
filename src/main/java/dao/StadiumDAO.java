package dao;

import db.DatabaseConnection;
import exceptions.DuplicateLocationException;
import exceptions.InsufficientStadiumsException;
import model.match.Stadium;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

// Administra el alta, la consulta, la modificación y la baja de estadios en PostgreSQL,
// validando sus nombres y la ciudad a la que pertenecen.

public class StadiumDAO {
    public void add(Stadium stadium) throws SQLException {
        if (exists(stadium.getName(), 0)) {
            throw new DuplicateLocationException("A stadium with this name already exists.");
        }
        String sql = "INSERT INTO stadium (name, id_city) VALUES (?, ?)";
        try (PreparedStatement ps = DatabaseConnection.getConnection().prepareStatement(sql,
                Statement.RETURN_GENERATED_KEYS)) {
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
        String sql = "SELECT id_stadium, name, id_city " + "FROM stadium " + "ORDER BY name";
        try (Statement statement = DatabaseConnection.getConnection().createStatement(); ResultSet result = statement.executeQuery(sql)) {
            while (result.next()) {
                Stadium stadium = new Stadium(result.getInt("id_stadium"), result.getString("name"),
                        result.getInt("id_city"));
                stadiums.add(stadium);
            }
        }
        return stadiums;
    }

    public void delete(int stadiumId) throws SQLException {
        int remainingStadiums = findAll().size() - 1;
        if (remainingStadiums < 13) {
            throw new InsufficientStadiumsException(13, remainingStadiums);
        }
        String sql = "DELETE FROM stadium " + "WHERE id_stadium = ?";
        try (PreparedStatement statement = DatabaseConnection.getConnection().prepareStatement(sql)) {
            statement.setInt(1, stadiumId);
            statement.executeUpdate();
        }
    }

    public void update(Stadium stadium) throws SQLException {
        if (exists(stadium.getName(), stadium.getId())) {
            throw new DuplicateLocationException("A stadium with this name already exists.");
        }
        String sql = "UPDATE stadium SET name = ?, id_city = ? WHERE id_stadium = ?";
        try (PreparedStatement ps = DatabaseConnection.getConnection().prepareStatement(sql)) {
            ps.setString(1, stadium.getName());
            ps.setInt(2, stadium.getCityId());
            ps.setInt(3, stadium.getId());
            ps.executeUpdate();
        }
    }

    private boolean exists(String name, int excludedId) throws SQLException {
        String sql = "SELECT 1 FROM stadium " + "WHERE LOWER(TRIM(name)) = LOWER(TRIM(?)) "
                + "AND id_stadium <> ? LIMIT 1";
        try (PreparedStatement ps = DatabaseConnection.getConnection().prepareStatement(sql)) {
            ps.setString(1, name);
            ps.setInt(2, excludedId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }
}
