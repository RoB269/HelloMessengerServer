package com.github.rob269.helloMessengerServer.logging;

import java.time.LocalDateTime;
import java.util.logging.Formatter;
import java.util.logging.LogRecord;

public class FileFormatter extends Formatter {
    @Override
    public String format(LogRecord record) {
        LocalDateTime dateTime = LocalDateTime.now();
        return "[" + dateTime.toString().substring(11) + "]" +  record.getLevel().getName() + "(" + Thread.currentThread().getName() + "): " + record.getMessage().replaceAll("\n", "\n\t") + "\n";
    }
}
