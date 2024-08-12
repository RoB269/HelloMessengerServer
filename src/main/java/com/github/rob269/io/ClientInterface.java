package com.github.rob269.io;

import com.github.rob269.rsa.Key;
import com.github.rob269.rsa.RSAServerKeys;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.logging.Logger;

public class ClientInterface {
    private OutputStreamWriter osw;
    private Scanner scanner;
    private Socket clientSocket;
    private Key clientKey = new Key();

    private static final Logger LOGGER = Logger.getLogger(Thread.currentThread().getName() + ":" + ClientInterface.class.getName());

    public ClientInterface(Socket clientSocket) {
        this.clientSocket = clientSocket;
        try {
            osw = new OutputStreamWriter(clientSocket.getOutputStream());
            scanner = new Scanner(clientSocket.getInputStream());
            LOGGER.fine("Output and Input streams is open");
        } catch (IOException e) {
            LOGGER.warning("Can't open streams " + e);
        }
    }

    public void sendResponse(String string) {
        char[] chars = new char[string.length()];
        string.getChars(0, string.length(), chars, 1);
        int[] intString = new int[string.length()];
        for (int i = 0; i < intString.length; i++) {
            intString[i] = (int) chars[i];
        }
    }

    public Key init() {
        Key clientKey = null;
        List<String> request = readFromClient();
        if (!request.isEmpty()) {
            if (request.getFirst().equals("GET SERVER KEY")) {
                List<String> lines = new ArrayList<>();
                lines.add(String.valueOf(RSAServerKeys.getPublicKey().getKey()[0]));
                lines.add(String.valueOf(RSAServerKeys.getPublicKey().getKey()[1]));
                lines.add(String.valueOf(RSAServerKeys.getPublicKey().getMeta()[0]));
                lines.add(String.valueOf(RSAServerKeys.getPublicKey().getMeta()[1]));
                writeToClient(lines);
                 readFromClient();
            }
        }
        else {
            LOGGER.warning("Request is null");
        }
        return clientKey;
    }

    public List<String> readFromClient() {
        List<String> lines = new ArrayList<>();
        try {
            Scanner scanner = new Scanner(clientSocket.getInputStream());
            while (scanner.hasNext()) {
                lines.add(scanner.nextLine());
            }
            scanner.close();
        } catch (IOException e) {
            LOGGER.warning("Can't open request InputStream " + e);
        }
        return lines;
    }

    public void writeToClient(String message) {
        if (message == null) {
            LOGGER.warning("Null message");
            return;
        }
        try {
            osw.write(message);
            osw.flush();
            LOGGER.fine("Message sent");
        } catch (IOException e) {
            LOGGER.warning("Can't write the message by outputStreamWriter " + e);
        }
    }

    public void writeToClient(List<String> lines) {
        if (lines == null || lines.isEmpty()) {
            LOGGER.warning("Null message");
            return;
        }
        for (int i = 0; i < lines.size(); i++) {
            try {
                osw.write(lines.get(i) + "\n");
            } catch (IOException e) {
                LOGGER.warning("Can't send message " + e);
            }
        }
        try {
            osw.flush();
        } catch (IOException e) {
            LOGGER.warning("Can't flush message " + e);
        }
    }

    public void close() {
        try {
            osw.close();
            scanner.close();
        } catch (IOException e) {
            LOGGER.warning("Can't close streams " + e);
        }
    }
}
