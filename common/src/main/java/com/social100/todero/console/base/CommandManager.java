package com.social100.todero.console.base;

import com.social100.todero.common.message.MessageContainer;

import java.util.function.Consumer;

public interface CommandManager {
    String getHelpMessage(String plugin, String command);
    boolean process(MessageContainer line);
    boolean process(MessageContainer line, Consumer<String> consumer);
    String reload();
    String unload();
    String load();
    void terminate();
    String[] generateAutocompleteStrings();
}