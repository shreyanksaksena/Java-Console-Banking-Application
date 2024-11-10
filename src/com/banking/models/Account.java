package com.banking.models;

import com.banking.utils.BankingException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.logging.Logger;

public class Account {
    private static final Logger logger = Logger.getLogger(Account.class.getName());

    private static final double MINIMUM_BALANCE = 500.00;
    private static final double MAXIMUM_TRANSACTION_AMOUNT = 1_000_000.00;
    private static final double SAVINGS_INTEREST_RATE = 0.045;
    private static final int SCALE = 2;

    private final String accountNumber;
    private final String accountType;
    private final User accountHolder;
    private BigDecimal balance;
    private final List<Transaction> transactions;

    public Account(String accountNumber, String accountType, User accountHolder, double initialBalance) {
        validateAccountCreation(accountNumber, accountType, accountHolder, initialBalance);

        this.accountNumber = accountNumber;
        this.accountType = accountType.toUpperCase();
        this.accountHolder = accountHolder;
        this.balance = BigDecimal.valueOf(initialBalance).setScale(SCALE, RoundingMode.HALF_UP);
        this.transactions = new ArrayList<>();

        // Recording initial deposit here

        addTransaction(Transaction.Types.DEPOSIT, initialBalance);
        logger.info(String.format("Account created: %s, Type: %s, Initial Balance: $%.2f",
                accountNumber, accountType, initialBalance));
    }

    private void validateAccountCreation(String accountNumber, String accountType,
                                         User accountHolder, double initialBalance) {
        if (accountNumber == null || accountNumber.trim().isEmpty()) {
            throw new BankingException(
                    BankingException.ErrorType.VALIDATION_ERROR,
                    "Account number cannot be null or empty"
            );
        }

        if (accountType == null || accountType.trim().isEmpty()) {
            throw new BankingException(
                    BankingException.ErrorType.VALIDATION_ERROR,
                    "Account type cannot be null or empty"
            );
        }

        if (accountHolder == null) {
            throw new BankingException(
                    BankingException.ErrorType.VALIDATION_ERROR,
                    "Account holder cannot be null"
            );
        }

        if (initialBalance < MINIMUM_BALANCE) {
            throw new BankingException(
                    BankingException.ErrorType.VALIDATION_ERROR,
                    String.format("Initial balance must be at least $%.2f", MINIMUM_BALANCE)
            );
        }
    }

    public String getAccountNumber() {
        return accountNumber;
    }

    public String getAccountType() {
        return accountType;
    }

    public User getAccountHolder() {
        return accountHolder;
    }

    public double getBalance() {
        return balance.doubleValue();
    }

    public List<Transaction> getTransactions() {
        return Collections.unmodifiableList(transactions);
    }

    public void deposit(double amount) {
        try {
            validateTransactionAmount(amount);
            BigDecimal depositAmount = BigDecimal.valueOf(amount).setScale(SCALE, RoundingMode.HALF_UP);
            this.balance = this.balance.add(depositAmount);
            addTransaction(Transaction.Types.DEPOSIT, amount);

            logger.info(String.format("Deposit successful: Account=%s, Amount=$%.2f, New Balance=$%.2f",
                    accountNumber, amount, balance));

        } catch (Exception e) {
            logger.warning(String.format("Deposit failed: Account=%s, Amount=$%.2f, Error=%s",
                    accountNumber, amount, e.getMessage()));
            throw e;
        }
    }

    public void withdraw(double amount) {
        try {
            validateTransactionAmount(amount);
            validateWithdrawal(amount);

            BigDecimal withdrawalAmount = BigDecimal.valueOf(amount).setScale(SCALE, RoundingMode.HALF_UP);
            this.balance = this.balance.subtract(withdrawalAmount);
            addTransaction(Transaction.Types.WITHDRAWAL, amount);

            logger.info(String.format("Withdrawal successful: Account=%s, Amount=$%.2f, New Balance=$%.2f",
                    accountNumber, amount, balance));

        } catch (Exception e) {
            logger.warning(String.format("Withdrawal failed: Account=%s, Amount=$%.2f, Error=%s",
                    accountNumber, amount, e.getMessage()));
            throw e;
        }
    }

    private void validateTransactionAmount(double amount) {
        if (amount <= 0) {
            throw new BankingException(
                    BankingException.ErrorType.VALIDATION_ERROR,
                    "Transaction amount must be positive"
            );
        }

        if (amount > MAXIMUM_TRANSACTION_AMOUNT) {
            throw new BankingException(
                    BankingException.ErrorType.VALIDATION_ERROR,
                    String.format("Transaction amount cannot exceed $%.2f", MAXIMUM_TRANSACTION_AMOUNT)
            );
        }

        if (Double.isInfinite(amount) || Double.isNaN(amount)) {
            throw new BankingException(
                    BankingException.ErrorType.VALIDATION_ERROR,
                    "Invalid transaction amount"
            );
        }
    }

    private void validateWithdrawal(double amount) {
        BigDecimal withdrawalAmount = BigDecimal.valueOf(amount);
        BigDecimal projectedBalance = balance.subtract(withdrawalAmount);

        if (projectedBalance.compareTo(BigDecimal.valueOf(MINIMUM_BALANCE)) < 0) {
            throw new BankingException(
                    BankingException.ErrorType.TRANSACTION_ERROR,
                    String.format("Withdrawal would put account below minimum balance of $%.2f",
                            MINIMUM_BALANCE)
            );
        }
    }

    public void calculateMonthlyInterest() {
        if (!accountType.equals("SAVINGS") || balance.compareTo(BigDecimal.ZERO) <= 0) {
            return;
        }

        try {
            BigDecimal monthlyRate = BigDecimal.valueOf(SAVINGS_INTEREST_RATE / 12);
            BigDecimal interest = balance.multiply(monthlyRate)
                    .setScale(SCALE, RoundingMode.HALF_UP);

            if (interest.compareTo(BigDecimal.ZERO) > 0) {
                this.balance = this.balance.add(interest);
                addTransaction(Transaction.Types.INTEREST, interest.doubleValue());

                logger.info(String.format("Interest applied: Account=%s, Amount=$%.2f, New Balance=$%.2f",
                        accountNumber, interest, balance));
            }

        } catch (Exception e) {
            logger.warning(String.format("Interest calculation failed: Account=%s, Error=%s",
                    accountNumber, e.getMessage()));
            throw new BankingException(
                    BankingException.ErrorType.SYSTEM_ERROR,
                    String.format("Failed to calculate interest: %s", e.getMessage())
            );

        }
    }

    private void addTransaction(String type, double amount) {
        Transaction transaction = new Transaction(
                accountNumber,
                type,
                amount,
                balance.doubleValue()
        );
        this.transactions.add(transaction);
    }

    public List<Transaction> getTransactionsByDate(LocalDateTime startDate, LocalDateTime endDate) {
        if (startDate == null || endDate == null) {
            throw new BankingException(
                    BankingException.ErrorType.VALIDATION_ERROR,
                    "Start and end dates cannot be null"
            );
        }

        if (startDate.isAfter(endDate)) {
            throw new BankingException(
                    BankingException.ErrorType.VALIDATION_ERROR,
                    "Start date cannot be after end date"
            );
        }

        return transactions.stream()
                .filter(t -> !t.getTimestamp().isBefore(startDate) && !t.getTimestamp().isAfter(endDate))
                .collect(Collectors.toList());
    }

    @Override
    public String toString() {
        return String.format("Account[number=%s, type=%s, balance=$%.2f]",
                accountNumber, accountType, balance);
    }
}