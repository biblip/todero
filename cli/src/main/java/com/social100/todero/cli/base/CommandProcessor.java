package com.social100.todero.cli.base;

public interface CommandProcessor {
    void open();
    void process(String line);
    void close();
    CommandManager getCommandManager();
}
