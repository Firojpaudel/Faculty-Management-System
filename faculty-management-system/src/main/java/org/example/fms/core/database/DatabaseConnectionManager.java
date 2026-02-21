package org.example.fms.core.database;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * Manages the connection to the MySQL database.
 * Follows the Singleton pattern to ensure a single connection pool or
 * configuration.
 */
public class DatabaseConnectionManager {

    private static final String URL = "jdbc:mysql://localhost:3306/mbmc_fms";
    private static final String USER = "root"; // XAMPP default username
    private static final String PASSWORD = ""; // XAMPP default password is empty

    static {
        try {
            // Explicitly load the MySQL driver
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("Failed to load MySQL JDBC driver", e);
        }
    }

    /**
     * Retrieves a connection to the database.
     * 
     * @return Connection object
     * @throws SQLException if a database access error occurs
     */
    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL, USER, PASSWORD);
    }
}
