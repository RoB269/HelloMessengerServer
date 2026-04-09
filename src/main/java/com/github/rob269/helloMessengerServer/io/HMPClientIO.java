package com.github.rob269.helloMessengerServer.io;

import com.github.rob269.helloMessengerServer.*;
import com.github.rob269.helloMessengerServer.logging.LogFormatter;
import com.github.rob269.helloMessengerServer.rsa.Key;
import com.github.rob269.helloMessengerServer.rsa.RSA;
import com.github.rob269.helloMessengerServer.rsa.RSAServerKeys;
import com.github.rob269.helloMessengerServer.rsa.UserKey;

import java.io.*;
import java.math.BigInteger;
import java.net.Socket;
import java.net.SocketException;
import java.sql.SQLException;
import java.util.*;
import java.util.logging.Logger;

public class HMPClientIO implements ClientIO {
    public static final Map<Byte, String> commands = new HashMap<>();
    static {
        commands.put((byte) -10, "Sending new message");
        commands.put((byte) -11, "Sending new chat");
        commands.put((byte) 0, "Disconnect");
        commands.put((byte) 99, "Disconnect");
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
    private long handshakeTimer;
    private HMPInputRouter router;

    private static final Logger LOGGER = Logger.getLogger(HMPClientIO.class.getName());

    public HMPClientIO(Socket clientSocket) {
        LOGGER.info("Using HMP");
        try {
            this.clientSocket = clientSocket;
            dos = new DataOutputStream(clientSocket.getOutputStream());
            dis = new DataInputStream(clientSocket.getInputStream());
            handshakeTimer = System.currentTimeMillis();
            router = new HMPInputRouter(dis, this);
            router.setName("InputThread-" + router.getName().substring(7));
            router.start();
            LOGGER.fine("Output and Input streams is open");
        } catch (IOException e) {
            LOGGER.warning("Can't open streams\n" + LogFormatter.formatStackTrace(e));
        }
    }

    public boolean isInitialized() {
        return initialized;
    }

    @Override
    public void init() throws IOException, SQLException {
        Endpoints endpoints = new Endpoints(this);
        while (!isClosed) {
            byte request = readCommand();
            if (!isClosed) {
                if (initialized) endpoints.handleRequest(request);
                else handleInitialRequest(request);
            }
        }
    }

    private void handleInitialRequest(byte request) throws IOException {
        switch (request) {
            case 10 -> {//Get rsa keys
                UserKey userKey = RSAServerKeys.getPublicKey();
                BigInteger[] key = userKey.getKey();
                BigInteger[] meta = userKey.getMeta();
                Batch batch = writeBatch(51, 5, true);
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

    @Override
    public String getUsername() {
        return username;
    }

    public void initClientKey(Key clientKey) throws IOException {
        this.clientKey = clientKey;
        LOGGER.fine("User's key was received");
        if (checkInitialization()) {
            initialized = true;
            LOGGER.info("Handshake complete in " + (System.currentTimeMillis() - handshakeTimer) + "ms");
            try {
                clientSocket.setSoTimeout(0);
                clientSocket.setKeepAlive(true);
            } catch (SocketException e) {
                LOGGER.warning("Socket exception\n" + LogFormatter.formatStackTrace(e));
            }
        } else {
            LOGGER.warning("Fail initialization");
            close();
        }
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

    @Override
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

    @Override
    public String readString(boolean log) throws IOException {
        byte[] bytes = read();
        if (bytes != null){
            String string = new String(bytes);
            if (log) LOGGER.finer("Get message:\n" + string);
            return string;
        }
        return null;
    }

    @Override
    public void setUsername(String username) {
        this.username = username;
        router.setName("InputThread{" + username+ "}");
    }

    @Override
    public byte readCommand() throws IOException {
        byte[] bytes = read();
        if (bytes != null) {
            byte command = bytes[0];
            LOGGER.finer("Get command: " + command + " [R:" + commands.get(command) + "]");
            return command;
        }
        return 0;
    }

    @Override
    public BigInteger readBigint() throws IOException {
        byte[] bytes = read();
        if (bytes != null) {
            BigInteger bigint = new BigInteger(bytes);
            LOGGER.finer("Get message:\n" + bigint);
            return bigint;
        }
        return null;
    }

    byte[] read() throws IOException {
        byte[] result = null;
        LOGGER.finest("Reading bytes");
        if (Thread.currentThread().getName().startsWith("Main")) {
            synchronized (router.mainThreadInput) {
                while (router.mainThreadInput.isEmpty() && !isClosed) {
                    try {
                        router.mainThreadInput.wait();
                    } catch (InterruptedException e) {
                        LOGGER.warning("Threads exception\n" + LogFormatter.formatStackTrace(e));
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
                        LOGGER.warning("Threads exception\n" + LogFormatter.formatStackTrace(e));
                    }
                }
                result = router.sideThreadInput.poll();
            }
        }
        if (isClosed) throw new IOException();
        return result;
    }

    @Override
    public HMPBatch writeBatch(int command, int batchSize, boolean log) {
        return new HMPBatch((byte) command, this, batchSize, log);
    }

    @Override
    public void writeCommand(int message) throws IOException{
        writeCommand(message, 0);
    }

    @Override
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

    synchronized void write(byte[] message, boolean sendCommand) throws IOException {
        if (!isClosed) {
            LOGGER.finest("Sending byte message");
            try {
                if (initialized) message = RSA.encodeByteArray(message, clientKey);
                if (sendCommand) dos.writeByte(message.length);
                else dos.writeInt(message.length);

                dos.write(message);
                dos.flush();
            } catch (IOException e) {
                LOGGER.warning("Can't send the message\n" + LogFormatter.formatStackTrace(e));
            }
            return;
        }
        throw new IOException();
    }

    @Override
    public boolean isClosed() {
        return isClosed;
    }

    @Override
    public void close() {
        isClosed = true;
        try {
            dis.close();
            dos.close();
            router.close();
            LOGGER.fine("ClientIO closed");
        } catch (IOException e) {
            LOGGER.warning("ClientIO closing exception\n" + LogFormatter.formatStackTrace(e));
        }
    }
}

/*
Handshake codes:
Requests:
     0 - Disconnect
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
    99 - Disconnect
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