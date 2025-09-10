package com.github.rob269.helloMessengerServer;

import java.time.LocalDateTime;

public class Message {
    private long chatId;
    private long messageId;
    private String sender;
    private LocalDateTime date;
    private String message;

    public Message() {}

    public Message(long chatId, long messageId, String sender, LocalDateTime date, String message) {
        this.chatId = chatId;
        this.messageId = messageId;
        this.sender = sender;
        this.date = date;
        this.message = message;
    }

    @Override
    public String toString() {
        return "(" + date.toString() + ")" + sender + ": " + message;
    }

    public long getChatId() {
        return chatId;
    }

    public long getMessageId() {
        return messageId;
    }

    public String getSender() {
        return sender;
    }

    public String getMessage() {
        return message;
    }

    public LocalDateTime getDate() {
        return date;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof Message msg)) return false;
        return messageId == msg.messageId;
    }
}