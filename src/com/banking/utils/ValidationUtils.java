package com.banking.utils;

public class ValidationUtils {
    public static void validateUsername(String username) {
        if (username == null || username.trim().length() < 3) {
            throw new IllegalArgumentException("Username must be at least 3 characters long");
        }
        if (!username.matches("^[a-zA-Z0-9_]+$")) {
            throw new IllegalArgumentException("Username can only contain letters, numbers, and underscores");
        }
    }

    public static void validatePassword(String password) {
        if (password == null || password.length() < 6) {
            throw new IllegalArgumentException("Password must be at least 6 characters long");
        }
    }

    public static void validateAccountType(String accountType) {
        String type = accountType.toUpperCase();
        if (!type.equals("SAVINGS") && !type.equals("CHECKING")) {
            throw new IllegalArgumentException("Account type must be either SAVINGS or CHECKING");
        }
    }
}