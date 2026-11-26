
package repository;

import domain.Customer;
import util.DBConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class CustomerRepository {
    public void save(Customer c) {
        String sql = """
                INSERT INTO customer (customer_id, name, email)
                VALUES (?, ?, ?)
                ON CONFLICT (customer_id)
                DO UPDATE SET name = EXCLUDED.name,
                              email = EXCLUDED.email
                """;
        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, c.getId());
            statement.setString(2, c.getName());
            statement.setString(3, c.getEmail());
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Error saving customer", e);
        }
    }
    public List<Customer> findAll() {
        List<Customer> customers = new ArrayList<>();
        String sql = "SELECT customer_id, name, email FROM customer";
        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
                Customer customer = new Customer(
                        resultSet.getString("customer_id"),
                        resultSet.getString("name"),
                        resultSet.getString("email")
                );
                customers.add(customer);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error retrieving customers", e);
        }
        return customers;
    }
}
