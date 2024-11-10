package com.banking.service;

import com.banking.models.Account;
import com.banking.models.Transaction;
import com.banking.models.User;
import com.banking.utils.BankingException;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

public class StatementService {
    private final AccountService accountService;
    private static final DateTimeFormatter DATE_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final String SEPARATOR =
            "\n--------------------------------------------------\n";

    public StatementService(AccountService accountService) {
        this.accountService = accountService;
    }

    public String generateDetailedStatement(String accountNumber, User currentUser) {
        Account account = validateAndGetAccount(accountNumber, currentUser);
        List<Transaction> transactions = accountService.getTransactionHistory(accountNumber, currentUser);

        StringBuilder statement = new StringBuilder();
        appendStatementHeader(statement, account);

        if (transactions.isEmpty()) {
            statement.append("\nNo transactions found for this period.");
            return statement.toString();
        }

        appendTransactionDetails(statement, transactions);
        appendStatementSummary(statement, transactions, account.getAccountType());

        return statement.toString();
    }

    private Account validateAndGetAccount(String accountNumber, User currentUser) {
        Account account = accountService.getAccount(accountNumber);

        if (!account.getAccountHolder().getUserId().equals(currentUser.getUserId())) {
            throw new BankingException(
                    BankingException.ErrorType.AUTHENTICATION_ERROR,
                    "You don't have access to this account"
            );
        }

        return account;
    }

    private void appendStatementHeader(StringBuilder statement, Account account) {
        statement.append("\n============================================")
                .append("\n           ACCOUNT STATEMENT                ")
                .append("\n============================================")
                .append("\nAccount Details:")
                .append("\nAccount Number: ").append(account.getAccountNumber())
                .append("\nAccount Type: ").append(account.getAccountType())
                .append("\nCurrent Balance: $").append(String.format("%.2f", account.getBalance()))
                .append(SEPARATOR);
    }

    private void appendTransactionDetails(StringBuilder statement, List<Transaction> transactions) {
        statement.append("\nTransaction History:");
        statement.append(SEPARATOR);

        for (Transaction transaction : transactions) {
            statement.append(String.format("%s | %-10s | $%8.2f | Balance: $%8.2f%n",
                    transaction.getTimestamp().format(DATE_FORMATTER),
                    transaction.getType(),
                    transaction.getAmount(),
                    transaction.getBalanceAfter()));
        }
        statement.append(SEPARATOR);
    }

    private void appendStatementSummary(StringBuilder statement, List<Transaction> transactions, String accountType) {
        double totalDeposits = calculateTotalByType(transactions, "DEPOSIT");
        double totalWithdrawals = calculateTotalByType(transactions, "WITHDRAWAL");

        statement.append("\nStatement Summary:")
                .append("\nTotal Deposits: $").append(String.format("%.2f", totalDeposits))
                .append("\nTotal Withdrawals: $").append(String.format("%.2f", totalWithdrawals))
                .append("\nNumber of Transactions: ").append(transactions.size());

        if (accountType.equals("SAVINGS")) {
            double totalInterest = calculateTotalByType(transactions, "INTEREST");
            statement.append("\nTotal Interest Earned: $").append(String.format("%.2f", totalInterest));
        }

        statement.append(SEPARATOR);
    }

    private double calculateTotalByType(List<Transaction> transactions, String type) {
        return transactions.stream()
                .filter(t -> t.getType().equals(type))
                .mapToDouble(Transaction::getAmount)
                .sum();
    }

    public String generateMonthlyStatement(String accountNumber, User currentUser,
                                           LocalDateTime startDate, LocalDateTime endDate) {
        Account account = validateAndGetAccount(accountNumber, currentUser);
        List<Transaction> allTransactions = accountService.getTransactionHistory(accountNumber, currentUser);

        // Filtering transactions for the specified date range

        List<Transaction> periodTransactions = allTransactions.stream()
                .filter(t -> isWithinDateRange(t.getTimestamp(), startDate, endDate))
                .collect(Collectors.toList());

        StringBuilder statement = new StringBuilder();
        appendMonthlyStatementHeader(statement, account, startDate, endDate);

        if (periodTransactions.isEmpty()) {
            statement.append("\nNo transactions found for this period.");
            return statement.toString();
        }

        appendTransactionDetails(statement, periodTransactions);
        appendStatementSummary(statement, periodTransactions, account.getAccountType());

        return statement.toString();
    }

    private void appendMonthlyStatementHeader(StringBuilder statement, Account account,
                                              LocalDateTime startDate, LocalDateTime endDate) {
        statement.append("\n============================================")
                .append("\n           MONTHLY STATEMENT                ")
                .append("\n============================================")
                .append("\nPeriod: ")
                .append(startDate.format(DATE_FORMATTER))
                .append(" to ")
                .append(endDate.format(DATE_FORMATTER))
                .append("\n\nAccount Details:")
                .append("\nAccount Number: ").append(account.getAccountNumber())
                .append("\nAccount Type: ").append(account.getAccountType())
                .append("\nCurrent Balance: $").append(String.format("%.2f", account.getBalance()))
                .append(SEPARATOR);
    }

    private boolean isWithinDateRange(LocalDateTime timestamp,
                                      LocalDateTime startDate,
                                      LocalDateTime endDate) {
        return !timestamp.isBefore(startDate) && !timestamp.isAfter(endDate);
    }
}