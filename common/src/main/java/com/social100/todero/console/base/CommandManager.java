package com.social100.todero.console.base;

import com.social100.todero.common.message.MessageContainer;

public interface CommandManager {
    String getHelpMessage(String plugin, String command);
    boolean process(MessageContainer line);
    String reload();
    String unload();
    String load();
    void terminate();
    String[] generateAutocompleteStrings();
}