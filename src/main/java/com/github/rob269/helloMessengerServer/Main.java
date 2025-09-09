package com.github.rob269.helloMessengerServer;

import com.github.rob269.helloMessengerServer.io.ClientIO;
import com.github.rob269.helloMessengerServer.io.DatabaseInterface;
import com.github.rob269.helloMessengerServer.io.ResourcesIO;
import com.github.rob269.helloMessengerServer.logging.ConsoleFormatter;
import com.github.rob269.helloMessengerServer.logging.LogFilter;
import com.github.rob269.helloMessengerServer.rsa.Guarantor;
import com.github.rob269.helloMessengerServer.rsa.Key;
import com.github.rob269.helloMessengerServer.rsa.RSAServerKeys;

import java.io.*;
import java.math.BigInteger;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.sql.SQLException;
import java.util.*;
import java.util.logging.LogManager;
import java.util.logging.Logger;

//Server
public class Main {
    public volatile static Map<String, SideConnectionThread> onlineUsersThreads = new HashMap<>();
    private static ServerSocket serverSocket;
    static {
        System.setErr(new PrintStream(new LogFilter(System.err)));
        File logsDir = new File("log/");
        if (!logsDir.exists()) {
            if (!logsDir.mkdir()) {
                throw new RuntimeException();
            }
        }
        try {
            LogManager.getLogManager().readConfiguration(Objects.requireNonNull(Main.class.getResource("log.properties")).openStream());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
    private static final Logger LOGGER = Logger.getLogger(Main.class.getName());

    private static void parseConfigFile() throws SQLException {
        if (!ResourcesIO.isExist("config")) {
            ResourcesIO.write("config", new ArrayList<>());
            LOGGER.severe("The configuration file does not exists");
            throw new RuntimeException();
        }
        BigInteger[] publicKey = null;
        BigInteger[] privateKey = null;
        String dbRootPassword = null;
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
                dbRootPassword = config.split("=")[1];
            }
        }
        if (publicKey != null && privateKey != null && dbRootPassword != null) {
            DatabaseInterface.init(dbRootPassword);
            Guarantor.init(new Key(publicKey), new Key(privateKey));
        }
        else {
            LOGGER.severe("The configuration file does not contain the necessary data");
            throw new RuntimeException();
        }
    }

    public static void main(String[] args) {
        try {
            parseConfigFile();
        } catch (SQLException e) {
            LOGGER.warning("Can't connect to database");
            throw new RuntimeException(e);
        }
        RSAServerKeys.initKeys();
        try {
            serverSocket = new ServerSocket(5099);
            LOGGER.warning("The server is running");
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
    Socket clientSocket;

    private static final Logger LOGGER = Logger.getLogger(ConnectionThread.class.getName());

    public ConnectionThread(Socket clientSocket) {
        this.clientSocket = clientSocket;
    }

    @Override
    public void run() {
        LOGGER.info(clientSocket.getInetAddress().toString() + " connected");
        ClientIO clientIO = null;
        try {
            clientSocket.setSoTimeout(3_000);
            clientIO = new ClientIO(clientSocket);
            clientIO.init();
        } catch (SocketException e) {
            LOGGER.warning("Time out exception");
        } catch (IOException _) {
        } catch (SQLException e) {
            throw new RuntimeException(e);
        } finally {
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