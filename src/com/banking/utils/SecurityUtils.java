package com.banking.utils;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.util.Base64;
import java.util.logging.Logger;

public class SecurityUtils {
    private static final Logger logger = Logger.getLogger(SecurityUtils.class.getName());

    public static final int MAX_LOGIN_ATTEMPTS = 3;
    private static final int SALT_LENGTH = 16;
    private static final int ITERATIONS = 10000;
    private static final int KEY_LENGTH = 256;
    private static final int MIN_PASSWORD_LENGTH = 8;
    private static final SecureRandom RANDOM = new SecureRandom();

    private SecurityUtils() {}

    public static String generateSalt() {
        byte[] salt = new byte[SALT_LENGTH];
        RANDOM.nextBytes(salt);
        return Base64.getEncoder().encodeToString(salt);
    }

    public static String hashPassword(String password, String salt) {
        try {
            PBEKeySpec spec = new PBEKeySpec(
                    password.toCharArray(),
                    Base64.getDecoder().decode(salt),
                    ITERATIONS,
                    KEY_LENGTH
            );
            SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
            byte[] hash = factory.generateSecret(spec).getEncoded();
            return Base64.getEncoder().encodeToString(hash);
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            logger.severe(String.format("Error hashing password: %s", e.getMessage()));
            throw new BankingException(
                    BankingException.ErrorType.SYSTEM_ERROR,
                    "Error processing password"
            );
        }
    }

    public static boolean verifyPassword(String inputPassword, String storedHash, String salt) {
        String inputHash = hashPassword(inputPassword, salt);
        return storedHash.equals(inputHash);
    }

    public static void validatePassword(String password) {
        if (password == null || password.isEmpty()) {
            throw new BankingException(
                    BankingException.ErrorType.VALIDATION_ERROR,
                    "Password cannot be null or empty"
            );
        }

        if (password.length() < MIN_PASSWORD_LENGTH) {
            throw new BankingException(
                    BankingException.ErrorType.VALIDATION_ERROR,
                    String.format("Password must be at least %d characters long", MIN_PASSWORD_LENGTH)
            );
        }

        if (!password.matches(".*[A-Z].*")) {
            throw new BankingException(
                    BankingException.ErrorType.VALIDATION_ERROR,
                    "Password must contain at least one uppercase letter"
            );
        }

        if (!password.matches(".*[a-z].*")) {
            throw new BankingException(
                    BankingException.ErrorType.VALIDATION_ERROR,
                    "Password must contain at least one lowercase letter"
            );
        }

        if (!password.matches(".*\\d.*")) {
            throw new BankingException(
                    BankingException.ErrorType.VALIDATION_ERROR,
                    "Password must contain at least one number"
            );
        }

        if (!password.matches(".*[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>/?].*")) {
            throw new BankingException(
                    BankingException.ErrorType.VALIDATION_ERROR,
                    "Password must contain at least one special character"
            );
        }
    }
}