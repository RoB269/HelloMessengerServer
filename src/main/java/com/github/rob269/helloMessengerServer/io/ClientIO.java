package com.github.rob269.helloMessengerServer.io;

import com.github.rob269.helloMessengerServer.Chat;
import com.github.rob269.helloMessengerServer.Main;
import com.github.rob269.helloMessengerServer.Message;
import com.github.rob269.helloMessengerServer.SideConnectionThread;
import com.github.rob269.helloMessengerServer.logging.ConsoleFormatter;
import com.github.rob269.helloMessengerServer.rsa.Key;
import com.github.rob269.helloMessengerServer.rsa.RSA;
import com.github.rob269.helloMessengerServer.rsa.RSAServerKeys;
import com.github.rob269.helloMessengerServer.rsa.UserKey;

import java.io.*;
import java.math.BigInteger;
import java.net.Socket;
import java.net.SocketException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.logging.Logger;

public class ClientIO {
    private static final int MAX_MESSAGE_COUNT_IN_REQUEST = 100;
    public static final Map<Byte, String> commands = new HashMap<>();
    static {
        commands.put((byte) -10, "Sending new message");
        commands.put((byte) 0, "Exit");
        commands.put((byte) 99, "Exit");
        commands.put((byte) 10, "Get Server public key");
        commands.put((byte) 20, "User sending his public key");
        commands.put((byte) 21, "Login");
        commands.put((byte) 30, "Check initialization");
        commands.put((byte) 50, "OK");
        commands.put((byte) 51, "Sending server public key");
        commands.put((byte) 52, "User successfully authenticated");
        commands.put((byte) 53, "Sending chats");
        commands.put((byte) 54, "Chat was created");
        commands.put((byte) 55, "Message is successfully sent");
        commands.put((byte) 56, "Sending messages from database");
        commands.put((byte) 57, "You joined the chat");
        commands.put((byte) 60, "Error");
        commands.put((byte) 61, "Authentication error");
        commands.put((byte) 62, "Chat already exist");
        commands.put((byte) 63, "Wrong chat name");
        commands.put((byte) 64, "The user does not exist");
        commands.put((byte) 65, "Forbidden symbol");
        commands.put((byte) 66, "Message too long");
        commands.put((byte) 67, "Chat doesn't exist");
        commands.put((byte) 68, "User is not in the chat");
        commands.put((byte) 69, "Chat is blocked");
        commands.put((byte) 70, "User blocked in this chat");
        commands.put((byte) 71, "Wrong params");
        commands.put((byte) 72, "User already in the chat");
        commands.put((byte) 80, "Get chats");
        commands.put((byte) 81, "Create new private chat");
        commands.put((byte) 82, "Create new public chat");
        commands.put((byte) 83, "Send message");
        commands.put((byte) 84, "Get messages");
        commands.put((byte) 85, "Add user to the chat");
        commands.put((byte) 86, "Join the chat");
        commands.put((byte) 90, "Ping");    
    }
    private Socket clientSocket;
    private DataOutputStream dos;
    private DataInputStream dis;
    private Key clientKey;
    private boolean isClosed = false;
    private boolean initialized = false;
    private String username = null;
    private long userId;
    private long handshakeTimer;
    private InputRouter router;

    private static final Logger LOGGER = Logger.getLogger(ClientIO.class.getName());

    public ClientIO(Socket clientSocket) {
        try {
            this.clientSocket = clientSocket;
            dos = new DataOutputStream(clientSocket.getOutputStream());
            dis = new DataInputStream(clientSocket.getInputStream());
            handshakeTimer = System.currentTimeMillis();
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

    public void init() throws IOException, SQLException {
        while (!isClosed) {
            byte request = readCommand();
            if (!isClosed) {
                if (initialized) handleRequest(request);
                else handleInitialRequest(request);
            }
        }
    }

    private void initClientKey(Key clientKey) throws IOException {
        this.clientKey = clientKey;
        LOGGER.fine("User's key was received");
        if (checkInitialization()) {
            initialized = true;
            LOGGER.info("Handshake complete in " + (System.currentTimeMillis() - handshakeTimer) + "ms");
            try {
                clientSocket.setSoTimeout(0);
                clientSocket.setKeepAlive(true);
            } catch (SocketException e) {
                LOGGER.warning("Socket exception\n" + ConsoleFormatter.formatStackTrace(e));
            }
        } else {
            LOGGER.warning("Fail initialization");
            close();
        }
    }

    public String getUsername() {
        return username;
    }


    public long getUserId() {
        return userId;
    }

    private boolean checkInitialization() throws IOException {
        byte[] bytes = RSA.encodeByteArray(new byte[]{30}, clientKey);
        write(bytes, true);
        if (readCommand() == 30) {
            write(RSA.encodeByteArray(new byte[]{50}, clientKey), true);
            return true;
        }
        return false;
    }

    private void handleRequest(byte request) throws IOException, SQLException {
        switch (request) {
            case 90 -> writeCommand(90);//Ping
            case 0, 99 -> close();//Exit
            case 21 -> {//Login
                String username = readString();
                String password = readString(false);
                if (username.contains("\\")) {writeCommand(65); return;}
                String hash = sha256(password+username);
                synchronized (ClientIO.class) {
                    List<String[]> dbLine = DatabaseInterface.sqlRead(0, 2, username);
                    if (!dbLine.isEmpty()) {
                        if (!dbLine.getFirst()[0].equals(hash)) {
                            writeCommand(61);
                            close();
                            return;
                        }
                        userId = Long.parseLong(dbLine.getFirst()[1]);
                    } else {
                        DatabaseInterface.sqlWrite(1, username, hash);
                        dbLine = DatabaseInterface.sqlRead(2, 1, username);
                        userId = Long.parseLong(dbLine.getFirst()[0]);
                    }
                }
                this.username = username;
                router.setName("InputThread{" + username+ "}");
                Thread.currentThread().setName("MainConnectionThread{" + username + "}");
                SideConnectionThread thread = new SideConnectionThread(this);
                thread.setName("SideConnectionThread{" + username + "}");
                thread.start();
                Main.onlineUsersThreads.put(username, thread);
                DatabaseInterface.sqlWrite(16, username);
                writeCommand(52);
            }
            case 80 -> {//Get chats
                if (username == null) {writeCommand(61); return;}
                //Get private chats
                List<String[]> privateChats = DatabaseInterface.sqlRead(3, 7, userId, userId, userId);
                //Get public chats
                List<String[]> publicChats = DatabaseInterface.sqlRead(4, 7, userId);
                StringBuilder privateChatsBuilder = new StringBuilder();
                for (String[] chat : privateChats) {
                    chat[2] = chat[2].substring(0, 1);
                    if (chat[3] == null) {
                        chat[3] = "";
                        chat[4] = "";
                        chat[5] = "";
                        chat[6] = "";
                    }
                    else {
                        if (chat[4].equals(String.valueOf(userId)))
                            chat[4] = username;
                        else
                            chat[4] = chat[1];
                        chat[6] = chat[6].replaceAll("\\\\", "\\\\&");
                    }
                    for (int i = 0; i < 7; i++)
                        privateChatsBuilder.append(chat[i]).append("\\\\");
                }
                privateChatsBuilder.append(";");
                StringBuilder publicChatsBuilder = new StringBuilder();
                for (String[] chat : publicChats) {
                    chat[2] = chat[2].substring(0, 1);
                    if (chat[3] == null) {
                        chat[3] = "";
                        chat[4] = "";
                        chat[5] = "";
                        chat[6] = "";
                    }
                    else chat[6] = chat[6].replaceAll("\\\\", "\\\\&");
                    for (int i = 0; i < 7; i++) {
                        publicChatsBuilder.append(chat[i]).append("\\\\");
                    }
                }
                publicChatsBuilder.append(";");
                HMPBatch batch = writeBatch(53, 2, false);
                batch.write(privateChatsBuilder.toString());
                batch.write(publicChatsBuilder.toString());
            }
            case 81 -> {//Create new private chat todo connect to exist chat
                String recipient = readString(false);
                if (username == null) {writeCommand(61); return;}
                if (recipient.isEmpty()) {writeCommand(63); return;}
                if (recipient.contains("\\")) {writeCommand(65); return;}
                if (recipient.equals(username)) {writeCommand(60); return;}
                List<String[]> dbLine = DatabaseInterface.sqlRead(2, 1, recipient);
                if (dbLine.isEmpty()) {writeCommand(64); return;}
                long dbRecipientId = Long.parseLong(dbLine.getFirst()[0]);
                long chatId;
                LocalDateTime localDateTime;
                synchronized (ClientIO.class) {
                    List<String[]> contact = DatabaseInterface.sqlRead(5, 1, dbRecipientId, userId, userId, dbRecipientId);
                    if (!contact.isEmpty()) {
                        writeCommand(62);
                        return;
                    }
                    chatId = DatabaseInterface.createChat();
                    DatabaseInterface.sqlWrite(6, userId, chatId, "ok");
                    DatabaseInterface.sqlWrite(7, dbRecipientId, chatId);
                    DatabaseInterface.sqlWrite(8, userId, dbRecipientId, chatId);
                    localDateTime = roundDateTime(LocalDateTime.now());
                    DatabaseInterface.sqlWrite(11, (long) 0, chatId, (long) -1, "", localDateTime.toString());
                }
                HMPBatch batch = writeBatch(54, 1, false);
                batch.write(chatId + "\\\\" + localDateTime + "\\\\;");
                if (Main.onlineUsersThreads.containsKey(recipient)) {
                    Main.onlineUsersThreads.get(recipient).newChat(new Chat(chatId, username,
                            new Message(chatId, 0, "null", localDateTime, ""), true));
                }
            }
            case 82 -> {//Create new public chat
                String name = readString(false);
                if (username == null) {writeCommand(61); return;}
                if (name.isEmpty()) {writeCommand(63); return;}
                if (name.contains("\\")) {writeCommand(65); return;}
                long chatId = DatabaseInterface.createChat(name, userId);
                DatabaseInterface.sqlWrite(6, userId, chatId, "ok");
                LocalDateTime localDateTime = roundDateTime(LocalDateTime.now());
                DatabaseInterface.sqlWrite(11, (long) 0, chatId, (long) -1, "", localDateTime.toString());
                HMPBatch batch = writeBatch(54, 1, false);
                batch.write(chatId + "\\\\" + localDateTime + "\\\\;");
            }
            case 83 -> {//Send message
                long chatId = readLong(false);
                String message = readString(false);
                if (username == null) {writeCommand(61); return;}
                List<String[]> dbLine = DatabaseInterface.sqlRead(9, 1, userId, chatId);
                if (dbLine.isEmpty()) {
                    dbLine = DatabaseInterface.sqlRead(10,1,chatId);
                    if (dbLine.isEmpty()) {
                        writeCommand(67);
                        return;
                    }
                    writeCommand(68);
                    return;
                }
                if (dbLine.getFirst()[0].equals("block")) {writeCommand(69); return;}
                LocalDateTime localDateTime;
                long messageId;
                synchronized (ClientIO.class) {
                    dbLine = DatabaseInterface.sqlRead(10, 1,chatId);
                    messageId = Long.parseLong(dbLine.getFirst()[0]) + 1;
                    localDateTime = roundDateTime(LocalDateTime.now());
                    DatabaseInterface.sqlWrite(11, messageId, chatId, userId, message, localDateTime.toString());
                    DatabaseInterface.sqlWrite(12, messageId, chatId);
                }
                String messageMeta = messageId + "\\\\" + localDateTime;
                HMPBatch batch = writeBatch(55, 1, false);
                batch.write(messageMeta);
                dbLine = DatabaseInterface.sqlRead(17, 1, chatId, userId);
                for (String[] user : dbLine) {
                    if (Main.onlineUsersThreads.containsKey(user[0])) {
                        SideConnectionThread thread = Main.onlineUsersThreads.get(user[0]);
                        thread.newMessage(new Message(chatId, messageId, username, localDateTime, message));
                    }
                }
            }
            case 84 -> {//Get messages
                long chatId = readLong(false);
                String[] params = readString().split("\\\\");
                if (username == null) {writeCommand(61); return;}
                List<String[]> chatConnection = DatabaseInterface.sqlRead(9, 1, userId, chatId);
                if (chatConnection.isEmpty()) {writeCommand(68); return;}
                List<String[]> entries;
                try {
                    int messageCount = Integer.parseInt(params[1]);
                    if (messageCount > MAX_MESSAGE_COUNT_IN_REQUEST) messageCount = MAX_MESSAGE_COUNT_IN_REQUEST;
                    entries = DatabaseInterface.sqlRead(15, 4, chatId, Long.parseLong(params[0]), chatId, userId, messageCount);
                } catch (RuntimeException e) {
                    writeCommand(71);
                    return;
                }
                StringBuilder builder = new StringBuilder();
                for (String[] entry : entries) {
                    entry[3] = entry[3].replaceAll("\\\\", "\\\\&");
                    for (int i = 0; i < 4; i++)
                        builder.append(entry[i]).append("\\\\");
                }
                builder.append(";");
                HMPBatch batch = writeBatch(56, 1, false);
                batch.write(builder.toString());
            }
            case 85 -> {//Add user to the chat
                long chatId = readLong(false);
                String username = readString();

            }
            case 86 -> {//Join the chat
                long chatId = readLong(false);
                List<String[]> dbLines = DatabaseInterface.sqlRead(9, 1, userId, chatId);
                if (!dbLines.isEmpty()) {writeCommand(72); return;}
                dbLines = DatabaseInterface.sqlRead(18, 5, chatId);
                if (dbLines.isEmpty()) {writeCommand(67); return;}
                DatabaseInterface.sqlWrite(6, userId, chatId, "ok");
                StringBuilder builder = new StringBuilder();
                String[] chat = dbLines.getFirst();
                chat[4] = chat[4].replaceAll("\\\\", "\\\\&");
                for (String string : chat) {
                    builder.append(string).append("\\\\");
                }
                builder.append(";");
                HMPBatch batch = writeBatch(57, 1, false);
                batch.write(builder.toString());
            }
        }
    }

    private LocalDateTime roundDateTime(LocalDateTime localDateTime) {
        return localDateTime.getNano()<500000000L ? localDateTime.withNano(0) : localDateTime.withNano(0).plusSeconds(1);
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

    private void handleInitialRequest(byte request) throws IOException {
        switch (request) {
            case 10 -> {//Get rsa keys
                UserKey userKey = RSAServerKeys.getPublicKey();
                BigInteger[] key = userKey.getKey();
                BigInteger[] meta = userKey.getMeta();
                HMPBatch batch = writeBatch(51, 5, true);
                for (BigInteger integer : key) batch.write(integer);
                for (BigInteger integer : meta) batch.write(integer);
                batch.write(userKey.getUser().getUsername());
            }
            case 20 -> {//Key
                BigInteger[] key = new BigInteger[2];
                for (int i = 0; i < 2; i++) key[i] = readBigint();
                Key clientKey = new Key(key);
                initClientKey(clientKey);
            }
            case 0 -> close();
        }
    }

    public long readLong(boolean log) throws IOException {
        byte[] bytes = read();
        long val = 0;
        for (byte b : bytes) {
            val = val << 8;
            val = (val | (b & 0xff));
        }
        if (log) LOGGER.finer("Get message:\n" + val);
        return val;
    }

    public String readString() throws IOException {
        return readString(true);
    }

    public String readString(boolean log) throws IOException {
        byte[] bytes = read();
        if (bytes != null){
            String string = new String(bytes);
            if (log) LOGGER.finer("Get message:\n" + string);
            return string;
        }
        return null;
    }

    public byte readCommand() throws IOException {
        byte[] bytes = read();
        if (bytes != null) {
            byte command = bytes[0];
            LOGGER.finer("Get command: " + command + " [R:" + commands.get(command) + "]");
            return command;
        }
        return 0;
    }

    public BigInteger readBigint() throws IOException {
        byte[] bytes = read();
        if (bytes != null) {
            BigInteger bigint = new BigInteger(bytes);
            LOGGER.finer("Get message:\n" + bigint);
            return bigint;
        }
        return null;
    }

    public byte[] read() throws IOException {
        byte[] result = null;
        LOGGER.finest("Reading bytes");
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
        if (isClosed) throw new IOException();
        return result;
    }

    public HMPBatch writeBatch(int command, int batchSize, boolean log) {
        return new HMPBatch((byte) command, this, batchSize, log);
    }

    public void writeCommand(int message) throws IOException{
        writeCommand(message, 0);
    }

    public void writeCommand(int message, int packageCount) throws IOException {
        if (packageCount == 0) write(new byte[]{(byte) message}, true);
        else {
            byte[] command = intToByteArray(packageCount);
            command[0] = (byte) message;
            write(command, true);
        }
        LOGGER.finer("Write command: " + message + " [W:" + commands.get((byte) message) + "]");
    }

    private static byte[] intToByteArray(int integer) {
        byte[] bytes = new byte[4];
        int i;
        for (i = 3; i >= 0 && integer != 0; i--) {
            bytes[i] = (byte) integer;
            integer >>>= 8;
        }
        byte[] toReturn = new byte[4-i];
        System.arraycopy(bytes, i+1, toReturn, 1, toReturn.length-1);
        return toReturn;
    }

    protected synchronized void write(byte[] message, boolean sendCommand) throws IOException {
        if (!isClosed) {
            LOGGER.finest("Sending byte message");
            try {
                if (initialized) message = RSA.encodeByteArray(message, clientKey);

                if (sendCommand) dos.writeByte(message.length);
                else dos.writeInt(message.length);

                dos.write(message);
                dos.flush();
            } catch (IOException e) {
                LOGGER.warning("Can't send the message\n" + ConsoleFormatter.formatStackTrace(e));
            }
            return;
        }
        throw new IOException();
    }

    public void close() {
        isClosed = true;
        if (username != null) {
            Main.onlineUsersThreads.remove(username).interrupt();
        }
        try {
            dis.close();
            dos.close();
            router.close();
            LOGGER.fine("ClientIO closed");
        } catch (IOException e) {
            LOGGER.warning("ClientIO closing exception\n" + ConsoleFormatter.formatStackTrace(e));
        }
    }
}

/*
Handshake codes:
Requests:
     0 - Exit
    10 - Get Server public key -
    20 - User sending his public key +

Responses:
    50 - OK-
    51 - Sending server public key+
    52 - AUTHENTICATED-
    60 - Error-
    61 - Key is rejected(already exist)-
    61 - AUTHENTICATION ERROR-
    30 - Check initialization

Codes:
Requests:
    21 - Login
    80 - Get chats
    81 - Create new private chat
    82 - Create new public chat
    83 - Send message
    90 - Ping
    99 - Exit
Responses:
    50 - OK
    53 - Sending chats
    54 - Chat created
    60 - Error
    61 - AUTHENTICATION ERROR-
    62 - Chat already exist
    63 - Wrong name
    64 - User isn't exist
    65 - Forbidden symbol
    66 - Message too long
    67 - Chat doesn't exist
    68 - User is not in the chat
    69 - Chat is blocked
    70 - User blocked in this chat
    71 - Wrong params
*/