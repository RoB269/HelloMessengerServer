package com.github.rob269;

import com.github.rob269.io.ClientIO;
import com.github.rob269.io.DataBaseTable;
import com.github.rob269.logging.ConsoleFormatter;
import com.github.rob269.rsa.RSAServerKeys;

import java.io.File;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

//Server
public class Main {
    public static final DataBaseTable USERS = new DataBaseTable(DataBaseTable.Tables.USERS);
    public static final DataBaseTable MESSAGES = new DataBaseTable(DataBaseTable.Tables.USER_MESSAGES);
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

    public static void main(String[] args) {
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