package app;

import java.util.Scanner;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

/**
 * Utility class for reading database credentials and session date from user input.
 * <p>
 * This class prompts the user to enter the database URL, username, password,
 * and an optional session date. It validates the date format and returns all
 * inputs in a structured array.
 * </p>
 *
 * @author Sam and Jared
 */

public class LineReader {

    /**
     * Prompts the user to enter database credentials and a session date.
     * <p>
     * The method reads four values from standard input:
     * <ul>
     *   <li>[0] = Database URL</li>
     *   <li>[1] = Database Username</li>
     *   <li>[2] = Database Password</li>
     *   <li>[3] = Session Date (formatted as YYYY-MM-DD, or empty string if invalid or blank)</li>
     * </ul>
     * </p>
     *
     * @returns a {@code String[]} containing the database URL, username, password, and session date.
     *          If the date is invalid or omitted, the fourth element will be an empty string.
     * @throws java.util.InputMismatchException if input reading fails unexpectedly.
     */

    public static String[] getCredentials() {
        Scanner scanner = new Scanner(System.in);
        String[] inputs = new String[4];

        System.out.println("=== POS System Setup ===");
        System.out.println("Enter the following values, each on a new line:");
        System.out.println("1. Database URL");
        System.out.println("2. Database Username");
        System.out.println("3. Database Password");
        System.out.println("4. Session Date (YYYY-MM-DD, YYYY/MM/DD, or YYYY MM DD) -- Press Enter to default to current date");

        // Read credentials
        for (int i = 0; i < 3; i++) {
            inputs[i] = scanner.nextLine().trim();
        }

        // Read and validate date input
        String dateInput = scanner.nextLine().trim();

        if (dateInput.isEmpty()) {
            inputs[3] = "";
            System.out.println("No date entered -- default date will be used.");
        } else {
            // Try both allowed formats
            DateTimeFormatter[] acceptedFormats = new DateTimeFormatter[] {
                    DateTimeFormatter.ofPattern("yyyy MM dd"),
                    DateTimeFormatter.ofPattern("yyyy/MM/dd"),
                    DateTimeFormatter.ofPattern("yyyy-MM-dd")
            };

            boolean valid = false;
            for (DateTimeFormatter fmt : acceptedFormats) {
                try {
                    LocalDate date = LocalDate.parse(dateInput, fmt);
                    inputs[3] = date.format(DateTimeFormatter.ISO_LOCAL_DATE);
                    System.out.println("Session date set to: " + inputs[3]);
                    valid = true;
                    break;
                } catch (DateTimeParseException ignored) {
                    // Try the next format
                }
            }

            // If none matched, fallback to empty
            if (!valid) {
                inputs[3] = "";
                System.out.println("Invalid date format -- default date will be used.");
            }
        }

        return inputs;
    }
}
