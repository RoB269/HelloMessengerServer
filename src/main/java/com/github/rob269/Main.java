package com.github.rob269;

import com.github.rob269.io.ClientIO;
import com.github.rob269.io.DataBaseIO;
import com.github.rob269.logging.ConsoleFormatter;
import com.github.rob269.rsa.RSAServerKeys;
import com.github.rob269.rsa.WrongKeyException;

import java.io.File;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

//Server
public class Main {
    public static final DataBaseIO RSA_KEYS = new DataBaseIO(DataBaseIO.Tables.USER_RSA_KEYS);
    public static final DataBaseIO USERS = new DataBaseIO(DataBaseIO.Tables.USERS);
    public static final DataBaseIO MESSAGES = new DataBaseIO(DataBaseIO.Tables.USER_MESSAGES);
    public volatile static Set<String> usersOnline = new HashSet<>();
    public volatile static Set<String> needToCheckMessages = new HashSet<>();
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
        init();
        try (ServerSocket serverSocket = new ServerSocket(5099)) {
            LOGGER.info("The server is running");
            while (true) {
                Socket clientSocket = serverSocket.accept();
                new ConnectionMainThread(clientSocket).start();
            }
        } catch (IOException e) {
            LOGGER.warning("Server can't start\n" + ConsoleFormatter.formatStackTrace(e));
        }
    }

    private static void init() {
        RSAServerKeys.initKeys();
    }
}

class ConnectionMainThread extends Thread {
    public long handshakeTimer;
    Socket clientSocket;

    private static final Logger LOGGER = Logger.getLogger(ConnectionMainThread.class.getName());

    public ConnectionMainThread(Socket clientSocket) {
        this.clientSocket = clientSocket;
        handshakeTimer = System.currentTimeMillis();
    }

    @Override
    public void run() {
        LOGGER.info(Thread.currentThread().getName() + ": " + clientSocket.getInetAddress().toString() + " connected");
        try {
            clientSocket.setSoTimeout((int) TimeUnit.SECONDS.toMillis(10));
        } catch (SocketException e) {
            LOGGER.warning("Time out exception");
        }
        ClientIO clientIO = new ClientIO(clientSocket, handshakeTimer);
        try {
            clientIO.init();
        } catch (WrongKeyException e) {
            LOGGER.warning("Wrong Key\n" + ConsoleFormatter.formatStackTrace(e));
        }
        try {
            if (!clientIO.isClosed()) clientIO.close();
            LOGGER.info(clientSocket.getInetAddress() + " disconnected");
            clientSocket.close();
        } catch (IOException e) {
            LOGGER.warning("Exception at clientSocket.close()\n" + ConsoleFormatter.formatStackTrace(e));
        }
    }
}