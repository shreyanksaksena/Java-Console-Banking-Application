package com.banking.models;

import com.banking.utils.BankingException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Objects;
import java.util.UUID;
import java.util.logging.Logger;

public class Transaction {
    private static final Logger logger = Logger.getLogger(Transaction.class.getName());

    private static final int SCALE = 2;
    private static final DateTimeFormatter TIMESTAMP_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final String transactionId;
    private final LocalDateTime timestamp;
    private final String type;
    private final BigDecimal amount;
    private final BigDecimal balanceAfter;
    private final String description;

    public Transaction(String accountNumber, String type, double amount, double balanceAfter) {
        validateTransactionData(accountNumber, type, amount, balanceAfter);

        this.transactionId = UUID.randomUUID().toString();
        this.type = type;
        this.amount = BigDecimal.valueOf(amount).setScale(SCALE, RoundingMode.HALF_UP);
        this.balanceAfter = BigDecimal.valueOf(balanceAfter).setScale(SCALE, RoundingMode.HALF_UP);
        this.timestamp = LocalDateTime.now();
        this.description = getDefaultDescription();

        logger.fine(String.format("Transaction created: ID=%s, Account=%s, Type=%s, Amount=$%.2f",
                transactionId, accountNumber, type, amount));
    }

    private void validateTransactionData(String accountNumber, String type,
                                         double amount, double balanceAfter) {
        if (accountNumber == null || accountNumber.trim().isEmpty()) {
            throw new BankingException(
                    BankingException.ErrorType.VALIDATION_ERROR,
                    "Account number cannot be null or empty"
            );
        }

        if (type == null || type.trim().isEmpty()) {
            throw new BankingException(
                    BankingException.ErrorType.VALIDATION_ERROR,
                    "Transaction type cannot be null or empty"
            );
        }

        if (!isValidTransactionType(type)) {
            throw new BankingException(
                    BankingException.ErrorType.VALIDATION_ERROR,
                    String.format("Invalid transaction type: %s", type)
            );
        }

        if (amount < 0) {
            throw new BankingException(
                    BankingException.ErrorType.VALIDATION_ERROR,
                    "Transaction amount cannot be negative"
            );
        }

        if (Double.isInfinite(amount) || Double.isNaN(amount) ||
                Double.isInfinite(balanceAfter) || Double.isNaN(balanceAfter)) {
            throw new BankingException(
                    BankingException.ErrorType.VALIDATION_ERROR,
                    "Invalid numerical values in transaction"
            );
        }
    }

    private boolean isValidTransactionType(String type) {
        return type.equals(Types.DEPOSIT) ||
                type.equals(Types.WITHDRAWAL) ||
                type.equals(Types.INTEREST);
    }

    private String getDefaultDescription() {
        return switch (type) {
            case Types.DEPOSIT -> "Deposit transaction";
            case Types.WITHDRAWAL -> "Withdrawal transaction";
            case Types.INTEREST -> "Interest credit";
            default -> "Transaction";
        };
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public String getType() {
        return type;
    }

    public double getAmount() {
        return amount.doubleValue();
    }

    public double getBalanceAfter() {
        return balanceAfter.doubleValue();
    }

    public String getFormattedTimestamp() {
        return timestamp.format(TIMESTAMP_FORMATTER);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Transaction that = (Transaction) o;
        return Objects.equals(transactionId, that.transactionId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(transactionId);
    }

    @Override
    public String toString() {
        return String.format("Transaction[%s] %s - Amount: $%.2f, Balance: $%.2f, Time: %s, Desc: %s",
                transactionId.substring(0, 8),
                type,
                amount,
                balanceAfter,
                getFormattedTimestamp(),
                description);
    }

    public static final class Types {
        public static final String DEPOSIT = "DEPOSIT";
        public static final String WITHDRAWAL = "WITHDRAWAL";
        public static final String INTEREST = "INTEREST";

        private Types() {}
    }
}