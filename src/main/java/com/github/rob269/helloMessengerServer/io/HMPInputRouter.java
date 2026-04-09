package com.github.rob269.helloMessengerServer.io;

import com.github.rob269.helloMessengerServer.rsa.RSA;
import com.github.rob269.helloMessengerServer.rsa.RSAServerKeys;

import java.io.DataInputStream;
import java.io.IOException;
import java.util.*;
import java.util.logging.Logger;

public class HMPInputRouter extends Thread {
    private static final Logger LOGGER = Logger.getLogger(HMPInputRouter.class.getName());
    private final DataInputStream dis;
    private final HMPClientIO clientIO;
    public final Deque<byte[]> mainThreadInput = new ArrayDeque<>();
    public final Deque<byte[]> sideThreadInput = new ArrayDeque<>();

    public HMPInputRouter(DataInputStream dis, HMPClientIO clientIO) {
        this.dis = dis;
        this.clientIO = clientIO;
    }

    public void close() {
        interrupt();
    }

    @Override
    public void run() {
        try {
            byte[] input;
            while (!isInterrupted()) {
                int inputSize = (dis.readByte() & 0xff);
                input = new byte[inputSize];
                dis.readFully(input);

                if (input.length != 130 && (clientIO.isInitialized() || input[0] == 30)) {
                    mainThreadInput.add(new byte[]{0});
                    break;
                }

                if (input.length == 130) input = RSA.decodeByteArray(input, RSAServerKeys.getPrivateKey());
                int command = input[0];


                if (command >= 0) {
                    mainThreadInput.add(new byte[]{(byte) command});
                    readPackages(input, mainThreadInput);
                    synchronized (mainThreadInput) {
                        mainThreadInput.notify();
                    }
                }
                else {
                    sideThreadInput.add(new byte[]{(byte) command});
                    readPackages(input, sideThreadInput);
                    synchronized (sideThreadInput) {
                        sideThreadInput.notify();
                    }
                }
            }
        } catch (IOException _) {}
        clientIO.close();
        synchronized (mainThreadInput) {
            mainThreadInput.notify();
        }
        synchronized (sideThreadInput) {
            sideThreadInput.notify();
        }
        LOGGER.fine("Input router closed");
    }

    private void readPackages(byte[] input, Deque<byte[]> deque) throws IOException {
        if (input.length > 1) {
            for (int i = 0, packages = byteArrayToInt(input); i < packages; i++) {
                int byteLength = dis.readInt();
                byte[] bytes = new byte[byteLength];
                dis.readFully(bytes);
                if (clientIO.isInitialized()) bytes = RSA.decodeByteArray(bytes, RSAServerKeys.getPrivateKey());
                deque.add(bytes);
            }
        }
    }

    private static int byteArrayToInt(byte[] bytes) {
        int integer = 0;
        for (int i = 1; i < bytes.length; i++) {
            integer <<= 8;
            integer |=  bytes[i] & 0xff;
        }
        return integer;
    }
}