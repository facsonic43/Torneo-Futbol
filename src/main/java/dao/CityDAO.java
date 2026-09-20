package dao;

import db.DatabaseConnection;
import model.match.City;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/*
 * Administra las operaciones SQL de las ciudades.
 */
public class CityDAO {

    public void add(
            City city)
            throws SQLException {

        String sql =
                "INSERT INTO ciudad (nombre, pais) "
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
                    city.getName()
            );

            statement.setString(
                    2,
                    city.getCountry()
            );

            statement.executeUpdate();

            try (ResultSet result =
                         statement.getGeneratedKeys()) {

                if (result.next()) {

                    city.setId(
                            result.getInt(
                                    1
                            )
                    );
                }
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

    public void update(
            City city)
            throws SQLException {

        String sql =
                "UPDATE ciudad "
                        + "SET nombre = ?, pais = ? "
                        + "WHERE id_ciudad = ?";

        try (PreparedStatement statement =
                     DatabaseConnection
                             .getConnection()
                             .prepareStatement(
                                     sql
                             )) {

            statement.setString(
                    1,
                    city.getName()
            );

            statement.setString(
                    2,
                    city.getCountry()
            );

            statement.setInt(
                    3,
                    city.getId()
            );

            statement.executeUpdate();
        }
    }

    public void delete(
            int cityId)
            throws SQLException {

        String sql =
                "DELETE FROM ciudad "
                        + "WHERE id_ciudad = ?";

        try (PreparedStatement statement =
                     DatabaseConnection
                             .getConnection()
                             .prepareStatement(
                                     sql
                             )) {

            statement.setInt(
                    1,
                    cityId
            );

            statement.executeUpdate();
        }
    }
}