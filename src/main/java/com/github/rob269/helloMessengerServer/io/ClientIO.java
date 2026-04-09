package com.github.rob269.helloMessengerServer.io;

import java.io.IOException;
import java.math.BigInteger;
import java.sql.SQLException;

public interface ClientIO {
    void init() throws IOException, SQLException;

    BigInteger readBigint() throws IOException;

    byte readCommand() throws IOException;

    String readString(boolean log) throws IOException;

    default String readString() throws IOException {
        return readString(true);
    }

    void setUsername(String username);

    String getUsername();

    long readLong(boolean log) throws IOException;

    void writeCommand(int message) throws IOException;

    void writeCommand(int message, int packageCount) throws IOException;

    Batch writeBatch(int command, int batchSize, boolean log);

    boolean isClosed();

    void close();
}
