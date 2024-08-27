package com.github.rob269.logging;

import java.util.logging.Formatter;
import java.util.logging.LogRecord;

public class ConsoleFormater extends Formatter {
    @Override
    public String format(LogRecord record) {

        return record.getLevel().getName() + ": " + record.getMessage() + "\n";
    }
}
