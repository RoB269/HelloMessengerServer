package com.github.rob269.helloMessengerServer;

import com.github.rob269.helloMessengerServer.io.ClientIO;
import com.github.rob269.helloMessengerServer.io.HMPBatch;

import java.io.IOException;
import java.util.logging.Logger;

public class SideConnectionThread extends Thread {
    private static final Logger LOGGER = Logger.getLogger(SideConnectionThread.class.getName());
    private final ClientIO clientIO;
    private volatile Message newMessage = new Message();
    private volatile boolean hasNewMessage = false;

    public SideConnectionThread(ClientIO clientIO) {
        this.clientIO = clientIO;
    }

    public void newMessage(Message message) {
        synchronized (newMessage) {
            newMessage.update(message);
            hasNewMessage = true;
            newMessage.notify();
        }
    }

    private String formatMessage(Message message) {
        return message.getChatId() + "\\\\" + message.getMessageId() + "\\\\" + message.getSender() + "\\\\" +
                message.getDate() + "\\\\" + message.getMessage().replaceAll("\\\\", "\\\\&") + "\\\\" +
                ";";
    }

    @Override
    public void run() {
        try {
            while (!clientIO.isClosed()) {
                synchronized (newMessage) {
                    while (!clientIO.isClosed() && !hasNewMessage) {
                        newMessage.wait();
                    }
                    hasNewMessage = false;
                    HMPBatch batch = clientIO.writeBatch(-10, 1, false);
                    batch.write(formatMessage(newMessage));
                }
            }
        } catch (IOException | InterruptedException _) {}
        LOGGER.fine("Side thread closed");
    }
}