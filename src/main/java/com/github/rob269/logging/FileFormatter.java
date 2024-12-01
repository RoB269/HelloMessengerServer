package com.github.rob269.logging;

import java.util.logging.Formatter;
import java.util.logging.LogRecord;

public class FileFormatter extends Formatter {
    @Override
    public String format(LogRecord record) {
        return record.getLevel().getName() + "(" + Thread.currentThread().getName() + "): " + record.getMessage().replaceAll("\n", "\n\t") + "\n";
    }
}
