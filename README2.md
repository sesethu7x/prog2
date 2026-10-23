# QuickChat

QuickChat is a Java console application for user registration, login, and message sending. It validates user credentials, stores registered accounts in memory for the session, and allows a user to send, review, and save messages.

## Features

- User registration with validation for:
  - username format
  - password complexity
  - international cell phone format
- Login flow with credential checks
- Message validation and hashing
- Message sending workflow with options to send, discard, or store for later
- JSON message storage in a local file
- Unit tests for registration and login behavior

## Project Structure

- `src/main/java/App.java` — application entry point and console flow
- `src/test/java/LoginTest.java` — JUnit tests for registration and login validation
- `pom.xml` — Maven configuration and dependencies

## Prerequisites

- Java JDK 25
- Maven

## Run the project

From the project root:

```bash
mvn compile
java -cp target/classes App
```

## Run tests

```bash
mvn test
```

## Example flow

1. Register a new account.
2. Enter a username containing an underscore and no more than five characters.
3. Use a password with at least 8 characters, including:
   - one uppercase letter
   - one number
   - one special character
4. Enter a valid phone number in international format such as `+27838968976`.
5. Log in with the same username and password.
6. Send messages, review the message list, or store messages to JSON.

## Notes

- The project uses a single Java file for the main logic (`App.java`).
- Messages are stored in a file named `messages.json` while the app is running.
- The project is designed as a simple Java learning/demo application.
