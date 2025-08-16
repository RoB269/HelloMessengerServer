package com.github.rob269;

import com.github.rob269.io.ClientIO;
import com.github.rob269.io.DataBaseTable;
import com.github.rob269.io.ResourcesIO;
import com.github.rob269.logging.ConsoleFormatter;
import com.github.rob269.rsa.Guarantor;
import com.github.rob269.rsa.Key;
import com.github.rob269.rsa.RSAServerKeys;

import java.io.File;
import java.io.IOException;
import java.math.BigInteger;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

//Server
public class Main {
    public static DataBaseTable USERS;
    public static DataBaseTable MESSAGES;
    public volatile static Set<String> usersOnline = new HashSet<>();
    public volatile static Set<String> needToCheckMessages = new HashSet<>();
    private static ServerSocket serverSocket;
    static {
        File logsDir = new File("log/");
        if (!logsDir.exists()) {
            if (!logsDir.mkdir()) {
                throw new RuntimeException();
            }
        }
    }
    private static final Logger LOGGER = Logger.getLogger(Main.class.getName());

    private static void parseConfigFile() {
        BigInteger[] publicKey = null;
        BigInteger[] privateKey = null;
        String db_root_password = null;
        StringBuilder builder = new StringBuilder();
        for (String line : ResourcesIO.read("config")) builder.append(line);
        String[] configs = builder.toString().replaceAll(" ", "").split(";");
        for (String config : configs) {
            if (config.startsWith("guarantor_private_key")) {
                String[] key = config.split("=")[1].split(",");
                privateKey = new BigInteger[]{new BigInteger(key[0]), new BigInteger(key[1])};
            }
            else if (config.startsWith("guarantor_public_key")) {
                String[] key = config.split("=")[1].split(",");
                publicKey = new BigInteger[]{new BigInteger(key[0]), new BigInteger(key[1])};
            }
            else if (config.startsWith("db_root_password")) {
                db_root_password = config.split("=")[1];
            }
        }
        if (publicKey != null && privateKey != null && db_root_password != null) {
            DataBaseTable.init(db_root_password);
            Guarantor.init(new Key(publicKey), new Key(privateKey));
        }
        else {
            LOGGER.severe("The configuration file does not contain the necessary data");
            throw new RuntimeException();
        }
    }

    public static void main(String[] args) {
        parseConfigFile();
        RSAServerKeys.initKeys();
        try {
            serverSocket = new ServerSocket(5099);
            LOGGER.info("The server is running");
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    Socket clientSocket = serverSocket.accept();
                    ConnectionThread thread = new ConnectionThread(clientSocket);
                    thread.setName("MainConnectionThread-" + thread.getName().substring(7));
                    thread.start();
                }
                catch (SocketException e) {
                    if (Thread.currentThread().isInterrupted()) break;
                    throw e;
                }
            }
        } catch (IOException e) {
            LOGGER.warning("Server can't start\n" + ConsoleFormatter.formatStackTrace(e));
        }
    }

    protected static void shutdownTheServer() {
        Thread.currentThread().interrupt();
        if (serverSocket != null && !serverSocket.isClosed()) {
            try {
                serverSocket.close();
            } catch (IOException e) {
                LOGGER.warning("Close exception\n" + ConsoleFormatter.formatStackTrace(e));
            }
        }
    }
}

class ConnectionThread extends Thread {
    public long handshakeTimer;
    Socket clientSocket;

    private static final Logger LOGGER = Logger.getLogger(ConnectionThread.class.getName());

    public ConnectionThread(Socket clientSocket) {
        this.clientSocket = clientSocket;
        handshakeTimer = System.currentTimeMillis();
    }

    @Override
    public void run() {
        LOGGER.info(Thread.currentThread().getName() + ": " + clientSocket.getInetAddress().toString() + " connected");
        ClientIO clientIO = null;
        try {
            clientSocket.setSoTimeout((int) TimeUnit.SECONDS.toMillis(10));
            clientIO = new ClientIO(clientSocket, handshakeTimer);
            clientIO.init();
        } catch (SocketException e) {
            LOGGER.warning("Time out exception");
        } catch (IOException ignored) {
        }
        finally {
            if (clientIO != null && !clientIO.isClosed()) clientIO.close();
            LOGGER.info(clientSocket.getInetAddress() + " disconnected");
            try {
                clientSocket.close();
            } catch (IOException e) {
                LOGGER.warning("Socket closing exception\n" + ConsoleFormatter.formatStackTrace(e));
            }
        }
    }
}