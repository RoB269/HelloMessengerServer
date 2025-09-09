package com.github.rob269.helloMessengerServer.logging;

import java.io.*;
import java.nio.charset.StandardCharsets;

public class LogFilter extends FilterOutputStream {
    private final ByteArrayOutputStream buffer = new ByteArrayOutputStream();

    public LogFilter(OutputStream out) {
        super(out);
    }

    @Override
    public void write(int b) throws IOException {
        if (b == '\n') {
            processLine();
        } else {
            buffer.write(b);
        }
    }

    @Override
    public void write(byte[] b, int off, int len) throws IOException {
        for (int i = off; i < off + len; i++) {
            write(b[i]);
        }
    }

    private void processLine() throws IOException {
        String line = buffer.toString(StandardCharsets.UTF_8);
        buffer.reset();
        if (!line.contains("jdk.internal.event")) {
            super.out.write((line + "\n").getBytes(StandardCharsets.UTF_8));
        }
    }
}
