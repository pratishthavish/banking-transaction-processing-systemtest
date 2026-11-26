
package util;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DBConnection {

    private static final String url =
            "jdbc:postgresql://localhost:5432/banking_db";

    private static final String user = "postgres";

    private static final String password =
            System.getenv("DB_PASSWORD");

    public static Connection getConnection() throws SQLException {
        if (password == null || password.isBlank()) {
            throw new SQLException("DB_PASSWORD environment variable is not set");
        }

        return DriverManager.getConnection(url, user, password);
    }
}
