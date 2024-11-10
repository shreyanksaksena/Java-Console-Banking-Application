package com.banking.service;

import com.banking.models.Account;
import com.banking.models.Transaction;
import com.banking.models.User;
import com.banking.utils.BankingException;

import java.time.LocalDateTime;
import java.util.logging.Logger;

public class TransactionService {
    private static final Logger logger = Logger.getLogger(TransactionService.class.getName());

    private static final double MINIMUM_TRANSACTION_AMOUNT = 0.01;
    private static final double MAXIMUM_TRANSACTION_AMOUNT = 1_000_000.00;
    private static final double DAILY_TRANSACTION_LIMIT = 50_000.00;

    private final AccountService accountService;

    public TransactionService(AccountService accountService) {
        if (accountService == null) {
            throw new BankingException(
                    BankingException.ErrorType.SYSTEM_ERROR,
                    "AccountService cannot be null"
            );
        }
        this.accountService = accountService;
        logger.info("TransactionService initialized");
    }

    public void deposit(String accountNumber, double amount, User currentUser) {
        try {
            validateTransactionParameters(accountNumber, amount, currentUser);

            Account account = validateAndGetAccount(accountNumber, currentUser);
            validateDailyTransactionLimit(account, amount);

            account.deposit(amount);

            logger.info(String.format("Deposit successful: Account=%s, Amount=$%.2f, User=%s",
                    accountNumber, amount, currentUser.getUsername()));

        } catch (Exception e) {
            logger.warning(String.format("Deposit failed: Account=%s, Amount=$%.2f, Error=%s",
                    accountNumber, amount, e.getMessage()));
            throw e;
        }
    }

    public void withdraw(String accountNumber, double amount, User currentUser) {
        try {
            validateTransactionParameters(accountNumber, amount, currentUser);

            Account account = validateAndGetAccount(accountNumber, currentUser);
            validateDailyTransactionLimit(account, amount);

            account.withdraw(amount);

            logger.info(String.format("Withdrawal successful: Account=%s, Amount=$%.2f, User=%s",
                    accountNumber, amount, currentUser.getUsername()));

        } catch (Exception e) {
            logger.warning(String.format("Withdrawal failed: Account=%s, Amount=$%.2f, Error=%s",
                    accountNumber, amount, e.getMessage()));
            throw e;
        }
    }

    private void validateTransactionParameters(String accountNumber, double amount, User currentUser) {
        if (accountNumber == null || accountNumber.trim().isEmpty()) {
            throw new BankingException(
                    BankingException.ErrorType.VALIDATION_ERROR,
                    "Account number cannot be null or empty"
            );
        }

        if (currentUser == null) {
            throw new BankingException(
                    BankingException.ErrorType.AUTHENTICATION_ERROR,
                    "User cannot be null"
            );
        }

        validateTransactionAmount(amount);
    }

    private void validateTransactionAmount(double amount) {
        if (amount < MINIMUM_TRANSACTION_AMOUNT) {
            throw new BankingException(
                    BankingException.ErrorType.VALIDATION_ERROR,
                    String.format("Transaction amount must be at least $%.2f", MINIMUM_TRANSACTION_AMOUNT)
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

    private Account validateAndGetAccount(String accountNumber, User currentUser) {
        Account account = accountService.getAccount(accountNumber);

        if (!account.getAccountHolder().getUserId().equals(currentUser.getUserId())) {
            logger.warning(String.format("Unauthorized account access attempt: User=%s, Account=%s",
                    currentUser.getUsername(), accountNumber));
            throw new BankingException(
                    BankingException.ErrorType.AUTHENTICATION_ERROR,
                    "You don't have access to this account"
            );
        }

        return account;
    }

    private void validateDailyTransactionLimit(Account account, double amount) {
        LocalDateTime startOfDay = LocalDateTime.now().withHour(0).withMinute(0).withSecond(0);
        LocalDateTime endOfDay = LocalDateTime.now().withHour(23).withMinute(59).withSecond(59);

        double dailyTotal = account.getTransactionsByDate(startOfDay, endOfDay).stream()
                .mapToDouble(Transaction::getAmount)
                .sum();

        if (dailyTotal + amount > DAILY_TRANSACTION_LIMIT) {
            throw new BankingException(
                    BankingException.ErrorType.VALIDATION_ERROR,
                    String.format("Transaction would exceed daily limit of $%.2f", DAILY_TRANSACTION_LIMIT)
            );
        }
    }
}