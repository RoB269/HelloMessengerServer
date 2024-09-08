package com.github.rob269.io;

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
    private final Tables table;

    public enum Tables {
        USER_RSA_KEYS("user_rsa_keys");
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
        columns = getMetaData("COLUMN_NAME");
    }

    private synchronized List<String> getMetaData(String get) {
        List<String> columns = new ArrayList<>();
        try (Connection connection = DriverManager.getConnection(url, userName, password)){
            DatabaseMetaData md = connection.getMetaData();
            ResultSet set = md.getColumns(null, null, table.toString(), null);
            while (set.next())
                columns.add(set.getString(get));
        } catch (SQLException e) {
            LOGGER.warning("SQL Exception");
            e.printStackTrace();
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
            LOGGER.warning("SQL Exception");
            e.printStackTrace();
        }
        return false;
    }

    private String formatValues(String[] values) {
        List<String> isAutoincrement = getMetaData("IS_AUTOINCREMENT");
        List<String> dataType = getMetaData("DATA_TYPE");
        for (int i = 0; i < this.columns.size(); i++) {
            if (isAutoincrement.get(i).equals("YES")) {
                dataType.set(i, "N");
            }
        }
        int j = 0;
        StringBuilder stringBuilder = new StringBuilder();
        for (int i = 0; i < isAutoincrement.size(); i++) {
            if (isAutoincrement.get(i).equals("YES")) continue;
            if (dataType.get(i).equals("12")) stringBuilder.append("'").append(values[j]).append("', ");
            else stringBuilder.append(values[j]).append(", ");
            j++;
        }
        return stringBuilder.substring(0, stringBuilder.length()-2);
    }

    public synchronized ResultSet read(int id) {
        ResultSet set = null;
        try (Connection conn = DriverManager.getConnection(url, userName, password);
        Statement statement = conn.createStatement()) {
            set = statement.executeQuery("SELECT * FROM %s WHERE id=%d".formatted(table.toString(), id));
        } catch (SQLException e) {
            LOGGER.warning("SQL Exception");
            e.printStackTrace();
        }
        return set;
    }

    public List<String> readLine(int columnIndex, String id) {
        return readLine(columnIndex, id, false);
    }

    public List<String> readLine(int columnIndex, int id) {
        return readLine(columnIndex, String.valueOf(id), true);
    }

    private synchronized List<String> readLine(int columnIndex, String id, boolean isNumber) {
        List<String> strings = new ArrayList<>();
        if (isExist(columnIndex, id)) {
            try (Connection conn = DriverManager.getConnection(url, userName, password);
                 Statement statement = conn.createStatement()) {
                if (!isNumber) id = "'" + id + "'";
                String sql = "SELECT * FROM %s WHERE %s=%s;".formatted(table.toString(), columns.get(columnIndex), id);
                ResultSet set = statement.executeQuery(sql);
                if (set.next()) {
                    for (int j = 0; j < columns.size(); j++) {
                        strings.add(set.getString(j+1));
                    }
                }
            } catch (SQLException e) {
                LOGGER.warning("SQL Exception");
                e.printStackTrace();
            }
        }
        return strings;
    }

    public synchronized void write(String[] values) {
        try (Connection conn = DriverManager.getConnection(url, userName, password);
        Statement statement = conn.createStatement()){

            List<String> isAutoincrement = getMetaData("IS_AUTOINCREMENT");
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
            LOGGER.warning("SQL Exception");
            e.printStackTrace();
        }
    }
}
