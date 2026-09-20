package dao;

import db.DatabaseConnection;
import model.match.Stadium;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/*
 * Administra las operaciones SQL de los estadios.
 */
public class StadiumDAO {

    public void add(
            Stadium stadium)
            throws SQLException {

        String sql =
                "INSERT INTO estadio (nombre, id_ciudad) "
                        + "VALUES (?, ?)";

        try (PreparedStatement statement =
                     DatabaseConnection
                             .getConnection()
                             .prepareStatement(
                                     sql,
                                     Statement.RETURN_GENERATED_KEYS
                             )) {

            statement.setString(
                    1,
                    stadium.getName()
            );

            statement.setInt(
                    2,
                    stadium.getCityId()
            );

            statement.executeUpdate();

            try (ResultSet result =
                         statement.getGeneratedKeys()) {

                if (result.next()) {

                    stadium.setId(
                            result.getInt(
                                    1
                            )
                    );
                }
            }
        }
    }

    public List<Stadium> findAll()
            throws SQLException {

        List<Stadium> stadiums =
                new ArrayList<>();

        String sql =
                "SELECT id_estadio, nombre, id_ciudad "
                        + "FROM estadio "
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

                Stadium stadium =
                        new Stadium(
                                result.getInt(
                                        "id_estadio"
                                ),
                                result.getString(
                                        "nombre"
                                ),
                                result.getInt(
                                        "id_ciudad"
                                )
                        );

                stadiums.add(
                        stadium
                );
            }
        }

        return stadiums;
    }

    public void update(
            Stadium stadium)
            throws SQLException {

        String sql =
                "UPDATE estadio "
                        + "SET nombre = ?, id_ciudad = ? "
                        + "WHERE id_estadio = ?";

        try (PreparedStatement statement =
                     DatabaseConnection
                             .getConnection()
                             .prepareStatement(
                                     sql
                             )) {

            statement.setString(
                    1,
                    stadium.getName()
            );

            statement.setInt(
                    2,
                    stadium.getCityId()
            );

            statement.setInt(
                    3,
                    stadium.getId()
            );

            statement.executeUpdate();
        }
    }

    public void delete(
            int stadiumId)
            throws SQLException {

        String sql =
                "DELETE FROM estadio "
                        + "WHERE id_estadio = ?";

        try (PreparedStatement statement =
                     DatabaseConnection
                             .getConnection()
                             .prepareStatement(
                                     sql
                             )) {

            statement.setInt(
                    1,
                    stadiumId
            );

            statement.executeUpdate();
        }
    }
}