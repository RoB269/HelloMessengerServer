package com.github.rob269.helloMessengerServer;

import com.github.rob269.helloMessengerServer.io.Batch;
import com.github.rob269.helloMessengerServer.io.ClientIO;
import com.github.rob269.helloMessengerServer.io.HMPBatch;

import java.io.IOException;
import java.util.logging.Logger;

public class SideConnectionThread extends Thread {
    private static final Logger LOGGER = Logger.getLogger(SideConnectionThread.class.getName());
    private final ClientIO clientIO;
    private volatile int command = 0;
    private final Object monitor = new Object();

    private Message newMessage;
    private Chat chat;

    public SideConnectionThread(ClientIO clientIO) {
        this.clientIO = clientIO;
    }

    public void newMessage(Message message) {
        synchronized (monitor) {
            while (command != 0) {
                try {
                    monitor.wait();
                } catch (InterruptedException _) {
                }
            }
            newMessage = message;
            command = -10;
            monitor.notify();
        }
    }

    public void newChat(Chat chat) {
        synchronized (monitor) {
            while (command != 0) {
                try {
                    monitor.wait();
                } catch (InterruptedException _) {
                }
            }
            this.chat = chat;
            command = -11;
            monitor.notify();
        }
    }

    private String formatMessage(Message message) {
        return message.getChatId() + "\\\\" + message.getMessageId() + "\\\\" + message.getSender() + "\\\\" +
                message.getDate().format(Main.dateTimeFormatter) + "\\\\" + message.getMessage().replaceAll("\\\\", "\\\\&") + "\\\\;";
    }

    private String formatChat(Chat chat) {
        Message message = chat.lastMessage();
        return chat.chatId() + "\\\\" + chat.name() + "\\\\" + message.getMessageId() + "\\\\" + message.getSender() +
                "\\\\" + message.getDate().format(Main.dateTimeFormatter) + "\\\\" + message.getMessage().replaceAll("\\\\", "\\\\&") + "\\\\" + (chat.isPrivate() ? "1" : "0") + "\\\\;";
    }

    @Override
    public void run() {
        try {
            while (!clientIO.isClosed()) {
                synchronized (monitor) {
                    while (!clientIO.isClosed() && command == 0) {
                        monitor.wait();
                    }
                    switch (command) {
                        case -10 -> {
                            Batch batch = clientIO.writeBatch(-10, 1, false);
                            batch.write(formatMessage(newMessage));
                        }
                        case -11 -> {
                            Batch batch = clientIO.writeBatch(-11, 1, false);
                            batch.write(formatChat(chat));
                        }
                    }
                    command = 0;
                    monitor.notify();
                }
            }
        } catch (IOException | InterruptedException _) {}
        LOGGER.fine("Side thread closed");
    }
}