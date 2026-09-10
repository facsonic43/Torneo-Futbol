package run;

import db.DatabaseConnection;
import java.sql.Connection;

public class TestConnection {
    public static void main(String[] args) {
        try {
            Connection conn = DatabaseConnection.getConnection();
            System.out.println("Connection successful: " + !conn.isClosed());
        } catch (Exception e) {
            System.out.println("Connection failed: " + e.getMessage());
        }
    }
}
