package com.github.rob269.io;

import com.github.rob269.logging.ConsoleFormatter;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

public class DataBaseIO {
    public static final Logger LOGGER = Logger.getLogger(DataBaseIO.class.getName());
    private static final String userName = "root";
    private static final String password = "root";
    private static final String url = "jdbc:mysql://127.0.0.1:3306/hello_messenger_db";
    private final List<String> columns;
    private final List<String> dataTypes;
    private final List<String> isAutoincrement;
    private final Tables table;

    public enum Tables {
        USER_RSA_KEYS("user_rsa_keys"),
        /*
        id
        user_key_0
        user_key_1
        user_key_meta_0
        user_key_meta_1
        user_id
         */
        USERS("users"),
        /*
        id
        user_id
        password
        last_online
         */
        USER_MESSAGES("user_messages");
        /*
        id
        sender
        recipient
        message
        date
         */
        private final String str;

        Tables(String str) {
            this.str = str;
        }

        @Override
        public String toString() {
            return str;
        }
    }

    public DataBaseIO(Tables table) {
        this.table = table;
        List<List<String>> col = getMetaData("COLUMN_NAME", "DATA_TYPE", "IS_AUTOINCREMENT");
        columns = col.get(0);
        dataTypes = col.get(1);
        isAutoincrement = col.get(2);
    }

    private synchronized List<List<String>> getMetaData(String... get) {
        List<List<String>> columns = new ArrayList<>();
        for (int i = 0; i < get.length; i++) {
            columns.add(new ArrayList<>());
        }
        try (Connection connection = DriverManager.getConnection(url, userName, password)){
            DatabaseMetaData md = connection.getMetaData();
            ResultSet set = md.getColumns("hello_messenger_db", null, table.toString(), null);
            while (set.next()) {
                for (int i = 0; i < get.length; i++) {
                    List<String> buff = columns.get(i);
                    buff.add(set.getString(get[i]));
                    columns.set(i, buff);
                }
            }
        } catch (SQLException e) {
            LOGGER.warning("SQL Exception\n" + ConsoleFormatter.formatStackTrace(e));
        }
        return columns;
    }

    public synchronized boolean isExist(int columnIndex, String i) {
        try (Connection conn = DriverManager.getConnection(url, userName, password);
        Statement statement = conn.createStatement()){
            String column = columns.get(columnIndex);
            ResultSet set = statement.executeQuery("SELECT %s FROM %s WHERE %s='%s'"
                    .formatted(column, table, column, i));
            return set.next();
        } catch (SQLException e) {
            LOGGER.warning("SQL Exception\n" + ConsoleFormatter.formatStackTrace(e));
        }
        return false;
    }

    private String formatValues(String[] values) {
        for (int i = 0; i < this.columns.size(); i++) {
            if (isAutoincrement.get(i).equals("YES")) {
                dataTypes.set(i, "N");
            }
        }
        int j = 0;
        StringBuilder stringBuilder = new StringBuilder();
        for (int i = 0; i < isAutoincrement.size(); i++) {
            if (isAutoincrement.get(i).equals("YES")) continue;
            if (dataTypes.get(i).equals("12")) stringBuilder.append("'").append(values[j].replaceAll("'", "\\\\'")).append("', ");
            else stringBuilder.append(values[j]).append(", ");
            j++;
        }
        return stringBuilder.substring(0, stringBuilder.length()-2);
    }

    public List<String[]> read(int columnIndex, int id) {
        return read(columnIndex, String.valueOf(id), true);
    }

    public List<String[]> read(int columnIndex, String id) {
        return read(columnIndex, id, false);
    }

    private synchronized List<String[]> read(int columnIndex, String id, boolean isNumber) {
        List<String[]> strings = new ArrayList<>();
        try (Connection conn = DriverManager.getConnection(url, userName, password);
        Statement statement = conn.createStatement()) {
            if (!isNumber) id = "'" + id + "'";
            ResultSet set = statement.executeQuery("SELECT * FROM %s WHERE %s=%s".formatted(table.toString(), columns.get(columnIndex), id));
            while (set.next()) {
                String[] line = new String[columns.size()];
                for (int i = 0; i < columns.size(); i++) {
                    line[i] = set.getString(i+1);
                }
                strings.add(line);
            }
        } catch (SQLException e) {
            LOGGER.warning("SQL Exception\n" + ConsoleFormatter.formatStackTrace(e));
        }
        return strings;
    }

    public List<String> readLine(int columnIndex, String id) {
        return readLine(columnIndex, id, false);
    }

    public List<String> readLine(int columnIndex, int id) {
        return readLine(columnIndex, String.valueOf(id), true);
    }

    private synchronized List<String> readLine(int columnIndex, String id, boolean isNumber) {
        List<String> strings = new ArrayList<>();
        try (Connection conn = DriverManager.getConnection(url, userName, password);
             Statement statement = conn.createStatement()) {
            if (!isNumber) id = "'" + id + "'";
            String sql = "SELECT * FROM %s WHERE %s=%s;".formatted(table.toString(), columns.get(columnIndex), id);
            ResultSet set = statement.executeQuery(sql);
            if (set.next()) {
                for (int j = 0; j < columns.size(); j++) {
                    strings.add(set.getString(j + 1));
                }
            }
        } catch (SQLException e) {
            LOGGER.warning("SQL Exception\n" + ConsoleFormatter.formatStackTrace(e));
        }
        return strings;
    }

    public synchronized void write(String[] values) {
        try (Connection conn = DriverManager.getConnection(url, userName, password);
        Statement statement = conn.createStatement()){
            StringBuilder stringBuilder = new StringBuilder();
            for (int i = 0; i < columns.size(); i++) {
                if (isAutoincrement.get(i).equals("NO")) {
                    stringBuilder.append(columns.get(i)).append(", ");
                }
            }
            String fields = stringBuilder.substring(0, stringBuilder.length()-2);
            String sql = ("INSERT INTO %s (%s) values(%s);")
                    .formatted(table.toString(), fields, formatValues(values));
            statement.executeUpdate(sql);
        } catch (SQLException e) {
            LOGGER.warning("SQL Exception\n" + ConsoleFormatter.formatStackTrace(e));
        }
    }

    public synchronized List<String[]> sqlRead(String sql) {
        List<String[]> strings = new ArrayList<>();
        try (Connection conn = DriverManager.getConnection(url, userName, password);
        Statement statement = conn.createStatement()){
            ResultSet set = statement.executeQuery(sql);
            while (set.next()) {
                String[] line = new String[columns.size()];
                for (int i = 0; i < columns.size(); i++) {
                    line[i] = set.getString(i+1);
                }
                strings.add(line);
            }
        } catch (SQLException e) {
            LOGGER.warning("SQL exception\n" + ConsoleFormatter.formatStackTrace(e));
        }
        return strings;
    }

    public synchronized void remove(int columnIndex, String id) {
        try (Connection conn = DriverManager.getConnection(url, userName, password);
             Statement statement = conn.createStatement()){
            statement.executeUpdate((dataTypes.get(columnIndex).equals("12") ? "DELETE FROM %s WHERE %s='%s'" : "DELETE FROM %s WHERE %s=%s")
                    .formatted(table.toString(), columns.get(columnIndex), id));
        } catch (SQLException e) {
            LOGGER.warning("SQL Exception\n" + ConsoleFormatter.formatStackTrace(e));
        }
    }

    public synchronized void update(int selectColumnInd, String id, int columnInd, String val) {
        try (Connection conn = DriverManager.getConnection(url, userName, password);
        Statement statement = conn.createStatement()){
            statement.executeUpdate("UPDATE %s SET %s=%s WHERE %s=%s".formatted(table.toString(), columns.get(columnInd),
                    dataTypes.get(columnInd).equals("12") ? "'" + val + "'" : val, columns.get(selectColumnInd),
                    dataTypes.get(selectColumnInd).equals("12") ? "'" + id + "'" : id));
        } catch (SQLException e) {
            LOGGER.warning("SQL Exception\n" + ConsoleFormatter.formatStackTrace(e));
        }
    }
}
