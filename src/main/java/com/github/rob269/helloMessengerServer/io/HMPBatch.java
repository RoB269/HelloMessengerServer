package com.github.rob269.helloMessengerServer.io;

import java.io.IOException;
import java.math.BigInteger;
import java.util.logging.Logger;

public class HMPBatch implements Batch {
    private static final Logger LOGGER = Logger.getLogger(HMPBatch.class.getName());
    private final byte command;
    private final HMPClientIO clientIO;
    private final int maxSize;
    private int size = 0;
    private final byte[][] batch;
    private String[] logs = null;

    public HMPBatch(byte command, HMPClientIO clientIO, int size, boolean log) {
        this.command = command;
        this.clientIO = clientIO;
        this.maxSize = size;
        batch = new byte[size][];
        if (log) logs = new String[size];
    }

    public void write(long message) throws IOException {
        if (!clientIO.isClosed()) {
            if (logs != null) logs[size] = "Write long:\n" + message;
            write(longToByteArray(message));
        }
    }

    private static byte[] longToByteArray(long integer) {
        byte[] bytes = new byte[8];
        int i;
        for (i = 7; i >= 0 && integer != 0; i--) {
            bytes[i] = (byte) integer;
            integer >>>= 8;
        }
        byte[] toReturn = new byte[7-i];
        System.arraycopy(bytes, i+1, toReturn, 0, toReturn.length);
        return toReturn;
    }

    public void write(String message) throws IOException {
        if (!clientIO.isClosed()){
            if (logs != null) logs[size] = "Write string:\n" + message;
            write(message.getBytes());
        }
    }

    public void write(BigInteger message) throws IOException {
        if (!clientIO.isClosed()) {
            if (logs != null) logs[size] = "Write bigint:\n" + message;
            write(message.toByteArray());
        }
    }

    public void write(byte[] message) throws IOException {
        batch[size] = message;
        size++;
        if (size == maxSize) {
            synchronized (clientIO) {
                clientIO.writeCommand(command, size);
                if (logs != null) {
                    for (int i = 0; i < batch.length; i++) {
                        clientIO.write(batch[i], false);
                        LOGGER.finer(logs[i]);
                    }
                }
                else {
                    for (byte[] HMPPackage : batch)
                        clientIO.write(HMPPackage, false);
                }
            }
        }
    }
}
