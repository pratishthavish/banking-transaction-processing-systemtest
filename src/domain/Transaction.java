
package domain;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class Transaction {

    private String id;
    private String idempotencyKey;
    private String accountNumber;
    private String sourceAccountNumber;
    private String destinationAccountNumber;
    private BigDecimal amount;
    private LocalDateTime timestamp;
    private String note;
    private Type type;
    private String status;

    public Transaction(
            String id,
            String idempotencyKey,
            String accountNumber,
            String sourceAccountNumber,
            String destinationAccountNumber,
            BigDecimal amount,
            LocalDateTime timestamp,
            String note,
            Type type,
            String status) {

        this.id = id;
        this.idempotencyKey = idempotencyKey;
        this.accountNumber = accountNumber;
        this.sourceAccountNumber = sourceAccountNumber;
        this.destinationAccountNumber = destinationAccountNumber;
        this.amount = amount;
        this.timestamp = timestamp;
        this.note = note;
        this.type = type;
        this.status = status;
    }

    public String getId() {
        return id;
    }

    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    public String getAccountNumber() {
        return accountNumber;
    }

    public String getSourceAccountNumber() {
        return sourceAccountNumber;
    }

    public String getDestinationAccountNumber() {
        return destinationAccountNumber;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public String getNote() {
        return note;
    }

    public Type getType() {
        return type;
    }

    public String getStatus() {
        return status;
    }
}
