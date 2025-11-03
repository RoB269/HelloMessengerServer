package com.github.rob269.helloMessengerServer.io;

import java.io.IOException;
import java.math.BigInteger;

public interface Batch {

    void write(long message) throws IOException;

    void write(String message) throws IOException;

    void write(BigInteger message) throws IOException;

    void write(byte[] message) throws IOException;
}
