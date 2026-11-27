
package service.impl;

import domain.Account;
import domain.Customer;
import domain.Transaction;
import domain.Type;
import exceptions.AccountNotFoundException;
import exceptions.InsufficientFundException;
import exceptions.ValidationException;
import repository.AccountRepository;
import repository.CustomerRepository;
import repository.TransactionRepository;
import service.BankService;
import util.DBConnection;
import util.Validation;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

public class BankServiceImpl implements BankService {

    private final AccountRepository accountRepository = new AccountRepository();
    private final TransactionRepository transactionRepository = new TransactionRepository();
    private final CustomerRepository customerRepository = new CustomerRepository();

    private final Validation<String> validateName = name -> {
        if (name == null || name.isBlank()) {
            throw new ValidationException("Name is required");
        }
    };

    private final Validation<String> validateEmail = email -> {
        if (email == null || !email.contains("@")) {
            throw new ValidationException("Valid email is required");
        }
    };

    private final Validation<String> validateType = type -> {
        if (type == null ||
                !(type.equalsIgnoreCase("SAVINGS") ||
                        type.equalsIgnoreCase("CURRENT"))) {
            throw new ValidationException("Account Type must be SAVINGS or CURRENT");
        }
    };

    private BigDecimal validateAmount(Double amount) {

        if (amount == null || !Double.isFinite(amount) || amount <= 0) {
            throw new ValidationException("Amount must be greater than zero");
        }

        return BigDecimal.valueOf(amount).setScale(2);
    }

    @Override
    public String openAccount(String name, String email, String accountType) {

        validateName.validate(name);
        validateEmail.validate(email);
        validateType.validate(accountType);

        String customerId = UUID.randomUUID().toString();
        Customer customer = new Customer(customerId, name, email);

        customerRepository.save(customer);

        String accountNumber = getAccountNumber();

        Account account = new Account(accountNumber, customerId, 0.0, accountType.toUpperCase());

        accountRepository.save(account);

        return accountNumber;
    }

    @Override
    public List<Account> listAccounts() {

        return accountRepository.findAll().stream().sorted(Comparator.comparing(Account::getAccountNo))
                .collect(Collectors.toList());
    }

    @Override
    public void deposit(String accountNumber, Double amount, String note,String idempotencyKey) {

        BigDecimal value = validateAmount(amount);

        executeTransaction(connection -> {

            Transaction existing = checkIdempotency(connection, idempotencyKey, null, accountNumber, value, note, Type.DEPOSIT);
            if (existing != null) {
                return;
            }

            Account account = getLockedAccount(connection, accountNumber);
            validateActiveAccount(account);

            BigDecimal newBalance = BigDecimal.valueOf(account.getBalance()).add(value);

            accountRepository.updateBalance(connection, accountNumber, newBalance);
            Transaction transaction = createTransaction(null, accountNumber, value, note, Type.DEPOSIT, idempotencyKey);
            transactionRepository.add(connection, transaction);
        });
    }

    @Override
    public void withdraw(String accountNumber, Double amount, String note,String idempotencyKey) {

        BigDecimal value = validateAmount(amount);

        executeTransaction(connection -> {
            Transaction existing = checkIdempotency(connection, idempotencyKey, accountNumber, null, value, note, Type.WITHDRAW);
            if (existing != null) {
                return;
            }

            Account account = getLockedAccount(connection, accountNumber);
            validateActiveAccount(account);

            BigDecimal currentBalance = BigDecimal.valueOf(account.getBalance());

            if (currentBalance.compareTo(value) < 0) {
                throw new InsufficientFundException("Insufficient balance");
            }

            BigDecimal newBalance = currentBalance.subtract(value);

            accountRepository.updateBalance(connection, accountNumber, newBalance);
            Transaction transaction = createTransaction(accountNumber, null, value, note, Type.WITHDRAW,idempotencyKey);
            transactionRepository.add(connection, transaction);
        });
    }

    @Override
    public void transfer(String fromAccount, String toAccount, Double amount, String note,String idempotencyKey) {

        if (fromAccount == null || toAccount == null || fromAccount.equals(toAccount)) {
            throw new ValidationException("Source and destination accounts must be different");
        }

        BigDecimal value = validateAmount(amount);

        executeTransaction(connection -> {
            Transaction existing = checkIdempotency(connection, idempotencyKey, fromAccount, toAccount, value, note, Type.TRANSFER);
            if (existing != null) {
                return;
            }

            String firstAccount = fromAccount.compareTo(toAccount) < 0 ? fromAccount : toAccount;
            String secondAccount = fromAccount.compareTo(toAccount) < 0 ? toAccount : fromAccount;

            Account first = getLockedAccount(connection, firstAccount);
            Account second = getLockedAccount(connection, secondAccount);

            Account source = fromAccount.equals(firstAccount) ? first : second;
            Account destination = toAccount.equals(firstAccount) ? first : second;

            validateActiveAccount(source);
            validateActiveAccount(destination);

            BigDecimal sourceBalance = BigDecimal.valueOf(source.getBalance());

            if (sourceBalance.compareTo(value) < 0) {
                throw new InsufficientFundException("Insufficient balance");
            }

            BigDecimal destinationBalance = BigDecimal.valueOf(destination.getBalance());

            accountRepository.updateBalance(connection, fromAccount, sourceBalance.subtract(value));
            accountRepository.updateBalance(connection, toAccount, destinationBalance.add(value));
            Transaction transaction = createTransaction(fromAccount, toAccount, value, note, Type.TRANSFER, idempotencyKey);
            transactionRepository.add(connection, transaction);
        });
    }

    @Override
    public List<Transaction> getStatement(String account) {

        return transactionRepository.findByAccount(account).stream()
                .sorted(Comparator.comparing(Transaction::getTimestamp)).collect(Collectors.toList());
    }

    @Override
    public List<Account> searchAccountByCustomerName(String query) {

        String search = query == null ? "" : query.toLowerCase();

        return customerRepository.findAll()
                .stream().filter(customer -> customer.getName().toLowerCase().contains(search))
                .flatMap(customer -> accountRepository.findByCustomerId(customer.getId()).stream())
                .sorted(Comparator.comparing(Account::getAccountNo)).collect(Collectors.toList());
    }

    private Account getLockedAccount(Connection connection, String accountNumber) throws SQLException {
        return accountRepository.findByNumberForUpdate(connection, accountNumber)
                .orElseThrow(() -> new AccountNotFoundException("Account not found: " + accountNumber));
    }

    private void validateActiveAccount(Account account) {

        if (!"ACTIVE".equalsIgnoreCase(account.getStatus())) {
            throw new ValidationException("Account is not active");
        }
    }

    private Transaction checkIdempotency(Connection connection, String idempotencyKey, String sourceAccount, String destinationAccount, BigDecimal amount, String note, Type type) throws SQLException {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            throw new ValidationException("Idempotency key is required");
        }
        transactionRepository.lockIdempotencyKey(connection, idempotencyKey);
        Transaction existing = transactionRepository.findByIdempotencyKey(connection, idempotencyKey);

        if (existing == null) {
            return null;
        }
        boolean sameRequest = existing.getType() == type && Objects.equals(existing.getSourceAccountNumber(), sourceAccount
        ) && Objects.equals(existing.getDestinationAccountNumber(), destinationAccount) && existing.getAmount().compareTo(amount) == 0 && Objects.equals(existing.getNote(), note);

        if (!sameRequest) {
            throw new ValidationException("Idempotency key has already been used for a different request");
        }

        return existing;
    }


    private Transaction createTransaction(String sourceAccount, String destinationAccount, BigDecimal amount, String note, Type type, String idempotencyKey) {
        String accountNumber = sourceAccount != null ? sourceAccount : destinationAccount;
        return new Transaction(UUID.randomUUID().toString(), idempotencyKey, accountNumber, sourceAccount,
                destinationAccount, amount, LocalDateTime.now(), note, type, "SUCCESS");
    }

    private void executeTransaction(SqlOperation operation) {

        try (Connection connection = DBConnection.getConnection()) {

            connection.setAutoCommit(false);

            try {
                operation.execute(connection);
                connection.commit();

            } catch (Exception e) {

                try {
                    connection.rollback();
                } catch (SQLException rollbackError) {
                    e.addSuppressed(rollbackError);
                }

                throw new RuntimeException(
                        "Banking transaction failed: " + e.getMessage(), e);
            }

        } catch (SQLException e) {
            throw new RuntimeException(
                    "Database transaction failed", e);
        }
    }

    @FunctionalInterface
    private interface SqlOperation {
        void execute(Connection connection) throws Exception;
    }

    private String getAccountNumber() {

        int size = accountRepository.findAll().size() + 1;

        return String.format("AC%06d", size);
    }
}
