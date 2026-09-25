package db;

import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

/*
 * Administra la conexión con la base de datos PostgreSQL.
 * Lee la configuración desde database.properties para que cada integrante
 * pueda utilizar sus propios datos de conexión sin modificar el código Java.
 */
public class DatabaseConnection {
    private static String url;
    private static String user;
    private static String password;
    private static Connection connection;

    private DatabaseConnection() {
    }

    // Lee una sola vez los datos necesarios para conectarse con PostgreSQL.
    private static void loadConfiguration() throws SQLException {
        if (url != null) {
            return;
        }

        Properties properties = new Properties();

        try (InputStream inputStream =
                     DatabaseConnection.class.getClassLoader()
                             .getResourceAsStream("database.properties")) {

            if (inputStream == null) {
                throw new SQLException(
                        "database.properties was not found in src/main/resources."
                );
            }

            properties.load(inputStream);

            url = properties.getProperty("db.url");
            user = properties.getProperty("db.user");
            password = properties.getProperty("db.password");

            if (url == null || user == null || password == null) {
                throw new SQLException(
                        "The database configuration is incomplete."
                );
            }

        } catch (SQLException exception) {
            throw exception;

        } catch (Exception exception) {
            throw new SQLException(
                    "Error reading database configuration: "
                            + exception.getMessage()
            );
        }
    }

    // Devuelve una conexión abierta y la vuelve a crear si fue cerrada.
    public static Connection getConnection() throws SQLException {
        loadConfiguration();

        if (connection == null || connection.isClosed()) {
            connection = DriverManager.getConnection(
                    url,
                    user,
                    password
            );
        }

        return connection;
    }

    // Permite cerrar correctamente la conexión cuando ya no se necesita.
    public static void closeConnection() {
        if (connection != null) {
            try {
                connection.close();
            } catch (SQLException exception) {
                System.out.println(
                        "Error closing database connection: "
                                + exception.getMessage()
                );
            }
        }
    }
}