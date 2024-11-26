package com.social100.todero.cli.base;

import java.util.regex.Pattern;

public interface CommandProcessor {
    void open();
    void process(Pattern pattern, String line);
    void close();
    CommandManager getCommandManager();
}
