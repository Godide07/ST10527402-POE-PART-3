package com.mycompany.main;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Scanner;

import static org.junit.jupiter.api.Assertions.*;

class MainTest {

    private final PrintStream originalOut = System.out;
    private ByteArrayOutputStream outContent;

    @BeforeEach
    void setUp() {
        outContent = new ByteArrayOutputStream();
        System.setOut(new PrintStream(outContent));

        Main.registeredUsername = null;
        Main.registeredPassword = null;
        Main.registeredCellphone = null;
        Main.totalMessages = 0;
        Main.messagesEntered = 0;
        Main.messagesSent = 0;
        Main.sentMessages.clear();
        Main.storedMessages.clear();
        Main.disregardedMessages.clear();
        Main.messageHashes.clear();
        Main.messageIDs.clear();
    }

    @AfterEach
    void tearDown() throws Exception {
        System.setOut(originalOut);
        Files.deleteIfExists(Path.of("stored_messages.json"));
    }

    private void provideInput(String data) {
        Main.input = new Scanner(new ByteArrayInputStream(data.getBytes()));
    }

    private String output() {
        return outContent.toString();
    }

    // ---------- Username ----------

    @Test
    void checkUserName_valid() {
        assertTrue(Main.checkUserName("ky_l1"));
    }

    @Test
    void checkUserName_noUnderscore() {
        assertFalse(Main.checkUserName("kyle1"));
    }

    @Test
    void checkUserName_tooLong() {
        assertFalse(Main.checkUserName("kyle_1"));
    }

    @Test
    void checkUserName_tooShort() {
        assertFalse(Main.checkUserName("ky_"));
    }

    // ---------- Password ----------

    @Test
    void checkPasswordComplexity_valid() {
        assertTrue(Main.checkPasswordComplexity("Ch&&sec@ke99!"));
    }

    @Test
    void checkPasswordComplexity_tooShort() {
        assertFalse(Main.checkPasswordComplexity("Ab1!"));
    }

    @Test
    void checkPasswordComplexity_noUppercase() {
        assertFalse(Main.checkPasswordComplexity("password1!"));
    }

    @Test
    void checkPasswordComplexity_noLowercase() {
        assertFalse(Main.checkPasswordComplexity("PASSWORD1!"));
    }

    @Test
    void checkPasswordComplexity_noDigit() {
        assertFalse(Main.checkPasswordComplexity("Password!!"));
    }

    @Test
    void checkPasswordComplexity_noSpecialCharacter() {
        assertFalse(Main.checkPasswordComplexity("Password11"));
    }

    // ---------- Cellphone ----------

    @Test
    void checkCellphoneNumber_valid() {
        assertTrue(Main.checkCellphoneNumber("+27831234567"));
    }

    @Test
    void checkCellphoneNumber_missingPrefix() {
        assertFalse(Main.checkCellphoneNumber("0831234567"));
    }

    @Test
    void checkCellphoneNumber_tooFewDigits() {
        assertFalse(Main.checkCellphoneNumber("+2783123456"));
    }

    @Test
    void checkCellphoneNumber_tooManyDigits() {
        assertFalse(Main.checkCellphoneNumber("+278312345678"));
    }

    @Test
    void checkCellphoneNumber_containsLetters() {
        assertFalse(Main.checkCellphoneNumber("+2783123456a"));
    }

    // ---------- Registration ----------

    @Test
    void registerUser_validDetailsAreStored() {
        provideInput("ky_l1\nCh&&sec@ke99!\n+27831234567\n");
        Main.registerUser();

        assertEquals("ky_l1", Main.registeredUsername);
        assertEquals("Ch&&sec@ke99!", Main.registeredPassword);
        assertEquals("+27831234567", Main.registeredCellphone);
        assertTrue(output().contains("User registered successfully!"));
    }

    @Test
    void registerUser_invalidThenValidRetries() {
        provideInput("bad\nbad\nbad\n"
                + "ky_l1\nCh&&sec@ke99!\n+27831234567\n");
        Main.registerUser();

        assertTrue(output().contains("Registration failed."));
        assertTrue(output().contains("Username must be exactly 5 characters"));
        assertTrue(output().contains("Password must have at least 8 characters"));
        assertTrue(output().contains("Cellphone number must start with +27"));
        assertEquals("ky_l1", Main.registeredUsername);
    }

    // ---------- Login ----------

    @Test
    void loginUser_correctCredentials() {
        Main.registeredUsername = "ky_l1";
        Main.registeredPassword = "Ch&&sec@ke99!";
        assertTrue(Main.loginUser("ky_l1", "Ch&&sec@ke99!"));
    }

    @Test
    void loginUser_wrongPassword() {
        Main.registeredUsername = "ky_l1";
        Main.registeredPassword = "Ch&&sec@ke99!";
        assertFalse(Main.loginUser("ky_l1", "wrong"));
    }

    @Test
    void loginUser_wrongUsername() {
        Main.registeredUsername = "ky_l1";
        Main.registeredPassword = "Ch&&sec@ke99!";
        assertFalse(Main.loginUser("xx_x1", "Ch&&sec@ke99!"));
    }

    @Test
    void returnLoginStatus_success() {
        Main.registeredUsername = "ky_l1";
        assertEquals("Login successful. Welcome ky_l1!", Main.returnLoginStatus(true));
    }

    @Test
    void returnLoginStatus_failure() {
        assertEquals("Username or password incorrect. Please try again.", Main.returnLoginStatus(false));
    }

    @Test
    void performLogin_succeedsOnSecondAttempt() {
        Main.registeredUsername = "ky_l1";
        Main.registeredPassword = "Ch&&sec@ke99!";
        provideInput("ky_l1\nwrong\nky_l1\nCh&&sec@ke99!\n");

        assertTrue(Main.performLogin());
    }

    @Test
    void performLogin_failsAfterThreeAttempts() {
        Main.registeredUsername = "ky_l1";
        Main.registeredPassword = "Ch&&sec@ke99!";
        provideInput("a\nb\nc\nd\ne\nf\n");

        assertFalse(Main.performLogin());
        assertTrue(output().contains("Too many failed attempts."));
    }

    // ---------- Message ID and hash ----------

    @Test
    void generateMessageID_isTenDigitNumber() {
        for (int i = 0; i < 100; i++) {
            String id = Main.generateMessageID();
            assertEquals(10, id.length());
            assertTrue(id.matches("[0-9]{10}"));
        }
    }

    @Test
    void createMessageHash_multiWordMessage() {
        assertEquals("12:1:HITONIGHT", Main.createMessageHash("1234567890", 1, "Hi tonight"));
    }

    @Test
    void createMessageHash_singleWordUsesSameWordTwice() {
        assertEquals("98:3:HELLOHELLO", Main.createMessageHash("9876543210", 3, "Hello"));
    }

    @Test
    void createMessageHash_ignoresSurroundingWhitespace() {
        assertEquals("12:2:HIBYE", Main.createMessageHash("1234567890", 2, "  Hi there   Bye  "));
    }

    // ---------- Reading integers ----------

    @Test
    void readInteger_returnsValidNumber() {
        provideInput("42\n");
        assertEquals(42, Main.readInteger("Number: "));
    }

    @Test
    void readInteger_retriesOnInvalidInput() {
        provideInput("abc\n7\n");
        assertEquals(7, Main.readInteger("Number: "));
        assertTrue(output().contains("Please enter a valid whole number."));
    }

    // ---------- Sending messages ----------

    @Test
    void sendMessages_sendOption() {
        Main.totalMessages = 3;
        provideInput("+27831234567\nHello world\n1\n");
        Main.sendMessages();

        assertEquals(1, Main.sentMessages.size());
        assertEquals(1, Main.messagesSent);
        assertEquals(1, Main.messagesEntered);
        assertEquals(1, Main.messageIDs.size());
        assertEquals(1, Main.messageHashes.size());
        assertTrue(output().contains("Message successfully sent"));
        assertTrue(Main.sentMessages.get(0).endsWith("|+27831234567|Hello world"));
    }

    @Test
    void sendMessages_disregardOption() {
        Main.totalMessages = 3;
        provideInput("+27831234567\nHello world\n2\n");
        Main.sendMessages();

        assertEquals(0, Main.sentMessages.size());
        assertEquals(1, Main.disregardedMessages.size());
        assertEquals("Hello world", Main.disregardedMessages.get(0));
        assertEquals(1, Main.messagesEntered);
        assertTrue(output().contains("Message disregarded."));
    }

    @Test
    void sendMessages_storeOptionSavesJsonFile() throws Exception {
        Main.totalMessages = 3;
        provideInput("+27831234567\nHello \"world\"\n3\n");
        Main.sendMessages();

        assertEquals(1, Main.storedMessages.size());
        assertTrue(output().contains("Message successfully stored"));

        Path file = Path.of("stored_messages.json");
        assertTrue(Files.exists(file));
        String json = Files.readString(file);
        assertTrue(json.startsWith("["));
        assertTrue(json.contains("Hello \\\"world\\\""));
    }

    @Test
    void sendMessages_invalidRecipientThenValid() {
        Main.totalMessages = 1;
        provideInput("0831234567\n+27831234567\nHi there\n1\n");
        Main.sendMessages();

        assertTrue(output().contains("Invalid number."));
        assertEquals(1, Main.messagesSent);
    }

    @Test
    void sendMessages_emptyThenTooLongThenValidMessage() {
        Main.totalMessages = 1;
        String tooLong = "a".repeat(250);
        provideInput("+27831234567\n   \n" + tooLong + "\nValid message\n1\n");
        Main.sendMessages();

        assertTrue(output().contains("Message cannot be empty."));
        assertTrue(output().contains("Please enter a message of less than 250 characters."));
        assertEquals(1, Main.messagesSent);
    }

    @Test
    void sendMessages_invalidMenuChoiceRetries() {
        Main.totalMessages = 1;
        provideInput("+27831234567\nHello there\n9\n1\n");
        Main.sendMessages();

        assertTrue(output().contains("Invalid choice. Choose 1, 2 or 3."));
        assertEquals(1, Main.messagesSent);
    }

    @Test
    void sendMessages_doesNothingWhenLimitReached() {
        Main.totalMessages = 1;
        Main.messagesEntered = 1;
        provideInput("");
        Main.sendMessages();

        assertTrue(output().contains("You have already entered your maximum number of messages."));
        assertEquals(0, Main.sentMessages.size());
    }

    // ---------- Stored messages ----------

    private void addStored(String id, String hash, String recipient, String message) {
        Main.storedMessages.add(id + "|" + hash + "|" + recipient + "|" + message);
    }

    @Test
    void displayStoredMessages_listsIdAndRecipient() {
        addStored("1111111111", "11:1:HIBYE", "+27831111111", "Hi bye");
        addStored("2222222222", "22:2:YOHEY", "+27832222222", "Yo hey");
        Main.displayStoredMessages();

        String out = output();
        assertTrue(out.contains("Message ID: 1111111111, Recipient: +27831111111"));
        assertTrue(out.contains("Message ID: 2222222222, Recipient: +27832222222"));
    }

    @Test
    void displayLongestStoredMessage_returnsLongest() {
        addStored("1111111111", "11:1:AB", "+27831111111", "short");
        addStored("2222222222", "22:2:CD", "+27832222222", "this one is the longest message");
        addStored("3333333333", "33:3:EF", "+27833333333", "medium length");
        Main.displayLongestStoredMessage();

        assertTrue(output().contains("Longest stored message: this one is the longest message"));
    }

    @Test
    void displayLongestStoredMessage_emptyList() {
        Main.displayLongestStoredMessage();
        assertTrue(output().contains("Longest stored message: "));
    }

    @Test
    void searchMessageByID_found() {
        addStored("1111111111", "11:1:HIBYE", "+27831111111", "Hi bye");
        Main.searchMessageByID("1111111111");

        assertTrue(output().contains("Message found - Recipient: +27831111111, Message: Hi bye"));
    }

    @Test
    void searchMessageByID_notFound() {
        addStored("1111111111", "11:1:HIBYE", "+27831111111", "Hi bye");
        Main.searchMessageByID("0000000000");

        assertTrue(output().contains("Message ID not found."));
    }

    @Test
    void searchMessagesByRecipient_returnsAllMatches() {
        addStored("1111111111", "11:1:AB", "+27831111111", "first");
        addStored("2222222222", "22:2:CD", "+27832222222", "other");
        addStored("3333333333", "33:3:EF", "+27831111111", "second");
        Main.searchMessagesByRecipient("+27831111111");

        String out = output();
        assertTrue(out.contains("Message: first"));
        assertTrue(out.contains("Message: second"));
        assertFalse(out.contains("Message: other"));
    }

    @Test
    void searchMessagesByRecipient_noMatchesPrintsNothing() {
        addStored("1111111111", "11:1:AB", "+27831111111", "first");
        Main.searchMessagesByRecipient("+27839999999");

        assertFalse(output().contains("Message:"));
    }

    @Test
    void deleteMessageByHash_removesMessage() {
        addStored("1111111111", "11:1:HIBYE", "+27831111111", "Hi bye");
        addStored("2222222222", "22:2:YOHEY", "+27832222222", "Yo hey");
        Main.deleteMessageByHash("11:1:HIBYE");

        assertEquals(1, Main.storedMessages.size());
        assertTrue(output().contains("Message: \"Hi bye\" successfully deleted."));
    }

    @Test
    void deleteMessageByHash_notFound() {
        addStored("1111111111", "11:1:HIBYE", "+27831111111", "Hi bye");
        Main.deleteMessageByHash("99:9:NOPE");

        assertEquals(1, Main.storedMessages.size());
        assertTrue(output().contains("Message hash not found."));
    }

    // ---------- Reports and display ----------

    @Test
    void displayReport_listsSentMessages() {
        Main.sentMessages.add("1111111111|11:1:HIBYE|+27831111111|Hi bye");
        Main.displayReport();

        assertTrue(output().contains(
                "Message ID: 1111111111, Message Hash: 11:1:HIBYE, Recipient: +27831111111, Message: Hi bye"));
    }

    @Test
    void displayMessage_printsAllFields() {
        Main.displayMessage("1234567890", "12:1:HITONIGHT", "+27831234567", "Hi tonight");
        String out = output();

        assertTrue(out.contains("Message ID: 1234567890"));
        assertTrue(out.contains("Message Hash: 12:1:HITONIGHT"));
        assertTrue(out.contains("Recipient: +27831234567"));
        assertTrue(out.contains("Message: Hi tonight"));
    }

    @Test
    void saveStoredMessages_writesValidJsonArray() throws Exception {
        Main.storedMessages.add("1|H|+27831234567|line");
        Main.storedMessages.add("2|H|+27831234567|tab\there");
        Main.saveStoredMessages();

        String json = Files.readString(Path.of("stored_messages.json"));
        assertTrue(json.startsWith("["));
        assertTrue(json.trim().endsWith("]"));
        assertTrue(json.contains("\"1|H|+27831234567|line\","));
        assertTrue(json.contains("tab\\there"));
    }
}
