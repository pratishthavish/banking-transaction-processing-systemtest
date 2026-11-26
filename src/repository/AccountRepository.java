
package repository;

import domain.Account;
import util.DBConnection;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class AccountRepository {

    public void save(Account account) {

        String sql = """
                INSERT INTO account
                (account_no, customer_id, balance, account_type)
                VALUES (?, ?, ?, ?)
                ON CONFLICT (account_no)
                DO UPDATE SET
                    balance = EXCLUDED.balance,
                    account_type = EXCLUDED.account_type
                """;

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, account.getAccountNo());
            statement.setString(2, account.getCustomerId());
            statement.setBigDecimal(3,
                    BigDecimal.valueOf(account.getBalance()));
            statement.setString(4, account.getAccountType());

            statement.executeUpdate();

        } catch (SQLException e) {
            throw new RuntimeException("Error saving account", e);
        }
    }

    public List<Account> findAll() {

        List<Account> accounts = new ArrayList<>();

        String sql = """
                SELECT account_no, customer_id, balance, account_type, status
                FROM account
                """;

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {

            while (resultSet.next()) {
                accounts.add(mapAccount(resultSet));
            }

        } catch (SQLException e) {
            throw new RuntimeException("Error retrieving accounts", e);
        }

        return accounts;
    }

    public Optional<Account> findByNumber(String accountNumber) {

        String sql = """
                SELECT account_no, customer_id, balance, account_type, status
                FROM account
                WHERE account_no = ?
                """;

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, accountNumber);

            try (ResultSet resultSet = statement.executeQuery()) {

                if (resultSet.next()) {
                    return Optional.of(mapAccount(resultSet));
                }
            }

        } catch (SQLException e) {
            throw new RuntimeException("Error finding account", e);
        }

        return Optional.empty();
    }

    public Optional<Account> findByNumberForUpdate(
            Connection connection, String accountNumber) throws SQLException {

        String sql = """
                SELECT account_no, customer_id, balance, account_type, status
                FROM account
                WHERE account_no = ?
                FOR UPDATE
                """;

        try (PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, accountNumber);

            try (ResultSet resultSet = statement.executeQuery()) {

                if (resultSet.next()) {
                    return Optional.of(mapAccount(resultSet));
                }
            }
        }

        return Optional.empty();
    }

    public void updateBalance(
            Connection connection,
            String accountNumber,
            BigDecimal balance) throws SQLException {

        String sql = """
                UPDATE account
                SET balance = ?
                WHERE account_no = ?
                  AND status = 'ACTIVE'
                """;

        try (PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setBigDecimal(1, balance);
            statement.setString(2, accountNumber);

            int rowsUpdated = statement.executeUpdate();

            if (rowsUpdated != 1) {
                throw new SQLException(
                        "Account balance could not be updated");
            }
        }
    }

    public List<Account> findByCustomerId(String customerId) {

        List<Account> accounts = new ArrayList<>();

        String sql = """
                SELECT account_no, customer_id, balance, account_type, status
                FROM account
                WHERE customer_id = ?
                """;

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, customerId);

            try (ResultSet resultSet = statement.executeQuery()) {

                while (resultSet.next()) {
                    accounts.add(mapAccount(resultSet));
                }
            }

        } catch (SQLException e) {
            throw new RuntimeException("Error finding customer accounts", e);
        }

        return accounts;
    }

    private Account mapAccount(ResultSet resultSet) throws SQLException {

        return new Account(
                resultSet.getString("account_no"),
                resultSet.getString("customer_id"),
                resultSet.getBigDecimal("balance").doubleValue(),
                resultSet.getString("account_type"),
                resultSet.getString("status")
        );
    }
}
