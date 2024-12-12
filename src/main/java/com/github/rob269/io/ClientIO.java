package com.github.rob269.io;

import com.github.rob269.Main;
import com.github.rob269.Message;
import com.github.rob269.User;
import com.github.rob269.logging.ConsoleFormatter;
import com.github.rob269.rsa.*;
import com.google.common.hash.Hashing;

import java.io.*;
import java.math.BigInteger;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ClientIO {
    private DataOutputStream dos;
    private DataInputStream dis;
    private Key clientKey;
    private boolean isClosed = false;
    private boolean initialized = false;
    private String userId = null;
    private long handshakeTimer;

    private static final Logger LOGGER = Logger.getLogger(ClientIO.class.getName());

    public ClientIO(Socket clientSocket, long handshakeTimer) {
        try {
            dos = new DataOutputStream(clientSocket.getOutputStream());
            dis = new DataInputStream(clientSocket.getInputStream());
            this.handshakeTimer = handshakeTimer;
            LOGGER.fine("Output and Input streams is open");
        } catch (IOException e) {
            LOGGER.warning("Can't open streams\n" + ConsoleFormatter.formatStackTrace(e));
        }
    }

    public String getUserId() {
        return userId;
    }

    public boolean isClosed() {
        return isClosed;
    }

    public void init() throws WrongKeyException {
        while (!isClosed) {
            String request = readFirst();
            if (request.isEmpty()) {
                close();
                break;
            }
            if (!initialized){
                handleInitialRequest(request);
            }
            else {
                handleRequest(request);
            }
        }
    }

    private void initClientKey(UserKey clientKey) throws WrongKeyException{
        if (clientKey.isAuthenticated()) {
            this.clientKey = clientKey;
            LOGGER.info("User key is approved");
            if (checkInitialization()) {
                initialized = true;
                List<String> login = read();
                String hash = Hashing.sha256().hashString(login.get(1)+login.getFirst(), StandardCharsets.UTF_8).toString();
                List<String> dbLine = Main.USERS.readLine(1, login.get(0));
                if (!dbLine.isEmpty()) {
                    if (!dbLine.get(2).equals(hash)) {
                        write("AUTHENTICATION ERROR");
                        close();
                        return;
                    }
                }
                else {
                    Main.USERS.write(new String[]{login.getFirst(), hash, "NOW()"});
                }
                userId = login.getFirst();
                Thread.currentThread().setName("{" + userId+"}MainThread");
                Main.usersOnline.add(userId);
                write("AUTHENTICATED");
                LOGGER.warning("Handshake complete in " + (System.currentTimeMillis() - handshakeTimer) + "ms");
            }
            else {
                LOGGER.warning("Fail initialization");
                close();
            }
        }
        else {
            write("Wrong key");
            throw new WrongKeyException("Wrong key");
        }
    }

    private boolean checkInitialization() {
        write(RSA.encodeString("INITIALIZED", clientKey));
        if (RSA.decodeString(readFirst(), RSAServerKeys.getPrivateKey()).equals("INITIALIZED")) {
            write(RSA.encodeString("OK", clientKey));
            return true;
        }
        return false;
    }

    private void handleRequest(String request) {
        switch (request) {
            case "PING" -> write("PING", Level.FINEST);
            case "KEEP ALIVE" -> write("OK KEEP ALIVE", Level.ALL);
            case "EXIT" -> close();
            case "SEND MESSAGE" -> {
                List<String> messageStr = read();
                Message message = new Message(messageStr.getFirst(), userId, messageStr.get(1));
                Message.writeToDatabase(message);
                if (Main.usersOnline.contains(messageStr.getFirst()))
                    Main.needToCheckMessages.add(messageStr.getFirst());
                write("MESSAGE OK");
            }
            case "CHECK" -> {
                if (Main.needToCheckMessages.contains(userId)) {
                    write("CHECK YES", Level.ALL);
                }
                else {
                    write("CHECK NO", Level.ALL);
                }
            }
            case "GET NEW MESSAGES" -> {
                List<String[]> messages = Main.MESSAGES.sqlRead
                        ("SELECT * FROM hello_messenger_db.user_messages WHERE (SELECT last_online FROM hello_messenger_db.users WHERE user_id='%s') <= date AND recipient='%s';"
                                .formatted(userId, userId));
                Main.needToCheckMessages.remove(userId);
                sendMessages(messages);
            }
            case "GET MESSAGES" -> {
                List<String[]> messages = Main.MESSAGES.sqlRead(
                        "SELECT * FROM hello_messenger_db.user_messages WHERE recipient='%s';".formatted(userId));
                Main.needToCheckMessages.remove(userId);
                sendMessages(messages);
            }
            case "GET MESSAGES COUNT" -> {
                List<String[]> messages = Main.MESSAGES.sqlRead(
                        "SELECT * FROM hello_messenger_db.user_messages WHERE recipient='%s';".formatted(userId));
                write(String.valueOf(messages.size()));
            }
            case "GET SENT MESSAGES" -> {
                List<String[]> messages = Main.MESSAGES.sqlRead(
                        "SELECT * FROM hello_messenger_db.user_messages WHERE sender='%s'".formatted(userId)
                );
                sendSentMessages(messages);
            }
            case "GET SENT MESSAGES COUNT" -> {
                List<String[]> messages = Main.MESSAGES.sqlRead(
                        "SELECT * FROM hello_messenger_db.user_messages WHERE sender='%s';".formatted(userId));
                write(String.valueOf(messages.size()));
            }
            case "SEND FILE" -> {
                List<String> lines;
                ResourcesIO.delete("files/file.txt");
                long start = System.currentTimeMillis();
                while (true){
                    lines = read();
                    if (lines.getFirst().equals("#END")) break;
                    ResourcesIO.write("files/file.txt", lines, true);
                }
                LOGGER.warning("Complete in " + (System.currentTimeMillis()-start) + "ms");
            }
        }
    }

    private void sendMessages(List<String[]> messages) {
        Map<String, List<Message>> messagesMap = new HashMap<>();
        for (String[] message : messages) {
            if (messagesMap.containsKey(message[1])) {
                List<Message> l = messagesMap.get(message[1]);
                l.add(new Message(message[2], message[1], message[3], message[4]));
                messagesMap.put(message[1], l);
            } else {
                messagesMap.put(message[1], new ArrayList<>(List.of(new Message(message[2], message[1], message[3], message[4]))));
            }
        }
        String[] senders = messagesMap.keySet().toArray(new String[0]);
        for (String sender : senders) {
            write("#USER", Level.ALL);
            write("{" + sender + "}", Level.ALL);
            List<Message> message = messagesMap.get(sender);
            for (Message value : message) {
                write("{" + value.getMessage() + "}" + "\n" + "{" + value.getDate() + "}", Level.ALL);
            }
        }
        write("#END", Level.ALL);
        Main.USERS.update(1, userId, 3, "NOW()");
    }
    private void sendSentMessages(List<String[]> messages) {
        Map<String, List<Message>> messagesMap = new HashMap<>();
        for (String[] message : messages) {
            if (messagesMap.containsKey(message[2])) {
                List<Message> l = messagesMap.get(message[2]);
                l.add(new Message(message[2], message[1], message[3], message[4]));
                messagesMap.put(message[2], l);
            } else {
                messagesMap.put(message[2], new ArrayList<>(List.of(new Message(message[2], message[1], message[3], message[4]))));
            }
        }
        String[] recipients = messagesMap.keySet().toArray(new String[0]);
        for (String recipient : recipients) {
            write("#USER", Level.ALL);
            write("{" + recipient + "}", Level.ALL);
            List<Message> message = messagesMap.get(recipient);
            for (Message value : message) {
                write("{" + value.getMessage() + "}" + "\n" + "{" + value.getDate() + "}", Level.ALL);
            }
        }
        write("#END", Level.ALL);
    }

    private void handleInitialRequest(String request) throws WrongKeyException {
        switch (request) {
            case "GET RSA KEY" -> write(RSAServerKeys.getPublicKey().toString());
            case "KEY" -> {
                List<String> lines = read();
                if (lines.size() >= 5) {
                    BigInteger[] keyInteger = new BigInteger[]{
                            new BigInteger(lines.get(0)),
                            new BigInteger(lines.get(1))
                    };
                    User user = new User(RSA.decodeString(lines.get(4), RSAServerKeys.getPrivateKey()));
                    UserKey clientKey = new UserKey(keyInteger, user);
                    BigInteger[] meta = new BigInteger[]{
                            new BigInteger(lines.get(2)),
                            new BigInteger(lines.get(3))
                    };
                    clientKey.setMeta(meta);
                    initClientKey(clientKey);
                }
                else {
                    LOGGER.warning("Wrong key format");
                    write("WRONG KEY FORMAT");
                }
            }
            case "REGISTER NEW KEY" -> {
                List<String> lines = read();
                if (lines.size()>=3) {
                    UserKey newKey = new UserKey(new BigInteger[]{new BigInteger(lines.getFirst()), new BigInteger(lines.get(1))},
                            new User(RSA.decodeString(lines.get(2), RSAServerKeys.getPrivateKey())));
                    if (!RSAKeys.isIdentified(newKey)) {
                        UserKey keyToReturn;
                        RSAKeys.registerNewKey(newKey, false);
                        keyToReturn = UserKey.getFromDatabase(newKey.getUser().getId());
                        if (keyToReturn == null) {
                            LOGGER.warning("Key is null");
                            write("500 ERROR");
                            return;
                        }
                        write("META");
                        write(keyToReturn.getMeta()[0] + "\n" + keyToReturn.getMeta()[1] + "\n");
                    } else {
                        LOGGER.warning("Key is already registered");
                        write("KEY IS REJECTED");
                    }
                }
                else {
                    LOGGER.warning("Wrong key format");
                    write("WRONG KEY FORMAT");
                }
            }
            case "RESET KEY" -> {
                List<String> login = List.of(RSA.decodeString(readFirst(), RSAServerKeys.getPrivateKey()).split("\n"));
                List<String> dbResponse = Main.USERS.readLine(1, login.getFirst());
                String hash = Hashing.sha256().hashString(login.get(1)+login.getFirst(), StandardCharsets.UTF_8).toString();
                if ((!dbResponse.isEmpty()) && (!dbResponse.get(2).equals(hash))){
                    write("AUTHENTICATION ERROR");
                    close();
                    return;
                }
                Main.RSA_KEYS.remove(5, login.getFirst());
                write("OK");
            }
        }
    }

    public String readFirst(){
        List<String> list = read();
        if (list != null && !list.isEmpty()) {
            return list.getFirst();
        }
        return "";
    }

    public List<String> read() {
        return read(Level.FINER);
    }

    public List<String> read(Level loggingLevel) {
        List<String> lines = new ArrayList<>();
        if (!isClosed){
            try {
                String inputString = dis.readUTF();
                if (initialized) {
                    inputString = RSA.decodeString(inputString, RSAServerKeys.getPrivateKey());
                }
                lines = new ArrayList<>(List.of(inputString.split("\n")));
                StringBuilder stringBuilder = new StringBuilder();
                for (String line : lines)
                    stringBuilder.append(line).append("\n");
                String log = (stringBuilder.toString().endsWith("\n") ? stringBuilder.substring(0, stringBuilder.length() - 1) : stringBuilder.toString());
                if (!log.equals("KEEP ALIVE") && !log.equals("CHECK")) LOGGER.log(loggingLevel, "Get message:\n" + log);
            } catch (IOException e) {
                LOGGER.warning("Can't read lines\n" + ConsoleFormatter.formatStackTrace(e));
            }
        }
        return lines;
    }

    public void write(String message) {
        write(message, Level.FINER);
    }

    public void write(String message, Level loggingLevel) {
        if (message == null) {
            LOGGER.warning("Null message");
            return;
        }
        if (!isClosed){
            try {
                String log = (message.endsWith("\n") ? message.substring(0, message.length() - 1) : message);
                LOGGER.log(loggingLevel, "Message sent:\n" + log);
                if (initialized)
                    message = RSA.encodeString(message, clientKey);
                dos.writeUTF(message);
                dos.flush();
            } catch (IOException e) {
                LOGGER.warning("Can't send the message\n" + ConsoleFormatter.formatStackTrace(e));
            }
        }
    }

    public void write(List<String> lines) {
        if (lines == null || lines.isEmpty()) {
            LOGGER.warning("Null message");
            return;
        }
        if (!isClosed){
            StringBuilder stringBuilder = new StringBuilder();
            for (String line : lines)
                stringBuilder.append(line).append("\n");
            String message = stringBuilder.toString();
            LOGGER.finer("Sending message:\n" + (message.endsWith("\n") ? message.substring(0, message.length() - 1) : message));
            try {
                if (initialized) message = RSA.encodeString(message, clientKey);
                dos.writeUTF(message);
                dos.flush();
            } catch (IOException e) {
                LOGGER.warning("Can't send the message\n" + ConsoleFormatter.formatStackTrace(e));
            }
        }
    }

    public void close() {
        if (userId != null) Main.usersOnline.remove(userId);
        isClosed = true;
        try {
            dis.close();
            dos.close();
        } catch (IOException e) {
            LOGGER.warning("Can't close streams");
        }
    }
}
