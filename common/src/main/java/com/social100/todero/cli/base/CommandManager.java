package com.social100.todero.cli.base;

public interface CommandManager {
    String getHelpMessage(String plugin, String command);
    String execute(String line);
    String reload();
    String unload();
    String load();
    void terminate();
    String[] generateAutocompleteStrings();
}