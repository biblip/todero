package com.social100.todero.console.base;

public interface CommandManager {
    String getHelpMessage(String plugin, String command);
    String process(String line);
    String reload();
    String unload();
    String load();
    void terminate();
    String[] generateAutocompleteStrings();
}