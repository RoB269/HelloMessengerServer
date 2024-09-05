package com.github.rob269;

import com.github.rob269.io.ClientIO;
import com.github.rob269.io.DataBaseIO;
import com.github.rob269.rsa.RSAServerKeys;
import com.github.rob269.rsa.WrongKeyException;

import java.io.File;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

//Server
public class Main {
    public static final DataBaseIO RSA_KEYS = new DataBaseIO(DataBaseIO.Tables.USER_RSA_KEYS);
    static {
        File logsDir = new File("log/");
        if (!logsDir.exists()) {
            logsDir.mkdir();
        }
    }
    private static final Logger LOGGER = Logger.getLogger(Main.class.getName());

    public static void main(String[] args) {
        init();
        try (ServerSocket serverSocket = new ServerSocket(5099)){
            LOGGER.info("The server is running");
            while (true) {
                Socket clientSocket = serverSocket.accept();
                new ServerThread(clientSocket).start();
            }
        } catch (IOException e) {
            LOGGER.warning("Server can't start");
            e.printStackTrace();
        }
    }

    private static void init() {
        RSAServerKeys.initKeys();
    }
}

class ServerThread extends Thread {
    Socket clientSocket;

    private static final Logger LOGGER = Logger.getLogger(ServerThread.class.getName());

    public ServerThread(Socket clientSocket) {
        this.clientSocket = clientSocket;
    }

    @Override
    public void run() {
        LOGGER.info(Thread.currentThread().getName() + ": " + clientSocket.getInetAddress().toString() + " connected");
        try {
            clientSocket.setSoTimeout((int) TimeUnit.SECONDS.toMillis(10));
        } catch (SocketException e) {
            LOGGER.warning("Time out exception");
        }
        ClientIO clientIO = new ClientIO(clientSocket);
        try {
            clientIO.init();
        } catch (WrongKeyException e) {
            LOGGER.warning("Wrong Key");
            e.printStackTrace();
        }
        try {
            if (!clientIO.isClosed()) clientIO.close();
            LOGGER.info(clientSocket.getInetAddress() + " disconnected");
            clientSocket.close();
        } catch (IOException e) {
            LOGGER.warning("Exception at clientSocket.close()");
            e.printStackTrace();
        }
    }
}