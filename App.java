import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Scanner;

/**
 * ===========================================================================
 *  QuickChat - Registration, Login & Messaging (single-file version)
 * ===========================================================================
 *  Classes in this file:
 *    1. Login        - registration + login validation (Part 1)
 *    2. Message      - message validation, hashing, sending, JSON storage (Part 2)  
 *    3. App          - public class with main(): register -> login -> QuickChat menu
 *
 *  Only App is public (Java allows one public class per file and it must match
 *  the file name). To run the tests in NetBeans, move LoginTest / MessageTest
 *  into their own files (and make them public) so "Test File" can find them.
 * ===========================================================================
 */

// ---------------------------------------------------------------------------
// 1. Login class
// ---------------------------------------------------------------------------
class Login {

    private String firstName;
    private String lastName;
    private String username;
    private String password;
    private String cellPhoneNumber;

    private static final List<Login> registeredUsers = new ArrayList<>();

    private boolean loginSuccessful;
    private String loggedInFirstName;
    private String loggedInLastName;

    public Login(String firstName, String lastName, String username, String password, String cellPhoneNumber) {
        this.firstName = firstName;
        this.lastName = lastName;
        this.username = username;
        this.password = password;
        this.cellPhoneNumber = cellPhoneNumber;
    }

    /** @return true if the username contains an underscore and is <= 5 characters long. */
    public boolean checkUserName() {
        return username != null && username.contains("_") && username.length() <= 5;
    }

    /** @return true if the password is 8+ chars with an uppercase letter, a digit, and a special character. */
    public boolean checkPasswordComplexity() {
        if (password == null || password.length() < 8) {
            return false;
        }
        boolean hasUpper = password.chars().anyMatch(Character::isUpperCase);
        boolean hasDigit = password.chars().anyMatch(Character::isDigit);
        boolean hasSpecial = password.matches(".*[^a-zA-Z0-9].*");
        return hasUpper && hasDigit && hasSpecial;
    }

    /**
     * Shared cell number rule (also reused by Message.checkRecipientCell()):
     * a '+' then a 1-3 digit country code, then 9 digits, e.g. +27838968976.
     */
    public static boolean isValidCellNumber(String number) {
        String regex = "^\\+\\d{1,3}\\d{9}$";
        return number != null && number.matches(regex);
    }

    /** @return true if the cell phone number matches the international format, e.g. +27838968976. */
    public boolean checkCellPhoneNumber() {
        return isValidCellNumber(cellPhoneNumber);
    }

    /** Evaluates registration inputs, stores the user if all checks pass, and returns the status message. */
    public String registerUser() {
        boolean userOk = checkUserName();
        boolean passOk = checkPasswordComplexity();
        boolean cellOk = checkCellPhoneNumber();

        StringBuilder message = new StringBuilder();
        message.append(userOk
                ? "Username successfully captured."
                : "Username is not correctly formatted; please ensure that your username contains an underscore and is no more than five characters in length.");
        message.append(System.lineSeparator());
        message.append(passOk
                ? "Password successfully captured."
                : "Password is not correctly formatted; please ensure that the password contains at least eight characters, a capital letter, a number, and a special character.");
        message.append(System.lineSeparator());
        message.append(cellOk
                ? "Cell phone number successfully added."
                : "Cell number is incorrectly formatted or does not contain an international code; please correct the number and try again.");

        if (userOk && passOk && cellOk) {
            registeredUsers.add(this);
        }

        return message.toString();
    }

    /** @return true if a registered user matches the given username and password. */
    public boolean loginUser(String usernameInput, String passwordInput) {
        for (Login user : registeredUsers) {
            if (user.username.equals(usernameInput) && user.password.equals(passwordInput)) {
                loginSuccessful = true;
                loggedInFirstName = user.firstName;
                loggedInLastName = user.lastName;
                return true;
            }
        }
        loginSuccessful = false;
        loggedInFirstName = null;
        loggedInLastName = null;
        return false;
    }

    /** @return the welcome message on successful login, or the failure message otherwise. */
    public String returnLoginStatus() {
        if (loginSuccessful) {
            return "Welcome " + loggedInFirstName + ", " + loggedInLastName + " it is great to see you again.";
        } else {
            return "Username or password incorrect, please try again.";
        }
    }

    public String getFirstName() { return firstName; }
    public String getLastName() { return lastName; }
    public String getUsername() { return username; }
    public String getCellPhoneNumber() { return cellPhoneNumber; }

    /** Clears the shared registered-users list. Handy between test runs. */
    public static void clearRegisteredUsers() {
        registeredUsers.clear();
    }
}

// ---------------------------------------------------------------------------
// 2. Message class
// ---------------------------------------------------------------------------
class Message {

    private static final int MAX_LENGTH = 250;
    private static final Random RANDOM = new Random();

    // "Database" of messages for as long as the program is running
    private static final List<Message> sentMessages = new ArrayList<>();
    private static final List<Message> storedMessages = new ArrayList<>();
    private static String storagePath = "messages.json";

    private final String messageID;
    private final int messageNumber;      // position of this message (first message = 0)
    private final String recipient;
    private final String messageText;
    private final String messageHash;

    /** Normal constructor: the message ID is randomly generated (10 digits). */
    public Message(int messageNumber, String recipient, String messageText) {
        this(generateMessageID(), messageNumber, recipient, messageText);
    }

    /** Constructor that accepts a known ID (used so the hash can be unit tested). */
    public Message(String messageID, int messageNumber, String recipient, String messageText) {
        this.messageID = messageID;
        this.messageNumber = messageNumber;
        this.recipient = recipient;
        this.messageText = messageText;
        this.messageHash = createMessageHash();
    }

    private static String generateMessageID() {
        long number = (long) (RANDOM.nextDouble() * 10_000_000_000L);
        return String.format("%010d", number);
    }

    // ---------- Required methods ----------

    /** @return true if the message ID is not more than ten characters. */
    public boolean checkMessageID() {
        return messageID != null && messageID.length() <= 10;
    }

    /** @return the "Message ID generated" statement. */
    public String messageIDGenerated() {
        return "Message ID generated: " + messageID;
    }

    /** Validates the recipient number (reuses the Login cell number rule). */
    public String checkRecipientCell() {
        return validateRecipient(recipient);
    }

    public static String validateRecipient(String number) {
        if (Login.isValidCellNumber(number)) {
            return "Cell phone number successfully captured.";
        }
        return "Cell phone number is incorrectly formatted or does not contain an international code. Please correct the number and try again.";
    }

    /** @return "Message ready to send." or how many characters the message is over the limit. */
    public String checkMessageLength() {
        if (messageText.length() <= MAX_LENGTH) {
            return "Message ready to send.";
        }
        int excess = messageText.length() - MAX_LENGTH;
        return "Message exceeds 250 characters by " + excess + "; please reduce the size.";
    }

    /**
     * Creates the message hash: first two digits of the ID, a colon, the message
     * number, a colon, then the first and last word of the message - all in caps.
     * Example: 00:0:HITONIGHT
     */
    public String createMessageHash() {
        String idPart = messageID.substring(0, Math.min(2, messageID.length()));

        String[] words = messageText.trim().split("\\s+");
        String first = cleanWord(words[0]);
        String last = cleanWord(words[words.length - 1]);

        return (idPart + ":" + messageNumber + ":" + first + last).toUpperCase();
    }

    private static String cleanWord(String word) {
        return word.replaceAll("[^A-Za-z0-9]", "");
    }

    /**
     * Lets the user choose what to do with the message.
     * 1 = send, 2 = disregard, 3 = store to send later.
     */
    public String SentMessage(int choice) {
        switch (choice) {
            case 1:
                sentMessages.add(this);
                return "Message successfully sent.";
            case 2:
                return "Press 0 to delete the message.";
            case 3:
                storeMessage();
                return "Message successfully stored.";
            default:
                return "Invalid option. Please choose 1, 2 or 3.";
        }
    }

    /** @return the details of every message sent while the program has been running. */
    public static String printMessages() {
        if (sentMessages.isEmpty()) {
            return "No messages have been sent yet.";
        }
        StringBuilder sb = new StringBuilder();
        for (Message m : sentMessages) {
            sb.append(m.getDetails()).append(System.lineSeparator()).append(System.lineSeparator());
        }
        return sb.toString().trim();
    }

    /** @return the total number of messages sent. */
    public static int returnTotalMessages() {
        return sentMessages.size();
    }

    /** Stores this message in a JSON file (all stored messages are written as a JSON array). */
    public void storeMessage() {
        storedMessages.add(this);

        StringBuilder json = new StringBuilder("[\n");
        for (int i = 0; i < storedMessages.size(); i++) {
            Message m = storedMessages.get(i);
            json.append("  {\n");
            json.append("    \"messageID\": \"").append(jsonEscape(m.messageID)).append("\",\n");
            json.append("    \"messageHash\": \"").append(jsonEscape(m.messageHash)).append("\",\n");
            json.append("    \"recipient\": \"").append(jsonEscape(m.recipient)).append("\",\n");
            json.append("    \"message\": \"").append(jsonEscape(m.messageText)).append("\"\n");
            json.append("  }").append(i < storedMessages.size() - 1 ? "," : "").append("\n");
        }
        json.append("]\n");

        try {
            Files.write(Paths.get(storagePath), json.toString().getBytes(StandardCharsets.UTF_8));
        } catch (IOException e) {
            System.out.println("Could not write to " + storagePath + ": " + e.getMessage());
        }
    }

    private static String jsonEscape(String text) {
        return text.replace("\\", "\\\\")
                   .replace("\"", "\\\"")
                   .replace("\n", "\\n")
                   .replace("\r", "\\r")
                   .replace("\t", "\\t");
    }

    // ---------- Helpers ----------

    /** Full details in the required order: Message ID, Message Hash, Recipient, Message. */
    public String getDetails() {
        return "Message ID: " + messageID + System.lineSeparator()
             + "Message Hash: " + messageHash + System.lineSeparator()
             + "Recipient: " + recipient + System.lineSeparator()
             + "Message: " + messageText;
    }

    public String getMessageID() { return messageID; }
    public String getMessageHash() { return messageHash; }
    public String getRecipient() { return recipient; }
    public String getMessageText() { return messageText; }

    public static void setStoragePath(String path) { storagePath = path; }

    /** Clears all in-memory messages. Handy between test runs. */
    public static void clearAll() {
        sentMessages.clear();
        storedMessages.clear();
    }
}

// ---------------------------------------------------------------------------
// 3. App class (public, runnable QuickChat console app)
// ---------------------------------------------------------------------------
public class App {

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);

        // ----- Register (repeat until all details are valid) -----
        Login login;
        while (true) {
            System.out.println("=== Register ===");
            System.out.print("First name: ");
            String firstName = scanner.nextLine();
            System.out.print("Last name: ");
            String lastName = scanner.nextLine();
            System.out.print("Username (must contain '_' and be <= 5 chars): ");
            String username = scanner.nextLine();
            System.out.print("Password (8+ chars, capital, number, special char): ");
            String password = scanner.nextLine();
            System.out.print("Cell number (e.g. +27838968976): ");
            String cell = scanner.nextLine();

            login = new Login(firstName, lastName, username, password, cell);
            System.out.println(login.registerUser());

            if (login.checkUserName() && login.checkPasswordComplexity() && login.checkCellPhoneNumber()) {
                break;
            }
            System.out.println("\nPlease try registering again.\n");
        }

        // ----- Login (repeat until successful) -----
        System.out.println("\n=== Login ===");
        while (true) {
            System.out.print("Username: ");
            String loginUsername = scanner.nextLine();
            System.out.print("Password: ");
            String loginPassword = scanner.nextLine();

            boolean loggedIn = login.loginUser(loginUsername, loginPassword);
            System.out.println(login.returnLoginStatus());

            if (loggedIn) {
                break;
            }
        }

        // ----- QuickChat (only reachable after a successful login) -----
        System.out.println("\nWelcome to QuickChat.");
        int messageLimit = readPositiveInt(scanner, "How many messages do you wish to enter? ");
        int messagesEntered = 0;

        boolean running = true;
        while (running) {
            System.out.println("\n1) Send Messages");
            System.out.println("2) Show recently sent messages");
            System.out.println("3) Quit");
            System.out.print("Choose an option: ");
            String choice = scanner.nextLine().trim();

            switch (choice) {
                case "1":
                    if (messagesEntered >= messageLimit) {
                        System.out.println("You have already entered the " + messageLimit + " message(s) you planned.");
                        break;
                    }
                    while (messagesEntered < messageLimit) {
                        System.out.println("\n--- Message " + (messagesEntered + 1) + " of " + messageLimit + " ---");
                        enterMessage(scanner, messagesEntered);
                        messagesEntered++;
                    }
                    System.out.println("\nTotal number of messages sent: " + Message.returnTotalMessages());
                    break;
                case "2":
                    System.out.println("Coming Soon.");
                    break;
                case "3":
                    System.out.println("Goodbye.");
                    running = false;
                    break;
                default:
                    System.out.println("Invalid option. Please enter 1, 2 or 3.");
            }
        }

        scanner.close();
    }

    /** Collects one message from the user and handles send / disregard / store. */
    private static void enterMessage(Scanner scanner, int messageNumber) {
        // Recipient
        String recipient;
        while (true) {
            System.out.print("Recipient cell number (e.g. +27718693002): ");
            recipient = scanner.nextLine().trim();
            String result = Message.validateRecipient(recipient);
            System.out.println(result);
            if (result.equals("Cell phone number successfully captured.")) {
                break;
            }
        }

        // Message text (max 250 characters)
        String text;
        while (true) {
            System.out.print("Message: ");
            text = scanner.nextLine();
            if (text.trim().isEmpty()) {
                System.out.println("The message cannot be empty.");
            } else if (text.length() > 250) {
                System.out.println("Please enter a message of less than 250 characters.");
            } else {
                break;
            }
        }

        Message message = new Message(messageNumber, recipient, text);
        System.out.println(message.messageIDGenerated());
        System.out.println(message.checkMessageLength());

        // Send / Disregard / Store
        int option;
        while (true) {
            System.out.println("1) Send Message");
            System.out.println("2) Disregard Message");
            System.out.println("3) Store Message to send later");
            System.out.print("Choose an option: ");
            String input = scanner.nextLine().trim();
            if (input.equals("1") || input.equals("2") || input.equals("3")) {
                option = Integer.parseInt(input);
                break;
            }
            System.out.println("Invalid option. Please enter 1, 2 or 3.");
        }

        System.out.println(message.SentMessage(option));

        if (option == 1) {
            System.out.println();
            System.out.println(message.getDetails());
        } else if (option == 2) {
            String confirm = scanner.nextLine().trim();
            System.out.println(confirm.equals("0") ? "Message deleted." : "Message disregarded.");
        }
    }

    private static int readPositiveInt(Scanner scanner, String prompt) {
        while (true) {
            System.out.print(prompt);
            String input = scanner.nextLine().trim();
            try {
                int value = Integer.parseInt(input);
                if (value > 0) {
                    return value;
                }
            } catch (NumberFormatException e) {
                // fall through to the error message
            }
            System.out.println("Please enter a whole number greater than 0.");
        }
    }
}
