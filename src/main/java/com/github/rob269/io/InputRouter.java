package com.github.rob269.io;

import com.github.rob269.rsa.RSA;
import com.github.rob269.rsa.RSAServerKeys;

import java.io.DataInputStream;
import java.io.IOException;
import java.util.*;
import java.util.logging.Logger;

public class InputRouter extends Thread {
    private static final Logger LOGGER = Logger.getLogger(InputRouter.class.getName());
    private DataInputStream dis;
    private final ClientIO clientIO;
    public final Deque<byte[]> mainThreadInput = new ArrayDeque<>();
    public final Deque<byte[]> sideThreadInput = new ArrayDeque<>();

    public InputRouter(DataInputStream dis, ClientIO clientIO) {
        this.dis = dis;
        this.clientIO = clientIO;
    }

    public void close() {
        interrupt();
    }

    private static final List<Integer> commandsWithContinue = new ArrayList<>(List.of(new Integer[]{20, 21, 22, 23}));
    @Override
    public void run() {
        try {
            while (!isInterrupted()) {
                byte[] input = new byte[130];
                int inputSize = dis.read(input);
                int command;
                if (inputSize == -1) {
                    mainThreadInput.add(new byte[]{0});
                    break;
                } else if (inputSize == 130) {
                    command = RSA.decodeByteArray(input, RSAServerKeys.getPrivateKey())[0];
                } else {
                    command = input[0];
                }
                if (command >= 0) {
                    mainThreadInput.add(new byte[]{(byte) command});
                    if (commandsWithContinue.contains(command)) {
                        int packages = dis.readInt();
                        for (int i = 0; i < packages; i++) {
                            int byteLength = dis.readInt();
                            byte[] bytes = new byte[byteLength];
                            dis.read(bytes);
                            if (clientIO.isInitialized()) bytes = RSA.decodeByteArray(bytes, RSAServerKeys.getPrivateKey());
                            mainThreadInput.add(bytes);
                        }
                    }
                    synchronized (mainThreadInput) {
                        mainThreadInput.notify();
                    }
                }
            }
        } catch (IOException ignored) {}
        synchronized (mainThreadInput) {
            clientIO.close();
            mainThreadInput.notify();
        }
        LOGGER.fine("Input router closed");
    }
}