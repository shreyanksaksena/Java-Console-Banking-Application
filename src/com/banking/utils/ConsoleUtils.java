package com.banking.utils;
import java.io.BufferedReader;
import java.io.InputStreamReader;

public class ConsoleUtils {
    public static void clearScreen() {
        System.out.print("\033[H\033[2J");
        System.out.flush();
    }

    public static void printHeader(String title) {
        System.out.println("\n============================================");
        System.out.printf("           %s%n", title);
        System.out.println("============================================");
    }

    public static void printDivider() {
        System.out.println("--------------------------------------------");
    }

    public static void pressEnterToContinue() {
        System.out.println("\nPress Enter to continue...");
        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
            reader.readLine();
        } catch (Exception e) {
            System.err.printf("An error occurred while waiting for input: %s%n", e.getMessage());
        }
    }
}