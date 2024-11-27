package com.github.rob269;

import java.time.LocalDateTime;
import java.util.Calendar;

public class Message {
    private String recipient;
    private String sender;
    private String message;
    private String date;

    public Message(String recipient, String sender, String message, String date) {
        this.recipient = recipient;
        this.sender = sender;
        this.message = message;
        this.date = date;
    }

    public Message(String recipient, String sender, String message) {
        this.recipient = recipient;
        this.sender = sender;
        this.message = message;
    }

    public static void writeToDatabase(Message message) {
        Main.MESSAGES.write(new String[]{message.sender, message.recipient, message.message, "NOW()"});
    }

    @Override
    public String toString() {
        return "(" + date + ")" + sender + "-->" + recipient + ":" + message;
    }

    public String getRecipient() {
        return recipient;
    }

    public void setRecipient(String recipient) {
        this.recipient = recipient;
    }

    public String getSender() {
        return sender;
    }

    public void setSender(String sender) {
        this.sender = sender;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }
}
