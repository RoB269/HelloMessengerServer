package com.github.rob269.helloMessengerServer;

import com.github.rob269.helloMessengerServer.io.Batch;
import com.github.rob269.helloMessengerServer.io.ClientIO;
import com.github.rob269.helloMessengerServer.io.DatabaseInterface;
import com.github.rob269.helloMessengerServer.logging.LogFormatter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.logging.Logger;

public class Endpoints {
    private static final Logger LOGGER = Logger.getLogger(Endpoints.class.getName());
    private final ClientIO clientIO;
    private long userId;

    private static final int MAX_MESSAGE_COUNT_IN_REQUEST = 100;

    public Endpoints(ClientIO clientIO) {
        this.clientIO = clientIO;
    }


    public void handleRequest(byte request) throws IOException, SQLException {
        switch (request) {
            case 90 -> clientIO.writeCommand(90);//Ping
            case 0, 99 -> clientIO.close();//Disconnect
            case 21 -> {//Login
                String username = clientIO.readString();
                String password = clientIO.readString(false);
                if (username.contains("\\")) {clientIO.writeCommand(65); return;}
                String hash = sha256(password+username);
                synchronized (Endpoints.class) {
                    List<String[]> dbLine = DatabaseInterface.sqlRead(0, 2, username);
                    if (!dbLine.isEmpty()) {
                        if (!dbLine.getFirst()[0].equals(hash)) {
                            clientIO.writeCommand(61);
                            clientIO.close();
                            return;
                        }
                        userId = Long.parseLong(dbLine.getFirst()[1]);
                    } else {
                        DatabaseInterface.sqlWrite(1, username, hash);
                        dbLine = DatabaseInterface.sqlRead(2, 1, username);
                        userId = Long.parseLong(dbLine.getFirst()[0]);
                    }
                }
                clientIO.setUsername(username);
                Thread.currentThread().setName("MainConnectionThread{" + username + "}");
                SideConnectionThread thread = new SideConnectionThread(clientIO);
                thread.setName("SideConnectionThread{" + username + "}");
                thread.start();
                Main.onlineUsersThreads.put(username, thread);
                DatabaseInterface.sqlWrite(16, username);
                clientIO.writeCommand(52);
                LOGGER.info("The user logged in under the username %s".formatted(username));
            }
            case 80 -> {//Get chats
                if (clientIO.getUsername() == null) {clientIO.writeCommand(61); return;}
                //Get private chats
                List<String[]> privateChats = DatabaseInterface.sqlRead(3, 7, userId, userId, userId);
                //Get public chats
                List<String[]> publicChats = DatabaseInterface.sqlRead(4, 7, userId);
                StringBuilder privateChatsBuilder = new StringBuilder();
                for (String[] chat : privateChats) {
                    chat[2] = chat[2].substring(0, 1);
                    if (chat[3] == null) {
                        chat[3] = "";
                        chat[4] = "";
                        chat[5] = "";
                        chat[6] = "";
                    }
                    else {
                        if (chat[4].equals(String.valueOf(userId)))
                            chat[4] = clientIO.getUsername();
                        else
                            chat[4] = chat[1];
                        chat[6] = chat[6].replaceAll("\\\\", "\\\\&");
                    }
                    for (int i = 0; i < 7; i++)
                        privateChatsBuilder.append(chat[i]).append("\\\\");
                }
                privateChatsBuilder.append(";");
                StringBuilder publicChatsBuilder = new StringBuilder();
                for (String[] chat : publicChats) {
                    chat[2] = chat[2].substring(0, 1);
                    if (chat[3] == null) {
                        chat[3] = "";
                        chat[4] = "";
                        chat[5] = "";
                        chat[6] = "";
                    }
                    else chat[6] = chat[6].replaceAll("\\\\", "\\\\&");
                    for (int i = 0; i < 7; i++) {
                        publicChatsBuilder.append(chat[i]).append("\\\\");
                    }
                }
                publicChatsBuilder.append(";");
                Batch batch = clientIO.writeBatch(53, 2, false);
                batch.write(privateChatsBuilder.toString());
                batch.write(publicChatsBuilder.toString());
            }
            case 81 -> {//Create new private chat
                String recipient = clientIO.readString(false);
                if (clientIO.getUsername() == null) {clientIO.writeCommand(61); return;}
                if (recipient.isEmpty()) {clientIO.writeCommand(63); return;}
                if (recipient.contains("\\")) {clientIO.writeCommand(65); return;}
                if (recipient.equals(clientIO.getUsername())) {clientIO.writeCommand(60); return;}
                List<String[]> dbLine = DatabaseInterface.sqlRead(2, 1, recipient);
                if (dbLine.isEmpty()) {clientIO.writeCommand(64); return;}
                long dbRecipientId = Long.parseLong(dbLine.getFirst()[0]);
                long chatId;
                LocalDateTime localDateTime;
                synchronized (Endpoints.class) {
                    List<String[]> contact = DatabaseInterface.sqlRead(5, 1, dbRecipientId, userId, userId, dbRecipientId);
                    if (!contact.isEmpty()) {
                        clientIO.writeCommand(62);
                        return;
                    }
                    chatId = DatabaseInterface.createChat();
                    DatabaseInterface.sqlWrite(6, userId, chatId, "ok");
                    DatabaseInterface.sqlWrite(7, dbRecipientId, chatId);
                    DatabaseInterface.sqlWrite(8, userId, dbRecipientId, chatId);
                    localDateTime = roundDateTime(LocalDateTime.now());
                    DatabaseInterface.sqlWrite(11, (long) 0, chatId, (long) -1, "", localDateTime.toString());
                }
                Batch batch = clientIO.writeBatch(54, 1, false);
                batch.write(chatId + "\\\\" + localDateTime.format(Main.dateTimeFormatter) + "\\\\;");
                if (Main.onlineUsersThreads.containsKey(recipient)) {
                    Main.onlineUsersThreads.get(recipient).newChat(new Chat(chatId, clientIO.getUsername(),
                            new Message(chatId, 0, "null", localDateTime, ""), true));
                }
            }
            case 82 -> {//Create new public chat
                String name = clientIO.readString(false);
                if (clientIO.getUsername() == null) {clientIO.writeCommand(61); return;}
                if (name.isEmpty()) {clientIO.writeCommand(63); return;}
                if (name.contains("\\")) {clientIO.writeCommand(65); return;}
                long chatId = DatabaseInterface.createChat(name, userId);
                DatabaseInterface.sqlWrite(6, userId, chatId, "ok");
                LocalDateTime localDateTime = roundDateTime(LocalDateTime.now());
                DatabaseInterface.sqlWrite(11, (long) 0, chatId, (long) -1, "", localDateTime.toString());
                Batch batch = clientIO.writeBatch(54, 1, false);
                batch.write(chatId + "\\\\" + localDateTime.format(Main.dateTimeFormatter) + "\\\\;");
            }
            case 83 -> {//Send message
                long chatId = clientIO.readLong(false);
                String message = clientIO.readString(false);
                if (clientIO.getUsername() == null) {clientIO.writeCommand(61); return;}
                List<String[]> dbLine = DatabaseInterface.sqlRead(9, 1, userId, chatId);
                if (dbLine.isEmpty()) {
                    dbLine = DatabaseInterface.sqlRead(10,1,chatId);
                    if (dbLine.isEmpty()) {
                        clientIO.writeCommand(67);
                        return;
                    }
                    clientIO.writeCommand(68);
                    return;
                }
                if (dbLine.getFirst()[0].equals("block")) {clientIO.writeCommand(69); return;}
                LocalDateTime localDateTime;
                long messageId;
                synchronized (Endpoints.class) {
                    dbLine = DatabaseInterface.sqlRead(10, 1,chatId);
                    messageId = Long.parseLong(dbLine.getFirst()[0]) + 1;
                    localDateTime = roundDateTime(LocalDateTime.now());
                    DatabaseInterface.sqlWrite(11, messageId, chatId, userId, message, localDateTime.toString());
                    DatabaseInterface.sqlWrite(12, messageId, chatId);
                }
                String messageMeta = messageId + "\\\\" + localDateTime.format(Main.dateTimeFormatter) + "\\\\;";
                Batch batch = clientIO.writeBatch(55, 1, false);
                batch.write(messageMeta);
                dbLine = DatabaseInterface.sqlRead(17, 1, chatId, userId);
                for (String[] user : dbLine) {
                    if (Main.onlineUsersThreads.containsKey(user[0])) {
                        SideConnectionThread thread = Main.onlineUsersThreads.get(user[0]);
                        thread.newMessage(new Message(chatId, messageId, clientIO.getUsername(), localDateTime, message));
                    }
                }
            }
            case 84 -> {//Get messages
                long chatId = clientIO.readLong(false);
                String[] params = clientIO.readString().split("\\\\\\\\");
                if (clientIO.getUsername() == null) {clientIO.writeCommand(61); return;}
                List<String[]> chatConnection = DatabaseInterface.sqlRead(9, 1, userId, chatId);
                if (chatConnection.isEmpty()) {clientIO.writeCommand(68); return;}
                List<String[]> entries;
                try {
                    int messageCount = Integer.parseInt(params[1]);
                    if (messageCount > MAX_MESSAGE_COUNT_IN_REQUEST) messageCount = MAX_MESSAGE_COUNT_IN_REQUEST;
                    entries = DatabaseInterface.sqlRead(15, 4, chatId, Long.parseLong(params[0]), chatId, userId, messageCount);
                } catch (RuntimeException e) {
                    clientIO.writeCommand(71);
                    return;
                }
                StringBuilder builder = new StringBuilder();
                for (String[] entry : entries) {
                    entry[3] = entry[3].replaceAll("\\\\", "\\\\&");
                    for (int i = 0; i < 4; i++)
                        builder.append(entry[i]).append("\\\\");
                }
                builder.append(";");
                Batch batch = clientIO.writeBatch(56, 1, false);
                batch.write(builder.toString());
            }
            case 85 -> {//Add user to the chat
                long chatId = clientIO.readLong(false);
                String username = clientIO.readString();
                //todo
            }
            case 86 -> {//Join the chat
                long chatId = clientIO.readLong(false);
                List<String[]> dbLines = DatabaseInterface.sqlRead(9, 1, userId, chatId);
                if (!dbLines.isEmpty()) {clientIO.writeCommand(72); return;}
                dbLines = DatabaseInterface.sqlRead(18, 5, chatId);
                if (dbLines.isEmpty()) {clientIO.writeCommand(67); return;}
                DatabaseInterface.sqlWrite(6, userId, chatId, "ok");
                StringBuilder builder = new StringBuilder();
                String[] chat = dbLines.getFirst();
                chat[4] = chat[4].replaceAll("\\\\", "\\\\&");
                for (String string : chat) {
                    builder.append(string).append("\\\\");
                }
                builder.append(";");
                Batch batch = clientIO.writeBatch(57, 1, false);
                batch.write(builder.toString());
            }
        }
    }

    private LocalDateTime roundDateTime(LocalDateTime localDateTime) {
        return localDateTime.getNano()<500000000L ? localDateTime.withNano(0) : localDateTime.withNano(0).plusSeconds(1);
    }

    private static String sha256(String string) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(string.getBytes(StandardCharsets.UTF_8));
            StringBuilder builder = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) builder.append("0");
                builder.append(hex);
            }
            return builder.toString();
        } catch (NoSuchAlgorithmException e) {
            LOGGER.warning("SHA-256 algorithm exception:\n" + LogFormatter.formatStackTrace(e));
        }
        return null;
    }
}
