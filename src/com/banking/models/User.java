package com.banking.models;

import com.banking.utils.BankingException;
import com.banking.utils.SecurityUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.logging.Logger;

public class User {
    private static final Logger logger = Logger.getLogger(User.class.getName());
    private static final int MIN_USERNAME_LENGTH = 3;
    private static final int MAX_USERNAME_LENGTH = 30;
    private static final int MAX_ACCOUNTS = 5;
    private final String userId;
    private final String username;
    private final String passwordHash;
    private final String salt;
    private final List<Account> accounts;
    private final LocalDateTime createdAt;
    private LocalDateTime lastLoginAt;
    private boolean isLocked;
    private int failedLoginAttempts;

    public User(String userId, String username, String password) {
        validateUserData(userId, username, password);

        this.userId = userId;
        this.username = username.trim();
        this.salt = SecurityUtils.generateSalt();
        this.passwordHash = SecurityUtils.hashPassword(password, this.salt);
        this.accounts = new ArrayList<>();
        this.createdAt = LocalDateTime.now();
        this.lastLoginAt = null;
        this.isLocked = false;
        this.failedLoginAttempts = 0;

        logger.info(String.format("User created: ID=%s, Username=%s", userId, username));
    }

    private void validateUserData(String userId, String username, String password) {
        validateUserId(userId);
        validateUsername(username);
        SecurityUtils.validatePassword(password);
    }

    private void validateUserId(String userId) {
        if (userId == null || userId.trim().isEmpty()) {
            throw new BankingException(
                    BankingException.ErrorType.VALIDATION_ERROR,
                    "User ID cannot be null or empty"
            );
        }
    }

    private void validateUsername(String username) {
        if (username == null || username.trim().isEmpty()) {
            throw new BankingException(
                    BankingException.ErrorType.VALIDATION_ERROR,
                    "Username cannot be null or empty"
            );
        }

        String trimmedUsername = username.trim();
        validateUsernameLength(trimmedUsername);
        validateUsernameCharacters(trimmedUsername);
    }

    private void validateUsernameLength(String username) {
        if (username.length() < MIN_USERNAME_LENGTH) {
            throw new BankingException(
                    BankingException.ErrorType.VALIDATION_ERROR,
                    String.format("Username must be at least %d characters long", MIN_USERNAME_LENGTH)
            );
        }

        if (username.length() > MAX_USERNAME_LENGTH) {
            throw new BankingException(
                    BankingException.ErrorType.VALIDATION_ERROR,
                    String.format("Username cannot exceed %d characters", MAX_USERNAME_LENGTH)
            );
        }
    }

    private void validateUsernameCharacters(String username) {
        if (!username.matches("^[a-zA-Z0-9_]+$")) {
            throw new BankingException(
                    BankingException.ErrorType.VALIDATION_ERROR,
                    "Username can only contain letters, numbers, and underscores"
            );
        }
    }

    public String getUserId() {
        return userId;
    }

    public String getUsername() {
        return username;
    }

    public List<Account> getAccounts() {
        return Collections.unmodifiableList(accounts);
    }

    // Account management
    public void addAccount(Account account) {
        validateNewAccount(account);
        accounts.add(account);
        logger.info(String.format("Account added to user %s: Account=%s, Type=%s",
                username, account.getAccountNumber(), account.getAccountType()));
    }

    private void validateNewAccount(Account account) {
        if (account == null) {
            throw new BankingException(
                    BankingException.ErrorType.VALIDATION_ERROR,
                    "Account cannot be null"
            );
        }

        if (!account.getAccountHolder().getUserId().equals(this.userId)) {
            throw new BankingException(
                    BankingException.ErrorType.VALIDATION_ERROR,
                    "Account holder does not match user"
            );
        }

        if (accounts.size() >= MAX_ACCOUNTS) {
            throw new BankingException(
                    BankingException.ErrorType.VALIDATION_ERROR,
                    String.format("User cannot have more than %d accounts", MAX_ACCOUNTS)
            );
        }

        if (accounts.stream().anyMatch(a -> a.getAccountNumber().equals(account.getAccountNumber()))) {
            throw new BankingException(
                    BankingException.ErrorType.VALIDATION_ERROR,
                    "Account already exists for this user"
            );
        }
    }

    // Authentication methods here
    public boolean validatePassword(String inputPassword) {
        if (isLocked) {
            throw new BankingException(
                    BankingException.ErrorType.AUTHENTICATION_ERROR,
                    "Account is locked due to too many failed login attempts"
            );
        }

        boolean isValid = SecurityUtils.verifyPassword(inputPassword, this.passwordHash, this.salt);

        if (!isValid) {
            handleFailedLogin();
            return false;
        }

        resetFailedLoginAttempts();
        updateLastLoginTime();
        return true;
    }

    private void handleFailedLogin() {
        failedLoginAttempts++;
        if (failedLoginAttempts >= SecurityUtils.MAX_LOGIN_ATTEMPTS) {
            isLocked = true;
            logger.warning(String.format("User account locked: %s (Too many failed login attempts)", username));
            throw new BankingException(
                    BankingException.ErrorType.AUTHENTICATION_ERROR,
                    "Account has been locked due to too many failed login attempts"
            );
        }
        logger.warning(String.format("Failed login attempt for user %s (%d/%d attempts)",
                username, failedLoginAttempts, SecurityUtils.MAX_LOGIN_ATTEMPTS));
    }

    private void resetFailedLoginAttempts() {
        failedLoginAttempts = 0;
        isLocked = false;
    }

    private void updateLastLoginTime() {
        lastLoginAt = LocalDateTime.now();
        logger.info(String.format("Successful login: User=%s, Time=%s", username, lastLoginAt));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        User user = (User) o;
        return Objects.equals(userId, user.userId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(userId);
    }

    @Override
    public String toString() {
        return String.format("User[id=%s, username=%s, accounts=%d, created=%s]",
                userId,
                username,
                accounts.size(),
                createdAt);
    }
}