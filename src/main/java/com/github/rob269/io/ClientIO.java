package com.github.rob269.io;

import com.github.rob269.Main;
import com.github.rob269.Message;
import com.github.rob269.User;
import com.github.rob269.logging.ConsoleFormatter;
import com.github.rob269.rsa.*;
import org.jetbrains.annotations.NotNull;

import java.io.*;
import java.math.BigInteger;
import java.net.Socket;
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

    public Key getClientKey() {
        return clientKey;
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
                List<String> dbLine = Main.USERS.readLine(1, login.get(0));
                if (!dbLine.isEmpty()) {
                    if (!dbLine.get(2).equals(login.get(1))) {
                        write("AUTHENTICATION ERROR");
                        close();
                        return;
                    }
                }
                else {
                    Main.USERS.write(new String[]{login.getFirst(), login.get(1), "NOW()"});
                }
                userId = login.get(0);
                write("AUTHENTICATED");
                LOGGER.warning("Handshake complete in " + (System.currentTimeMillis() - handshakeTimer) + "ms");
            }
            else {
                LOGGER.warning("Fail initialization");
                close();
            }
        }
        else {
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
            case "PING" ->  {
                write("PING");
            }
            case "KEEP ALIVE" -> write("OK KEEP ALIVE");
            case "EXIT" -> {
                close();
            }
            case "SEND MESSAGE" -> {
                List<String> messageStr = read();
                Message message = new Message(messageStr.getFirst(), userId, messageStr.get(1));
                Message.writeToDatabase(message);
                write("MESSAGE OK");
            }
            case "GET MESSAGES" -> {
                List<String[]> messages = Main.MESSAGES.sqlRead
                        ("SELECT * FROM hello_messenger_db.user_messages WHERE (SELECT last_online FROM hello_messenger_db.users WHERE user_id='%s') <= date AND recipient='%s';"
                                .formatted(userId, userId));
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
                    write("#USER");
                    write("{" + sender + "}");
                    List<Message> message = messagesMap.get(sender);
                    for (Message value : message) {
                        write("{" + value.getMessage() + "}" + "\n" + "{" + value.getDate() + "}");
                    }
                }
                write("#END");
            }
            case "SEND FILE" -> {
                List<String> lines;
                ResourcesIO.delete("files/file.txt");
                long start = System.currentTimeMillis();
                while (true){
                    lines = read();
                    if (lines.getFirst().equals("END")) break;
                    ResourcesIO.write("files/file.txt", lines, true);
                }
                LOGGER.warning("Complete in " + (System.currentTimeMillis()-start) + "ms");
            }
        }
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
                if ((!dbResponse.isEmpty()) && (!dbResponse.get(2).equals(login.get(1)))){
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
        List<String> lines = new ArrayList<>();
        try {
            String inputString = dis.readUTF();
            if (initialized) {
                inputString = RSA.decodeString(inputString, RSAServerKeys.getPrivateKey());
            }
            lines = new ArrayList<>(List.of(inputString.split("\n")));
            StringBuilder stringBuilder = new StringBuilder();
            for (String line : lines)
                stringBuilder.append(line).append("\n");
            String log = (stringBuilder.toString().endsWith("\n") ? stringBuilder.substring(0, stringBuilder.length()-1) : stringBuilder.toString());
            if (!log.equals("KEEP ALIVE")) LOGGER.finer("Get message:\n" + log);
        }
        catch (IOException e) {
            LOGGER.warning("Can't read lines\n" + ConsoleFormatter.formatStackTrace(e));
        }
        return lines;
    }

    public void write(String message) {
        if (message == null) {
            LOGGER.warning("Null message");
            return;
        }
        try {
            String log = (message.endsWith("\n") ? message.substring(0, message.length()-1) : message);
            if (!log.equals("OK KEEP ALIVE")) LOGGER.finer("Message sent:\n" + log);
            if (initialized)
                message = RSA.encodeString(message, clientKey);
            dos.writeUTF(message);
            dos.flush();
        } catch (IOException e) {
            LOGGER.warning("Can't send the message\n" + ConsoleFormatter.formatStackTrace(e));
        }
    }

    public void write(List<String> lines) {
        if (lines == null || lines.isEmpty()) {
            LOGGER.warning("Null message");
            return;
        }
        StringBuilder stringBuilder = new StringBuilder();
        for (String line : lines)
            stringBuilder.append(line).append("\n");
        String message = stringBuilder.toString();
        LOGGER.finer("Sending message:\n" + (message.endsWith("\n") ? message.substring(0, message.length()-1) : message));
        try {
            if (initialized) message = RSA.encodeString(message, clientKey);
            dos.writeUTF(message);
            dos.flush();
        } catch (IOException e) {
            LOGGER.warning("Can't send the message\n" + ConsoleFormatter.formatStackTrace(e));
        }
    }

    public void close() {
        isClosed = true;
        try {
            dis.close();
            dos.close();
        } catch (IOException e) {
            LOGGER.warning("Can't close streams");
        }
    }
}
