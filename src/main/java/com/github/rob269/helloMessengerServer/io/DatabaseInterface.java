package com.github.rob269.helloMessengerServer.io;

import com.github.rob269.helloMessengerServer.logging.ConsoleFormatter;

import java.sql.*;
import java.util.*;
import java.util.logging.Logger;

public class DatabaseInterface {
    public static final Logger LOGGER = Logger.getLogger(DatabaseInterface.class.getName());
    private static final String userName = "root";
    private static String password = null;
    private static final String url = "jdbc:mysql://127.0.0.1:3306/hello_messenger_db?allowMultiQueries=true";
    private static Connection conn;
    private static final List<PreparedStatement> statements = new ArrayList<>();
    private static void createStatements() throws SQLException {
        statements.add(conn.prepareStatement("SELECT password, user_id FROM users WHERE username=?"));//                                            0
        statements.add(conn.prepareStatement("INSERT INTO users (username, password) VALUES (?, ?)"));//                                            1
        statements.add(conn.prepareStatement("SELECT user_id FROM users WHERE username = ?"));//                                                    2
        statements.add(conn.prepareStatement(//                                                                                                         3
                            """
SELECT e.chat_id, e.username, e.status, um.message_id, um.sender, um.date, um.message FROM
(SELECT d.chat_id, d.username, d.status, c.last_message_id FROM
(SELECT b.chat_id, u.username, b.status FROM
(SELECT a.chat_id, a.user_id, cc.status FROM
(SELECT chat_id, user_2 AS user_id FROM contacts WHERE user_1 = ?
UNION SELECT chat_id, user_1 FROM contacts WHERE user_2 = ?) a
JOIN chat_connections cc ON cc.chat_id = a.chat_id WHERE cc.user_id = ?) b
JOIN users u ON u.user_id = b.user_id) d
JOIN chats c ON c.chat_id = d.chat_id) e
LEFT JOIN user_messages um ON um.message_id = e.last_message_id AND um.chat_id = e.chat_id"""));
        statements.add(conn.prepareStatement(//                                                                                                         4
                            """
SELECT b.chat_id, b.name, b.status, b.message_id, u.username AS sender, b.date, b.message FROM
(SELECT a.chat_id, a.name, a.status, um.message_id, um.message, um.date, um.sender FROM
(SELECT cc.chat_id, c.name, cc.status, c.last_message_id FROM
(SELECT chat_id, status FROM chat_connections WHERE user_id = ?) cc
JOIN chats c ON cc.chat_id = c.chat_id WHERE NOT c.name IS NULL) a
LEFT JOIN user_messages um ON um.chat_id = a.chat_id AND a.last_message_id = um.message_id) b
LEFT JOIN users u ON b.sender = u.user_id
                            """));
        statements.add(conn.prepareStatement("SELECT chat_id FROM contacts WHERE (user_1 = ? AND user_2 = ?) OR (user_1 = ? AND user_2 = ?)"));//   5
        statements.add(conn.prepareStatement("INSERT INTO chat_connections (user_id, chat_id, status) VALUES (?, ?, 'ok')"));//                     6
        statements.add(conn.prepareStatement("INSERT INTO chat_connections (user_id, chat_id) VALUES (?, ?)"));//                                   7
        statements.add(conn.prepareStatement("INSERT INTO contacts (user_1, user_2, chat_id) VALUES (?, ?, ?)"));//                                 8
        statements.add(conn.prepareStatement("SELECT status FROM chat_connections WHERE user_id = ? AND chat_id = ?"));//                           9
        statements.add(conn.prepareStatement("SELECT last_message_id FROM chats WHERE chat_id = ?"));//                                             10
        statements.add(conn.prepareStatement("INSERT INTO user_messages (message_id, chat_id, sender, message, date) VALUES (?, ?, ?, ?, ?)"));//   11
        statements.add(conn.prepareStatement("UPDATE chats SET last_message_id = ? WHERE chat_id = ?"));//                                          12
        statements.add(conn.prepareStatement("INSERT INTO chats () VALUES ();\nSELECT MAX(chat_id) FROM chats;"));//                                13
        statements.add(conn.prepareStatement("INSERT INTO chats (name, owner_id) VALUES (?, ?);\nSELECT MAX(chat_id) FROM chats;"));//              14
        statements.add(conn.prepareStatement(//                                                                                                         15
                """
SELECT msg.message_id, u.username, msg.date, msg.message FROM
(SELECT message_id, sender, date, message FROM user_messages WHERE chat_id = ? AND message_id < ?
AND NOT message_id IN (SELECT message_id FROM deleted_user_messages WHERE chat_id = ? AND message_id = user_messages.message_id
AND (user_id IS NULL OR user_id = ?))) msg
LEFT JOIN users u ON msg.sender = u.user_id
ORDER BY message_id DESC LIMIT ?
                """));
        statements.add(conn.prepareStatement("UPDATE users SET last_online = NOW() WHERE username = ?"));//                                         16
        statements.add(conn.prepareStatement("SELECT username FROM (SELECT user_id FROM chat_connections WHERE chat_id = ? AND user_id != ? " + //  17
                "AND status != 'block') cc JOIN users u ON cc.user_id = u.user_id"));
    }

    public static void init(String password) throws SQLException{
        if (DatabaseInterface.password == null) {
            DatabaseInterface.password = password;
            conn = DriverManager.getConnection(url, userName, password);
            createStatements();
            LOGGER.warning("Database connected");
        }
    }

    public static List<String[]> sqlRead(int statementInd, int responseSize, Object... values) throws SQLException {
        PreparedStatement statement = statements.get(statementInd);
        try {
            for (int i = 0; i < values.length; i++) {
                if (values[i] instanceof String string) {
                    statement.setString(i + 1, string);
                }
                else if (values[i] instanceof Long longInteger) {
                    statement.setLong(i+1, longInteger);
                }
                else if (values[i] instanceof Integer integer) {
                    statement.setInt(i+1, integer);
                }
            }
            return sqlRead(statement, responseSize);
        } catch (SQLException e) {
            LOGGER.warning("SQL exception\n" + ConsoleFormatter.formatStackTrace(e));
            throw e;
        }
    }

    public static void sqlWrite(int statementInd, Object... values) throws SQLException {
        PreparedStatement statement = statements.get(statementInd);
        try {
            for (int i = 0; i < values.length; i++) {
                if (values[i] instanceof String string) {
                    statement.setString(i + 1, string);
                }
                else if (values[i] instanceof Long integer) {
                    statement.setLong(i+1, integer);
                }
            }
            sqlWrite(statement);
        } catch (SQLException e) {
            LOGGER.warning("SQL exception\n" + ConsoleFormatter.formatStackTrace(e));
            throw e;
        }
    }

    private synchronized static void sqlWrite(PreparedStatement statement) throws SQLException {
        LOGGER.finest("SQL write request");
        try {
            statement.executeUpdate();
        } catch (SQLException e) {
            LOGGER.warning("SQL exception\n" + ConsoleFormatter.formatStackTrace(e));
            throw e;
        }
    }

    private static synchronized List<String[]> sqlRead(PreparedStatement statement, int responseSize) throws SQLException {
        LOGGER.finest("SQL read request");
        List<String[]> entries = new ArrayList<>();
        try {
            ResultSet rs = statement.executeQuery();
            String[] line;
            while (rs.next()) {
                line = new String[responseSize];
                for (int i = 0; i < responseSize; i++) {
                    line[i] = rs.getString(i+1);
                }
                entries.add(line);
            }
        } catch (SQLException e) {
            LOGGER.warning("SQL exception\n" + ConsoleFormatter.formatStackTrace(e));
            throw e;
        }
        return entries;
    }

    public static synchronized List<String[]> sqlRead(String sql, int responseSize) {
        LOGGER.finest("SQL read request");
        List<String[]> strings = new ArrayList<>();
        try (Statement statement = conn.createStatement()){
            ResultSet set = statement.executeQuery(sql);
            while (set.next()) {
                String[] line = new String[responseSize];
                for (int i = 0; i < responseSize; i++) {
                    line[i] = set.getString(i+1);
                }
                strings.add(line);
            }
        } catch (SQLException e) {
            LOGGER.warning("SQL exception\n" + ConsoleFormatter.formatStackTrace(e));
        }
        return strings;
    }

    public static synchronized void sqlWrite(String sql) {
        LOGGER.finest("SQL write request");
        try (Connection conn = DriverManager.getConnection(url, userName, password);
             Statement statement = conn.createStatement()){
            statement.executeUpdate(sql);
        } catch (SQLException e) {
            LOGGER.warning("SQL exception\n" + ConsoleFormatter.formatStackTrace(e));
        }
    }

    public static long createChat() throws SQLException{
        return createChat("", -1);
    }

    public static synchronized long createChat(String name, long ownerId) throws SQLException{
        LOGGER.finest("SQL create chat request");
        long chat_id;
        try {
            PreparedStatement statement;
            if (name.isEmpty()) statement = statements.get(13);
            else {
                statement = statements.get(14);
                statement.setString(1, name);
                statement.setLong(2, ownerId);
            }
            statement.execute();
            statement.getMoreResults();
            ResultSet set = statement.getResultSet();
            set.next();
            chat_id = set.getLong(1);
            return chat_id;
        } catch (SQLException e) {
            LOGGER.warning("SQL exception\n" + ConsoleFormatter.formatStackTrace(e));
            throw e;
        }
    }
}
