package com.banking.service;

import com.banking.models.Account;
import com.banking.models.Transaction;
import com.banking.models.User;
import com.banking.utils.BankingException;
import com.banking.utils.ValidationUtils;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

public class AccountService {
    private static final Logger logger = Logger.getLogger(AccountService.class.getName());
    private static final double MINIMUM_INITIAL_DEPOSIT = 500.00;
    private static final int ACCOUNT_NUMBER_LENGTH = 10;
    private static final int MAX_ACCOUNTS_PER_USER = 5;
    private final Map<String, Account> accountsByNumber;
    private final SecureRandom random;

    public AccountService() {
        this.accountsByNumber = new ConcurrentHashMap<>();
        this.random = new SecureRandom();
        logger.info("AccountService initialized");
    }

    public Account createAccount(User owner, String accountType, double initialDeposit) {
        try {
            validateAccountCreation(owner, accountType, initialDeposit);

            String accountNumber = generateAccountNumber();
            Account newAccount = new Account(accountNumber, accountType, owner, initialDeposit);

            accountsByNumber.put(accountNumber, newAccount);
            owner.addAccount(newAccount);

            logger.info(String.format("Account created: Number=%s, Type=%s, Owner=%s, Initial Deposit=$%.2f",
                    accountNumber, accountType, owner.getUsername(), initialDeposit));

            return newAccount;

        } catch (Exception e) {
            logger.severe(String.format("Failed to create account: Owner=%s, Type=%s, Error=%s",
                    owner.getUsername(), accountType, e.getMessage()));
            throw e;
        }
    }

    private void validateAccountCreation(User owner, String accountType, double initialDeposit) {
        if (owner == null) {
            throw new BankingException(
                    BankingException.ErrorType.VALIDATION_ERROR,
                    "Account owner cannot be null"
            );
        }

        ValidationUtils.validateAccountType(accountType);

        if (initialDeposit < MINIMUM_INITIAL_DEPOSIT) {
            throw new BankingException(
                    BankingException.ErrorType.VALIDATION_ERROR,
                    String.format("Initial deposit must be at least $%.2f", MINIMUM_INITIAL_DEPOSIT)
            );
        }

        if (getAccountsByUser(owner).size() >= MAX_ACCOUNTS_PER_USER) {
            throw new BankingException(
                    BankingException.ErrorType.VALIDATION_ERROR,
                    String.format("User cannot have more than %d accounts", MAX_ACCOUNTS_PER_USER)
            );
        }
    }

    public List<Transaction> getTransactionHistory(String accountNumber, User currentUser) {
        Account account = validateAndGetAccount(accountNumber, currentUser);
        return account.getTransactions();
    }
    public void calculateInterestForAllSavingsAccounts() {
        for (Account account : accountsByNumber.values()) {
            if (account.getAccountType().equals("SAVINGS")) {
                account.calculateMonthlyInterest();
            }
        }
    }

    private String generateAccountNumber() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < ACCOUNT_NUMBER_LENGTH; i++) {
            sb.append(random.nextInt(10));
        }
        String accountNumber = sb.toString();

        while (accountsByNumber.containsKey(accountNumber)) {
            accountNumber = generateAccountNumber();
        }

        return accountNumber;
    }

    public Account getAccount(String accountNumber) {
        if (accountNumber == null || accountNumber.trim().isEmpty()) {
            throw new BankingException(
                    BankingException.ErrorType.VALIDATION_ERROR,
                    "Account number cannot be null or empty"
            );
        }

        Account account = accountsByNumber.get(accountNumber.trim());
        if (account == null) {
            throw new BankingException(
                    BankingException.ErrorType.ACCOUNT_ERROR,
                    String.format("Account not found: %s", accountNumber)
            );
        }

        return account;
    }

    public List<Account> getAccountsByUser(User user) {
        if (user == null) {
            throw new BankingException(
                    BankingException.ErrorType.VALIDATION_ERROR,
                    "User cannot be null"
            );
        }
        return new ArrayList<>(user.getAccounts());
    }

    private Account validateAndGetAccount(String accountNumber, User currentUser) {
        Account account = getAccount(accountNumber);

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
}