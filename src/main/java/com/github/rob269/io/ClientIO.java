package com.github.rob269.io;

import com.github.rob269.Main;
import com.github.rob269.Message;
import com.github.rob269.User;
import com.github.rob269.logging.ConsoleFormatter;
import com.github.rob269.rsa.*;

import java.io.*;
import java.math.BigInteger;
import java.net.Socket;
import java.net.SocketException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ClientIO {
    private Socket clientSocket;
    private DataOutputStream dos;
    private DataInputStream dis;
    private Key clientKey;
    private boolean isClosed = false;
    private boolean initialized = false;
    private String userId = null;
    private long handshakeTimer;
    private InputRouter router;

    private static final Logger LOGGER = Logger.getLogger(ClientIO.class.getName());

    public ClientIO(Socket clientSocket, long handshakeTimer) {
        try {
            this.clientSocket = clientSocket;
            dos = new DataOutputStream(clientSocket.getOutputStream());
            dis = new DataInputStream(clientSocket.getInputStream());
            this.handshakeTimer = handshakeTimer;
            router = new InputRouter(dis, this);
            router.setName("InputThread-" + router.getName().substring(7));
            router.start();
            LOGGER.fine("Output and Input streams is open");
        } catch (IOException e) {
            LOGGER.warning("Can't open streams\n" + ConsoleFormatter.formatStackTrace(e));
        }
    }

    public boolean isInitialized() {
        return initialized;
    }

    public boolean isClosed() {
        return isClosed;
    }

    public void init() throws WrongKeyException {
        while (!isClosed) {
            byte request = readCommand();
            if (!isClosed) {
                if (!initialized) {
                    handleInitialRequest(request);
                } else {
                    handleRequest(request);
                }
            }
        }
    }

    private void initClientKey(UserKey clientKey) throws WrongKeyException{
        if (clientKey.isAuthenticated()) {
            this.clientKey = clientKey;
            LOGGER.info("User key is approved");
            if (checkInitialization()) {
                initialized = true;
                LOGGER.warning("Handshake complete in " + (System.currentTimeMillis() - handshakeTimer) + "ms");
                try {
                    clientSocket.setSoTimeout(0);
                    clientSocket.setKeepAlive(true);
                } catch (SocketException e) {
                    LOGGER.warning("Socket exception\n" + ConsoleFormatter.formatStackTrace(e));
                }
            }
            else {
                LOGGER.warning("Fail initialization");
                close();
            }
        }
        else {
            writeCommand(63);
            throw new WrongKeyException("Wrong key");
        }
    }

    private boolean checkInitialization() {
        byte[] bytes = RSA.encodeByteArray(new byte[]{30}, clientKey);
        write(bytes, false);
        if (readCommand() == 30) {
            write(RSA.encodeByteArray(new byte[]{50}, clientKey), false);
            return true;
        }
        return false;
    }

    private void handleRequest(byte request) {
        switch (request) {
            case 90 -> writeCommand(90);//Ping
            case 99 -> close();//Exit
            case 23 -> {//Login
                String userId = readString();
                String password = readString();
                String hash = sha256(password+userId);
                List<String> dbLine = Main.USERS.readLine(1, userId);
                if (!dbLine.isEmpty()) {
                    if (!dbLine.get(2).equals(hash)) {
                        writeCommand(62);
                        close();
                        return;
                    }
                }
                else {
                    String s = "INSERT INTO %s (user_id, password) VALUES ('%s', '%s');"
                            .formatted(DataBaseTable.Tables.USERS.toString(), userId, hash);
                    System.out.println(s);
                    Main.USERS.sqlWrite(s);
                }
                this.userId = userId;
                router.setName("InputThread{" + this.userId + "}");
                Thread.currentThread().setName("MainConnectionThread{" + this.userId + "}");
                Main.usersOnline.add(this.userId);
                writeCommand(53);
            }
        }
    }

    @Deprecated
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

    @Deprecated
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

    private static String sha256(String string) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(string.getBytes(StandardCharsets.UTF_8));
            StringBuilder builder = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) builder.append("0");
                builder.append(hex);
            }
            return builder.toString();
        } catch (NoSuchAlgorithmException e) {
            LOGGER.warning("SHA-256 algorithm exception:\n" + ConsoleFormatter.formatStackTrace(e));
        }
        return null;
    }

    private void handleInitialRequest(byte request) throws WrongKeyException {
        switch (request) {
            case 10 -> {//Get rsa keys
                writeCommand(52);
                UserKey userKey = RSAServerKeys.getPublicKey();
                BigInteger[] key = userKey.getKey();
                BigInteger[] meta = userKey.getMeta();
                writePackageCount(5);
                for (BigInteger integer : key) write(integer);
                for (BigInteger integer : meta) write(integer);
                write(userKey.getUser().getId());
            }
            case 20 -> {//Key
                BigInteger[] key = new BigInteger[2];
                BigInteger[] meta = new BigInteger[2];
                for (int i = 0; i < 2; i++) key[i] = readBigint();
                for (int i = 0; i < 2; i++) meta[i] = readBigint();
                User user = new User(new String(RSA.decodeByteArray(read(), RSAServerKeys.getPrivateKey())));
                UserKey clientKey = new UserKey(key, user);
                clientKey.setMeta(meta);
                initClientKey(clientKey);
            }
            case 21 -> {//Register new key
                BigInteger[] key = new BigInteger[2];
                for (int i = 0; i < 2; i++) key[i] = readBigint();
                String user = RSA.decodeByteToString(read(), RSAServerKeys.getPrivateKey());
                UserKey newKey = new UserKey(key, new User(user));
                if (!RSAKeysPair.isIdentified(newKey)) {
                    UserKey keyToReturn;
                    RSAKeysPair.registerNewKey(newKey, false);
                    keyToReturn = UserKey.getFromDatabase(newKey.getUser().getId());
                    if (keyToReturn == null) {
                        LOGGER.warning("Key is null");
                        writeCommand(60);
                        return;
                    }
                    writeCommand(51);
                    writePackageCount(2);
                    for (int i = 0; i < 2; i++) write(keyToReturn.getMeta()[i]);
                } else {
                    LOGGER.warning("Key is already registered");
                    writeCommand(61);
                }
            }
            case 22 -> {//Reset
                String user = RSA.decodeByteToString(read(), RSAServerKeys.getPrivateKey());
                String password = RSA.decodeByteToString(read(), RSAServerKeys.getPrivateKey());
                List<String> dbResponse = Main.USERS.readLine(1, user);
                String hash = sha256(password+user);
                if ((!dbResponse.isEmpty()) && (!dbResponse.get(2).equals(hash))){
                    writeCommand(62);
                    close();
                    return;
                }
                Main.RSA_KEYS.remove(4, user);
                writeCommand(50);
            }
            case 0 -> close();
        }
    }

    public String readString() {
        byte[] bytes = read();
        if (bytes != null){
            String string = new String(bytes);
            LOGGER.finer("Get message:\n" + string);
            return string;
        }
        return null;
    }

    public byte readCommand() {
        byte[] bytes = read();
        if (bytes != null) {
            byte command = bytes[0];
            LOGGER.finer("Get command:\n" + command);
            return command;
        }
        return 0;
    }

    public BigInteger readBigint() {
        byte[] bytes = read();
        if (bytes != null) {
            BigInteger bigint = new BigInteger(bytes);
            LOGGER.finer("Get message:\n" + bigint);
            return bigint;
        }
        return null;
    }

    public byte[] read() {
        byte[] result = null;
        if (Thread.currentThread().getName().startsWith("Main")) {
            synchronized (router.mainThreadInput) {
                while (router.mainThreadInput.isEmpty() && !isClosed) {
                    try {
                        router.mainThreadInput.wait();
                    } catch (InterruptedException e) {
                        LOGGER.warning("Threads exception\n" + ConsoleFormatter.formatStackTrace(e));
                    }
                }
                result = router.mainThreadInput.poll();
            }
        }
        else if (Thread.currentThread().getName().startsWith("Side")){
            synchronized (router.sideThreadInput) {
                while (router.sideThreadInput.isEmpty() && !isClosed) {
                    try {
                        router.sideThreadInput.wait();
                    } catch (InterruptedException e) {
                        LOGGER.warning("Threads exception\n" + ConsoleFormatter.formatStackTrace(e));
                    }
                }
                result = router.sideThreadInput.poll();
            }
        }
        return result;
    }

    public void write(String message) {
        write(message, Level.FINER);
    }

    public void write(String message, Level loggingLevel) {
        if (!isClosed){
            write(message.getBytes());
            LOGGER.log(loggingLevel, "Message sent:\n" + message);
        }
    }

    public void write(BigInteger message) {
        LOGGER.finer("Write bigint:\n" + message);
        write(message.toByteArray());
    }

    public void writeCommand(int message) {
        LOGGER.finer("Write command: " + message);
        write(new byte[]{(byte) message}, false);
    }

    public void writePackageCount(int count) {
        if (!isClosed) {
            try {
                dos.writeInt(count);
                dos.flush();
            } catch (IOException e) {
                LOGGER.warning("Can't send the message\n" + ConsoleFormatter.formatStackTrace(e));
            }
        }
    }

    public void write(byte[] message) {
        write(message, true);
    }

    public void write(byte[] message, boolean sendPackageSize) {
        if (!isClosed) {
            LOGGER.finest("Sending byte message");
            try {
                if (initialized) message = RSA.encodeByteArray(message, clientKey);
                if (sendPackageSize) dos.writeInt(message.length);
                dos.write(message);
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
            router.close();
            LOGGER.info("ClientIO closed");
        } catch (IOException e) {
            LOGGER.warning("Can't close streams");
        }
    }
}

/*
Handshake codes:
Requests:
     0 - Exit
    10 - Get Server public key -
    20 - User sending his public key +
    21 - User registering new key +
    22 - User resets the key +

Responses:
    50 - OK-
    51 - Sending meta+
    52 - Sending server public key+
    53 - AUTHENTICATED-
    60 - Error-
    61 - Key is rejected(already exist)-
    62 - AUTHENTICATION ERROR-
    63 - Wrong key-
    30 - Check initialization

Codes:
Requests:
    23 - Login
    90 - Ping
    0 - Exit
Responses:
    50 - OK
    60 - Error
*/