package app;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * Utility class for managing database connections to the remote TAMU PostgreSQL server.
 * <p>
 * This class handles setting and retrieving database credentials
 * and provides a static method for obtaining a {@link Connection} object
 * for executing SQL queries throughout the POS system.
 * 
 * </p>
 * @author Ryan, Sam, Brenden 
 */
public class DatabaseConnector {

    // Remote TAMU Postgres database

    // TODO: Use scanner input to populate these instance variables. Boilerplate code in LineReader.java.
    private static String DB_URL = "";
    private static String DB_USER = "";
    private static String DB_PASSWORD = ""; // <— fill this in

    private static Connection connection = null;

    /**
     * Sets the database URL for the PostgreSQL connection.
     *
     * @param url the JDBC URL string for the database
     */
    public static void setDbUrl(String url) {
        DB_URL = url;
    }

    /**
     * Sets the username used for database authentication.
     *
     * @param user the username for the database account
     */
    public static void setDbUser(String user) {
        DB_USER = user;
    }

    /**
     * Sets the password used for database authentication.
     *
     * @param password the password for the database account
     */
    public static void setDbPassword(String password) {
        DB_PASSWORD = password;
    }

    /**
     * Establishes and returns a connection to the database.
     * <p>
     * Loads the PostgreSQL JDBC driver and attempts to connect using
     * the stored URL, username, and password. If the connection fails,
     * the method returns {@code null} and prints an error message.
     * </p>
     *
     * @return a {@link Connection} object if successful, or {@code null} if connection fails
     */
    public static Connection getConnection() {
        try {
            // Load PostgreSQL JDBC driver explicitly
            Class.forName("org.postgresql.Driver");
            Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
            System.out.println(" Connected to TAMU Postgres database successfully!");
            return conn;

        } catch (ClassNotFoundException e) {
            System.err.println(" PostgreSQL JDBC driver not found.");
            e.printStackTrace();
            return null;

        } catch (SQLException e) {
            System.err.println(" Database connection failed: " + e.getMessage());
            return null;
        }
    }
}

