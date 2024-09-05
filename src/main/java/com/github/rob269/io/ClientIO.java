package com.github.rob269.io;

import com.github.rob269.User;
import com.github.rob269.rsa.*;

import java.io.*;
import java.math.BigInteger;
import java.net.Socket;
import java.util.*;
import java.util.logging.Logger;

public class ClientIO {
    private DataOutputStream dos;
    private DataInputStream dis;
    private Key clientKey;
    private boolean isClosed = false;
    private boolean initialized = false;

    private static final Logger LOGGER = Logger.getLogger(ClientIO.class.getName());

    public ClientIO(Socket clientSocket) {
        try {
            dos = new DataOutputStream(clientSocket.getOutputStream());
            dis = new DataInputStream(clientSocket.getInputStream());
            LOGGER.fine("Output and Input streams is open");
        } catch (IOException e) {
            LOGGER.warning("Can't open streams");
            e.printStackTrace();
        }
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
            handleRequest(request);
        }
    }

    private void initClientKey(Key clientKey) throws WrongKeyException{
        if (RSAKeys.isIdentified(clientKey) && clientKey.isAuthenticated()) {
            this.clientKey = clientKey;
            LOGGER.info("User key is approved");
            if (checkInitialization()) {
                initialized = true;
                LOGGER.info("YEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEE");
                close();//todo
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

    private void handleRequest(String request) throws WrongKeyException {
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
                    Key clientKey = new Key(keyInteger, user);
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
                    Key newKey = new Key(new BigInteger[]{new BigInteger(lines.getFirst()), new BigInteger(lines.get(1))},
                            new User(RSA.decodeString(lines.get(2), RSAServerKeys.getPrivateKey())));
                    if (!RSAKeys.isIdentified(newKey)) {
                        Key keyToReturn;
                        RSAKeys.registerNewKey(newKey);
                        keyToReturn = ResourcesIO.readJSON("RSA/clients/" + newKey.getUser().getId() + ResourcesIO.EXTENSION, Key.class);
                        if (keyToReturn == null) {
                            LOGGER.warning("Key is null");
                        }
                        int i = 0;
                        StringBuilder message = new StringBuilder();
                        for (String line : Objects.requireNonNull(keyToReturn).toString().split("\n")) {
                            if (i < 4) {
                                message.append(line).append("\n");
                                i++;
                            } else break;
                        }
                        write(message.toString());
                    } else {
                        LOGGER.warning("Key is already registered");
                        write("KEY IS REJECTED");
                        close();
                    }
                }
                else {
                    LOGGER.warning("Wrong key format");
                    write("WRONG KEY FORMAT");
                }
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
            LOGGER.finer("Get message: " + stringBuilder);
        } catch (IOException e) {
            LOGGER.warning("Can't read lines");
            e.printStackTrace();
        }
        return lines;
    }

    public void write(String message) {
        if (message == null) {
            LOGGER.warning("Null message");
            return;
        }
        try {
            if (initialized)
                message = RSA.encodeString(message, clientKey);
            dos.writeUTF(message);
            dos.flush();
            LOGGER.finer("Message sent:\n" + message);
        } catch (IOException e) {
            LOGGER.warning("Can't send the message");
            e.printStackTrace();
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
        LOGGER.finer("Sending message:\n" + message);
        try {
            if (initialized) message = RSA.encodeString(message, clientKey);
            dos.writeUTF(message);
            dos.flush();
        } catch (IOException e) {
            LOGGER.warning("Can't send the message");
            e.printStackTrace();
        }
    }

    public void close() {
        isClosed = true;
        try {
            dis.close();
            dos.close();
        } catch (IOException e) {
            LOGGER.warning("Can't close streams");
            e.printStackTrace();
        }
    }
}
