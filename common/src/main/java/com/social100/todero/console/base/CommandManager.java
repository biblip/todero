package com.social100.todero.console.base;

import java.util.function.Consumer;

public interface CommandManager {
    String getHelpMessage(String plugin, String command);
    String process(String line);
    String process(String line, Consumer<String> consumer);
    String reload();
    String unload();
    String load();
    void terminate();
    String[] generateAutocompleteStrings();
}