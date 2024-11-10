package com.banking;

import com.banking.models.Account;
import com.banking.models.Transaction;
import com.banking.service.*;
import com.banking.utils.BankingException;
import com.banking.utils.ConsoleUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Scanner;
import java.util.logging.Logger;
import java.util.logging.Level;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;
import java.util.logging.Level;
import java.util.logging.Handler;
import java.util.logging.ConsoleHandler;
import java.util.logging.SimpleFormatter;
import java.util.logging.LogRecord;
import java.io.PrintWriter;
import java.io.StringWriter;

public class Main {
    private static final Logger logger = Logger.getLogger(Main.class.getName());
    private static AuthenticationService authService;
    private static AccountService accountService;
    private static TransactionService transactionService;
    private static StatementService statementService;
    private static Scanner scanner;
    private static ScheduledExecutorService scheduler;

    private static final double MINIMUM_DEPOSIT = 500.0;
    private static final String EXIT_COMMAND = "exit";

    public static void main(String[] args) {
        setupLogging();
        initializeServices();

        while (true) {
            try {
                ConsoleUtils.clearScreen();

                if (authService.isLoggedIn()) {
                    showMainMenu();
                } else {
                    showAuthenticationMenu();
                }
            } catch (Exception e) {
                handleUnexpectedError(e);
            }
        }
    }

    private static void setupLogging() {
        Logger rootLogger = Logger.getLogger("");
        for (Handler handler : rootLogger.getHandlers()) {
            rootLogger.removeHandler(handler);
        }

        ConsoleHandler handler = new ConsoleHandler();
        handler.setFormatter(new SimpleFormatter() {
            private static final String format = "[%1$tF %1$tT] [%2$s] %3$s %n";

            @Override
            public String format(LogRecord record) {
                String throwable = "";
                if (record.getThrown() != null) {
                    StringWriter sw = new StringWriter();
                    PrintWriter pw = new PrintWriter(sw);
                    record.getThrown().printStackTrace(pw);
                    pw.close();
                    throwable = sw.toString();
                }
                return String.format(format,
                        new java.util.Date(record.getMillis()),
                        record.getLevel().getLocalizedName(),
                        record.getMessage() + throwable);
            }
        });

        rootLogger.addHandler(handler);
        rootLogger.setLevel(Level.INFO);

        System.setErr(new java.io.PrintStream(System.err) {
            public void println(String x) {
                logger.severe(x);
            }
        });
    }

    private static void runMainLoop() {
        while (true) {
            try {
                if (!authService.isLoggedIn()) {
                    showAuthenticationMenu();
                } else {
                    showMainMenu();
                }
            } catch (Exception e) {
                handleUnexpectedError(e);
            }
        }
    }

    private static void initializeServices() {
        try {
            logger.info("Initializing services...");

            ConsoleUtils.clearScreen();
            authService = new AuthenticationService();
            logger.info("AuthenticationService initialized");

            accountService = new AccountService();
            logger.info("AccountService initialized");

            transactionService = new TransactionService(accountService);
            logger.info("TransactionService initialized");

            statementService = new StatementService(accountService);
            logger.info("StatementService initialized");

            scanner = new Scanner(System.in);
            logger.info("Services initialized successfully");

        } catch (Exception e) {
            handleFatalError(e);
        }
    }
    private static void startInterestCalculationScheduler() {
        scheduler = Executors.newScheduledThreadPool(1);

        scheduler.scheduleAtFixedRate(
                () -> {
                    try {
                        System.out.println("Calculating interest for all savings accounts...");
                        accountService.calculateInterestForAllSavingsAccounts();
                        System.out.println("Interest calculation completed.");
                    } catch (Exception e) {
                        System.err.println("Error calculating interest: " + e.getMessage());
                    }
                },
                getInitialDelay(),
                TimeUnit.DAYS.toMillis(1),
                TimeUnit.MILLISECONDS
        );
    }
    private static long getInitialDelay() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime nextRun = now.withHour(0).withMinute(0).withSecond(0).withNano(0).plusDays(1);
        return TimeUnit.SECONDS.toMillis(java.time.Duration.between(now, nextRun).getSeconds());
    }

    private static void showAuthenticationMenu() {
        try {
            ConsoleUtils.clearScreen();
            System.out.println("\n=== Banking System ===");
            System.out.println("1. Login");
            System.out.println("2. Register");
            System.out.println("3. Exit");
            System.out.print("Choose an option (or type 'exit' to quit): ");

            String input = scanner.nextLine().trim().toLowerCase();

            if (input.equals(EXIT_COMMAND)) {
                handleExit();
            }

            int choice = validateMenuChoice(input, 1, 3);
            processAuthenticationChoice(choice);

        } catch (NumberFormatException e) {
            System.out.println("Please enter a valid number.");
            ConsoleUtils.pressEnterToContinue();
        } catch (BankingException e) {
            handleBankingError(e);
        }
    }

    private static void processAuthenticationChoice(int choice) {
        try {
            switch (choice) {
                case 1 -> handleLogin();
                case 2 -> handleRegistration();
                case 3 -> handleExit();
                default -> throw new BankingException(
                        BankingException.ErrorType.VALIDATION_ERROR,
                        "Invalid option selected."
                );
            }
        } catch (BankingException e) {
            handleBankingError(e);
        } catch (Exception e) {
            handleUnexpectedError(e);
        }
    }

    private static void showMainMenu() {
        try {
            ConsoleUtils.printHeader("MAIN MENU");
            System.out.println("\nLogged in as: " + authService.getCurrentUser().getUsername());
            ConsoleUtils.printDivider();

            displayMainMenuOptions();
            String input = scanner.nextLine().trim().toLowerCase();

            if (input.equals(EXIT_COMMAND)) {
                handleLogout();
                return;
            }

            int choice = validateMenuChoice(input, 1, 7);
            processMainMenuChoice(choice);

        } catch (NumberFormatException e) {
            handleBankingError(new BankingException(
                    BankingException.ErrorType.VALIDATION_ERROR,
                    "Please enter a valid number."
            ));
        } catch (BankingException e) {
            handleBankingError(e);
        } catch (Exception e) {
            handleUnexpectedError(e);
        }
    }

    private static void displayMainMenuOptions() {
        System.out.println("\n1. Create Account");
        System.out.println("2. View Accounts");
        System.out.println("3. Make Transaction");
        System.out.println("4. View Complete Statement");
        System.out.println("5. View Monthly Statement");
        System.out.println("6. Check Balance");
        System.out.println("7. Logout");
        System.out.print("\nChoose an option (or type 'exit' to logout): ");
    }

    private static void processMainMenuChoice(int choice) {
        try {
            switch (choice) {
                case 1 -> handleCreateAccount();
                case 2 -> handleViewAccounts();
                case 3 -> handleTransaction();
                case 4 -> handleViewCompleteStatement();
                case 5 -> handleMonthlyStatement();
                case 6 -> handleCheckBalance();
                case 7 -> handleLogout();
                default -> throw new BankingException(
                        BankingException.ErrorType.VALIDATION_ERROR,
                        "Invalid option selected."
                );
            }
        } catch (BankingException e) {
            handleBankingError(e);
        } catch (Exception e) {
            handleUnexpectedError(e);
        }
    }

    private static int validateMenuChoice(String input, int min, int max) {
        try {
            int choice = Integer.parseInt(input);
            if (choice < min || choice > max) {
                throw new BankingException(
                        BankingException.ErrorType.VALIDATION_ERROR,
                        String.format("Please enter a number between %d and %d.", min, max)
                );
            }
            return choice;
        } catch (NumberFormatException e) {
            throw new BankingException(
                    BankingException.ErrorType.VALIDATION_ERROR,
                    "Please enter a valid number."
            );
        }
    }
    private static void handleViewCompleteStatement() {
        try {
            List<Account> accounts = accountService.getAccountsByUser(authService.getCurrentUser());

            if (accounts.isEmpty()) {
                System.out.println("\nYou don't have any accounts yet. Create one from the main menu.");
                ConsoleUtils.pressEnterToContinue();
                return;
            }

            System.out.println("\n=== Complete Account Statement ===");
            displayAccountList(accounts);

            System.out.print("\nEnter account number: ");
            String accountNumber = scanner.nextLine().trim();
            validateAccountNumber(accountNumber, accounts);

            String statement = statementService.generateDetailedStatement(
                    accountNumber,
                    authService.getCurrentUser()
            );

            logger.info(String.format("Complete statement generated for account: %s", accountNumber));
            System.out.println(statement);
            ConsoleUtils.pressEnterToContinue();

        } catch (BankingException e) {
            handleBankingError(e);
        } catch (Exception e) {
            handleUnexpectedError(e);
        }
    }

    private static void handleViewAccounts() {
        try {
            List<Account> accounts = accountService.getAccountsByUser(authService.getCurrentUser());

            if (accounts.isEmpty()) {
                System.out.println("\nYou don't have any accounts yet. Create one from the main menu.");
                ConsoleUtils.pressEnterToContinue();
                return;
            }

            System.out.println("\n=== Your Accounts ===");
            for (Account account : accounts) {
                System.out.printf("Account Number: %s (%s)%n",
                        account.getAccountNumber(),
                        account.getAccountType());
                System.out.printf("Current Balance: $%.2f%n", account.getBalance());
                System.out.println("------------------------");
            }

            logger.info(String.format("Account list viewed by user: %s",
                    authService.getCurrentUser().getUsername()));
            ConsoleUtils.pressEnterToContinue();

        } catch (BankingException e) {
            handleBankingError(e);
        } catch (Exception e) {
            handleUnexpectedError(e);
        }
    }

    private static void handleCheckBalance() {
        try {
            List<Account> accounts = accountService.getAccountsByUser(authService.getCurrentUser());

            if (accounts.isEmpty()) {
                System.out.println("\nYou don't have any accounts yet. Create one from the main menu.");
                ConsoleUtils.pressEnterToContinue();
                return;
            }

            System.out.println("\n=== Check Balance ===");
            displayAccountList(accounts);

            System.out.print("\nEnter account number to check balance: ");
            String accountNumber = scanner.nextLine().trim();
            validateAccountNumber(accountNumber, accounts);

            Account account = accountService.getAccount(accountNumber);

            if (!account.getAccountHolder().getUserId().equals(authService.getCurrentUser().getUserId())) {
                throw new BankingException(
                        BankingException.ErrorType.AUTHENTICATION_ERROR,
                        "You don't have access to this account"
                );
            }

            System.out.printf("%nAccount Details:%n");
            System.out.printf("Account Number: %s%n", account.getAccountNumber());
            System.out.printf("Account Type: %s%n", account.getAccountType());
            System.out.printf("Current Balance: $%.2f%n", account.getBalance());

            logger.info(String.format("Balance checked for account: %s", accountNumber));
            ConsoleUtils.pressEnterToContinue();

        } catch (BankingException e) {
            handleBankingError(e);
        } catch (Exception e) {
            handleUnexpectedError(e);
        }
    }

    private static void handleMonthlyStatement() {
        try {
            List<Account> accounts = accountService.getAccountsByUser(authService.getCurrentUser());

            if (accounts.isEmpty()) {
                System.out.println("\nYou don't have any accounts yet. Create one from the main menu.");
                ConsoleUtils.pressEnterToContinue();
                return;
            }

            System.out.println("\n=== Monthly Statement ===");
            displayAccountList(accounts);

            System.out.print("\nEnter account number: ");
            String accountNumber = scanner.nextLine().trim();
            validateAccountNumber(accountNumber, accounts);

            LocalDateTime now = LocalDateTime.now();
            LocalDateTime startDate = now.withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0);
            LocalDateTime endDate = now.withDayOfMonth(
                    now.getMonth().length(now.toLocalDate().isLeapYear())
            ).withHour(23).withMinute(59).withSecond(59);

            String statement = statementService.generateMonthlyStatement(
                    accountNumber,
                    authService.getCurrentUser(),
                    startDate,
                    endDate
            );

            logger.info(String.format("Monthly statement generated for account: %s", accountNumber));
            System.out.println(statement);
            ConsoleUtils.pressEnterToContinue();

        } catch (BankingException e) {
            handleBankingError(e);
        } catch (Exception e) {
            handleUnexpectedError(e);
        }
    }

    private static void handleLogin() {
        try {
            System.out.print("Enter username: ");
            String username = scanner.nextLine().trim();

            if (username.isEmpty()) {
                throw new BankingException(
                        BankingException.ErrorType.VALIDATION_ERROR,
                        "Username cannot be empty."
                );
            }

            System.out.print("Enter password: ");
            String password = scanner.nextLine();

            if (password.isEmpty()) {
                throw new BankingException(
                        BankingException.ErrorType.VALIDATION_ERROR,
                        "Password cannot be empty."
                );
            }

            authService.login(username, password);
            logger.info("User logged in successfully: " + username);
            System.out.println("Login successful! Welcome, " + username + "!");
            ConsoleUtils.pressEnterToContinue();

        } catch (BankingException e) {
            handleBankingError(e);
        } catch (Exception e) {
            handleUnexpectedError(e);
        }
    }

    private static void handleRegistration() {
        try {
            System.out.print("Enter desired username: ");
            String username = scanner.nextLine().trim();

            System.out.print("Enter password: ");
            String password = scanner.nextLine();

            authService.registerUser(username, password);
            logger.info("New user registered successfully: " + username);
            System.out.println("Registration successful! Please login with your credentials.");
            ConsoleUtils.pressEnterToContinue();

        } catch (BankingException e) {
            handleBankingError(e);
        } catch (Exception e) {
            handleUnexpectedError(e);
        }
    }

    private static void handleCreateAccount() {
        try {
            System.out.println("\n=== Create New Account ===");
            System.out.println("Account Types Available:");
            System.out.println("1. Savings Account");
            System.out.println("2. Checking Account");
            System.out.print("Choose account type (1-2): ");

            int choice = validateMenuChoice(scanner.nextLine().trim(), 1, 2);
            String accountType = (choice == 1) ? "SAVINGS" : "CHECKING";

            System.out.printf("Enter initial deposit amount (minimum $%.2f): $", MINIMUM_DEPOSIT);
            double initialDeposit = validateAmount(scanner.nextLine().trim());

            Account account = accountService.createAccount(
                    authService.getCurrentUser(),
                    accountType,
                    initialDeposit
            );

            logger.info(String.format("New account created: %s, Type: %s, Initial deposit: $%.2f",
                    account.getAccountNumber(), accountType, initialDeposit));

            System.out.println("\nAccount created successfully!");
            System.out.println("Account Number: " + account.getAccountNumber());
            System.out.printf("Initial Balance: $%.2f%n", account.getBalance());
            ConsoleUtils.pressEnterToContinue();

        } catch (BankingException e) {
            handleBankingError(e);
        } catch (Exception e) {
            handleUnexpectedError(e);
        }
    }

    private static double validateAmount(String input) {
        try {
            double amount = Double.parseDouble(input);
            if (amount < MINIMUM_DEPOSIT) {
                throw new BankingException(
                        BankingException.ErrorType.VALIDATION_ERROR,
                        String.format("Minimum amount required is $%.2f", MINIMUM_DEPOSIT)
                );
            }
            return amount;
        } catch (NumberFormatException e) {
            throw new BankingException(
                    BankingException.ErrorType.VALIDATION_ERROR,
                    "Please enter a valid amount."
            );
        }
    }

    private static void handleTransaction() {
        try {
            List<Account> accounts = accountService.getAccountsByUser(authService.getCurrentUser());

            if (accounts.isEmpty()) {
                System.out.println("\nYou don't have any accounts yet. Create one from the main menu.");
                ConsoleUtils.pressEnterToContinue();
                return;
            }

            System.out.println("\n=== Make Transaction ===");
            System.out.println("1. Deposit");
            System.out.println("2. Withdraw");
            System.out.print("Choose transaction type (1-2): ");

            int choice = validateMenuChoice(scanner.nextLine().trim(), 1, 2);
            displayAccountList(accounts);

            System.out.print("\nEnter account number: ");
            String accountNumber = scanner.nextLine().trim();
            validateAccountNumber(accountNumber, accounts);

            System.out.print("Enter amount: $");
            double amount = validateAmount(scanner.nextLine().trim());

            processTransaction(choice, accountNumber, amount);

            Account account = accountService.getAccount(accountNumber);
            System.out.printf("Current balance: $%.2f%n", account.getBalance());
            ConsoleUtils.pressEnterToContinue();

        } catch (BankingException e) {
            handleBankingError(e);
        } catch (Exception e) {
            handleUnexpectedError(e);
        }
    }

    private static void processTransaction(int choice, String accountNumber, double amount) {
        if (choice == 1) {
            transactionService.deposit(accountNumber, amount, authService.getCurrentUser());
            logger.info(String.format("Deposit successful: Account=%s, Amount=$%.2f",
                    accountNumber, amount));
            System.out.printf("Successfully deposited $%.2f%n", amount);
        } else {
            transactionService.withdraw(accountNumber, amount, authService.getCurrentUser());
            logger.info(String.format("Withdrawal successful: Account=%s, Amount=$%.2f",
                    accountNumber, amount));
            System.out.printf("Successfully withdrew $%.2f%n", amount);
        }
    }

    private static void displayAccountList(List<Account> accounts) {
        System.out.println("\nYour accounts:");
        for (Account account : accounts) {
            System.out.printf("%s (%s) - Balance: $%.2f%n",
                    account.getAccountNumber(),
                    account.getAccountType(),
                    account.getBalance());
        }
    }

    private static void validateAccountNumber(String accountNumber, List<Account> accounts) {
        boolean validAccount = accounts.stream()
                .anyMatch(a -> a.getAccountNumber().equals(accountNumber));

        if (!validAccount) {
            throw new BankingException(
                    BankingException.ErrorType.VALIDATION_ERROR,
                    "Invalid account number."
            );
        }
    }

    private static void handleLogout() {
        try {
            String username = authService.getCurrentUser().getUsername();
            authService.logout();
            logger.info("User logged out successfully: " + username);
            System.out.println("\nLogged out successfully!");
            ConsoleUtils.pressEnterToContinue();
        } catch (Exception e) {
            handleUnexpectedError(e);
        }
    }

    private static void handleBankingError(BankingException e) {
        logger.warning(String.format("%s: %s", e.getErrorType(), e.getMessage()));
        System.out.println("\nERROR: " + e.getErrorType().getDescription());
        System.out.println("Message: " + e.getMessage());
        ConsoleUtils.pressEnterToContinue();
    }

    private static void handleUnexpectedError(Exception e) {
        logger.severe("Unexpected error occurred: " + e.getMessage());
        if (e.getStackTrace().length > 0) {
            logger.severe("Stack trace: " + e.getStackTrace()[0]);
        }
        System.out.println("\nAn unexpected error occurred. Please try again.");
        ConsoleUtils.pressEnterToContinue();
    }

    private static void handleFatalError(Exception e) {
        logger.severe("FATAL ERROR: Unable to initialize the banking system");
        logger.severe("Error: " + e.getMessage());
        if (e.getStackTrace().length > 0) {
            logger.severe("Stack trace: " + e.getStackTrace()[0]);
        }
        System.out.println("\nFATAL ERROR: Unable to initialize the banking system");
        System.out.println("Please check the logs for details.");
        System.out.println("The application will now exit.");
        System.exit(1);
    }

    private static void handleExit() {
        if (scheduler != null) {
            scheduler.shutdown();
            try {
                if (!scheduler.awaitTermination(60, TimeUnit.SECONDS)) {
                    scheduler.shutdownNow();
                }
            } catch (InterruptedException e) {
                scheduler.shutdownNow();
            }
        }

        ConsoleUtils.clearScreen();
        ConsoleUtils.printHeader("GOODBYE!");
        System.out.println("\nThank you for using our banking system.");
        System.out.println("Have a great day!");
        System.exit(0);
    }
}