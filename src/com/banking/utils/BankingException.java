package com.banking.utils;

public class BankingException extends RuntimeException {
    private final ErrorType errorType;

    public enum ErrorType {
        AUTHENTICATION_ERROR("Authentication Error"),
        ACCOUNT_ERROR("Account Error"),
        TRANSACTION_ERROR("Transaction Error"),
        VALIDATION_ERROR("Validation Error"),
        SYSTEM_ERROR("System Error");

        private final String description;

        ErrorType(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }

    public BankingException(ErrorType errorType, String message) {
        super(message);
        this.errorType = errorType;
    }

    public ErrorType getErrorType() {
        return errorType;
    }
}
