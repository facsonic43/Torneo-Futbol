package db;

import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

// Carga la configuración de PostgreSQL y mantiene una conexión compartida.
// La conexión se vuelve a abrir si fue cerrada y puede cerrarse al finalizar su uso.

public class DatabaseConnection {
    private static String url;
    private static String user;
    private static String password;
    private static Connection connection;

    private DatabaseConnection() {
    }

    public static Connection getConnection() throws SQLException {
        loadConfiguration();
        if (connection == null || connection.isClosed()) {
            connection = DriverManager.getConnection(url, user, password);
        }
        return connection;
    }

    public static String getUrl() {
        return url;
    }

    public static String getUser() {
        return user;
    }

    public static String getPassword() {
        return password;
    }

    private static void loadConfiguration() throws SQLException {
        if (url != null) {
            return;
        }
        Properties properties = new Properties();
        try (InputStream inputStream = DatabaseConnection.class.getClassLoader() .getResourceAsStream("database.properties")) {
            if (inputStream == null) {
                throw new SQLException("database.properties was not found in src/main/resources.");
            }
            properties.load(inputStream);
            url = properties.getProperty("db.url");
            user = properties.getProperty("db.user");
            password = properties.getProperty("db.password");
            if (url == null || user == null || password == null) {
                throw new SQLException("The database configuration is incomplete.");
            }
        } catch (SQLException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new SQLException("Error reading database configuration: " + exception.getMessage());
        }
    }

    public static void closeConnection() {
        if (connection != null) {
            try {
                connection.close();
            } catch (SQLException exception) {
                System.out.println("Error closing database connection: " + exception.getMessage());
            }
        }
    }
}
