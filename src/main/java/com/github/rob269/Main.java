package com.github.rob269;

import com.github.rob269.io.ClientIO;
import com.github.rob269.io.DataBaseTable;
import com.github.rob269.logging.ConsoleFormatter;
import com.github.rob269.rsa.RSAServerKeys;
import com.github.rob269.rsa.WrongKeyException;

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
    public static final DataBaseTable RSA_KEYS = new DataBaseTable(DataBaseTable.Tables.USER_RSA_KEYS);
    public static final DataBaseTable USERS = new DataBaseTable(DataBaseTable.Tables.USERS);
    public static final DataBaseTable MESSAGES = new DataBaseTable(DataBaseTable.Tables.USER_MESSAGES);
    public volatile static Set<String> usersOnline = new HashSet<>();
    public volatile static Set<String> needToCheckMessages = new HashSet<>();
    private static boolean isOnline = true;
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
        try (ServerSocket serverSocket = new ServerSocket(5099)) {
            LOGGER.info("The server is running");
            while (isOnline) {
                Socket clientSocket = serverSocket.accept();
                ConnectionThread thread = new ConnectionThread(clientSocket);
                thread.setName("MainConnectionThread-" + thread.getName().substring(7));
                thread.start();
            }
        } catch (IOException e) {
            LOGGER.warning("Server can't start\n" + ConsoleFormatter.formatStackTrace(e));
        }
    }

    protected static void disableServer() {
        isOnline = false;
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
        } catch (WrongKeyException e) {
            LOGGER.warning("Wrong Key\n" + ConsoleFormatter.formatStackTrace(e));
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