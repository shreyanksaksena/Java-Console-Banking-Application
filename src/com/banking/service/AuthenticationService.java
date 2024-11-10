package com.banking.service;

import com.banking.models.User;
import com.banking.utils.ValidationUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Logger;

public class AuthenticationService {
    private static final Logger logger = Logger.getLogger(AuthenticationService.class.getName());
    private final Map<String, User> usersByUsername;
    private User currentUser;

    // Log message templates fixed so that it shows up rather than with the input

    private static final String LOG_REGISTRATION_FAILED = """
            Registration failed: Username already exists - %s""";
    private static final String LOG_NEW_USER = """
            New user registered: %s""";
    private static final String LOG_LOGIN_FAILED_USER = """
            Login failed: User not found - %s""";
    private static final String LOG_LOGIN_FAILED_PASSWORD = """
            Login failed: Invalid password for user - %s""";
    private static final String LOG_LOGIN_SUCCESS = """
            User logged in successfully: %s""";
    private static final String LOG_LOGOUT = """
            User logged out: %s""";

    public AuthenticationService() {
        this.usersByUsername = new HashMap<>();
        this.currentUser = null;
        logger.info("AuthenticationService initialized");
    }

    public void registerUser(String username, String password) throws IllegalArgumentException {

        ValidationUtils.validateUsername(username);
        ValidationUtils.validatePassword(password);

        if (usersByUsername.containsKey(username)) {
            logger.warning(LOG_REGISTRATION_FAILED.formatted(username));
            throw new IllegalArgumentException("Username already exists");
        }

        String userId = UUID.randomUUID().toString();
        User newUser = new User(userId, username, password);
        usersByUsername.put(username, newUser);
        logger.info(LOG_NEW_USER.formatted(username));
    }

    public void login(String username, String password) throws IllegalArgumentException {
        User user = usersByUsername.get(username);
        if (user == null) {
            logger.warning(LOG_LOGIN_FAILED_USER.formatted(username));
            throw new IllegalArgumentException("User not found");
        }

        if (!user.validatePassword(password)) {
            logger.warning(LOG_LOGIN_FAILED_PASSWORD.formatted(username));
            throw new IllegalArgumentException("Invalid password");
        }

        this.currentUser = user;
        logger.info(LOG_LOGIN_SUCCESS.formatted(username));
    }

    public void logout() {
        if (currentUser != null) {
            logger.info(LOG_LOGOUT.formatted(currentUser.getUsername()));
            this.currentUser = null;
        }
    }

    public User getCurrentUser() {
        return currentUser;
    }

    public boolean isLoggedIn() {
        return currentUser != null;
    }
}