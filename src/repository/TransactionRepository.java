
package repository;

import domain.Transaction;
import domain.Type;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class TransactionRepository {

    public void add(Connection connection, Transaction transaction)
            throws SQLException {

        String sql = """
                INSERT INTO bank_transaction (
                    transaction_id,
                    idempotency_key,
                    transaction_type,
                    source_account_no,
                    destination_account_no,
                    amount,
                    status,
                    description,
                    created_at
                )
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;

        try (PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, transaction.getId());
            statement.setString(2, transaction.getIdempotencyKey());
            statement.setString(3, transaction.getType().name());
            statement.setString(4, transaction.getSourceAccountNumber());
            statement.setString(5, transaction.getDestinationAccountNumber());
            statement.setBigDecimal(6, transaction.getAmount());
            statement.setString(7, transaction.getStatus());
            statement.setString(8, transaction.getNote());
            statement.setTimestamp(9,
                    java.sql.Timestamp.valueOf(transaction.getTimestamp()));

            statement.executeUpdate();
        }
    }

    public List<Transaction> findByAccount(String accountNumber) {

        List<Transaction> transactions = new ArrayList<>();

        String sql = """
                SELECT
                    transaction_id,
                    idempotency_key,
                    source_account_no,
                    destination_account_no,
                    amount,
                    status,
                    description,
                    created_at,
                    transaction_type,
                    CASE
                        WHEN source_account_no = ? THEN source_account_no
                        ELSE destination_account_no
                    END AS account_number
                FROM bank_transaction
                WHERE source_account_no = ?
                   OR destination_account_no = ?
                ORDER BY created_at
                """;

        try (Connection connection =
                     util.DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, accountNumber);
            statement.setString(2, accountNumber);
            statement.setString(3, accountNumber);

            try (ResultSet resultSet = statement.executeQuery()) {

                while (resultSet.next()) {

                    Transaction transaction = new Transaction(
                            resultSet.getString("transaction_id"),
                            resultSet.getString("idempotency_key"),
                            resultSet.getString("account_number"),
                            resultSet.getString("source_account_no"),
                            resultSet.getString("destination_account_no"),
                            resultSet.getBigDecimal("amount"),
                            resultSet.getTimestamp("created_at").toLocalDateTime(),
                            resultSet.getString("description"),
                            Type.valueOf(resultSet.getString("transaction_type")),
                            resultSet.getString("status")
                    );

                    transactions.add(transaction);
                }
            }

        } catch (SQLException e) {
            throw new RuntimeException("Error retrieving transaction history", e);
        }

        return transactions;
    }
}
