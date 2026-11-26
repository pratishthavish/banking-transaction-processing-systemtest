package app;

import util.DBConnection;
import java.sql.Connection;
import java.sql.SQLException;

public class DBConnectionTest {

    public static void main(String[] args) {

        try (Connection connection = DBConnection.getConnection()) {

            if (connection != null) {
                System.out.println("PostgreSQL connected successfully!");
            }

        } catch (SQLException e) {
            System.out.println("Database connection failed!");
            e.printStackTrace();
        }
    }
}