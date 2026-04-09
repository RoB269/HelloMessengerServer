package com.github.rob269.helloMessengerServer;

import com.github.rob269.helloMessengerServer.io.ClientIO;
import com.github.rob269.helloMessengerServer.io.HMPClientIO;
import com.github.rob269.helloMessengerServer.io.DatabaseInterface;
import com.github.rob269.helloMessengerServer.logging.LogFormatter;
import com.github.rob269.helloMessengerServer.logging.LogFilter;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.sql.SQLException;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.logging.LogManager;
import java.util.logging.Logger;

//Server
public class Main {
    public volatile static Map<String, SideConnectionThread> onlineUsersThreads = new HashMap<>();
    public static final DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static ServerSocket serverSocket;
    private static int port = -1;
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

    public static void setPort(int val) {
        if (port == -1) port = val;
    }

    public static void main(String[] args) {
        new HMPConfig().parseConfigFiles();
        setPort(5099);
        try {
            DatabaseInterface.connect();
        } catch (SQLException e) {
            LOGGER.warning("Can't connect to database");
            throw new RuntimeException(e);
        }
        try {
            serverSocket = new ServerSocket(port);
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
            LOGGER.warning("Server can't start\n" + LogFormatter.formatStackTrace(e));
        }
    }

    public static void shutdownTheServer() {
        Thread.currentThread().interrupt();
        if (serverSocket != null && !serverSocket.isClosed()) {
            try {
                serverSocket.close();
            } catch (IOException e) {
                LOGGER.warning("Close exception\n" + LogFormatter.formatStackTrace(e));
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
            clientIO = new HMPClientIO(clientSocket);
            clientIO.init();
        } catch (SocketException e) {
            LOGGER.warning("Time out exception");
        } catch (IOException _) {
        } catch (SQLException e) {
            throw new RuntimeException(e);
        } finally {
            if (clientIO != null && !clientIO.isClosed()) clientIO.close();
            if (clientIO != null && clientIO.getUsername() != null) {
                Main.onlineUsersThreads.remove(clientIO.getUsername()).interrupt();
            }
            LOGGER.info(clientSocket.getInetAddress() + " disconnected");
            try {
                clientSocket.close();
            } catch (IOException e) {
                LOGGER.warning("Socket closing exception\n" + LogFormatter.formatStackTrace(e));
            }
        }
    }
}