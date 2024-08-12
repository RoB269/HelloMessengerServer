package com.github.rob269;

import com.github.rob269.io.ClientInterface;
import com.github.rob269.rsa.Key;
import com.github.rob269.rsa.RSAServerKeys;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.logging.Logger;

//Server
public class Main {

    private static final Logger LOGGER = Logger.getLogger(Thread.currentThread().getName() + ":" + Main.class.getName());
    public static void main(String[] args) {
        init();
        try (ServerSocket serverSocket = new ServerSocket(5099)){
            LOGGER.info("The server is running");
            while (true) {
                Socket clientSocket = serverSocket.accept();
                new ServerThread(clientSocket).start();
            }
        } catch (IOException e) {
            LOGGER.warning("Server can't start " + e);
        }
    }

    private static void init() {
        RSAServerKeys.initKeys();
    }
}

class ServerThread extends Thread {
    Socket clientSocket;

    private static final Logger LOGGER = Logger.getLogger(Thread.currentThread().getName() + ":" + ServerThread.class.getName());

    public ServerThread(Socket clientSocket) {
        this.clientSocket = clientSocket;
    }

    @Override
    public void run() {
        ClientInterface clientInterface = new ClientInterface(clientSocket);
        Key clientKey = clientInterface.init();
        if (clientKey != null) {

        }
        try {
            clientInterface.close();
            clientSocket.close();
        } catch (IOException e) {
            LOGGER.warning("Exception at clientSocket.close() " + e);
        }
    }
}