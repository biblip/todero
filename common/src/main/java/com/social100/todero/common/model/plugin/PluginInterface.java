package com.social100.todero.common.model.plugin;

public interface PluginInterface {
    Boolean hasCommand(String command);
    Object execute(String command, String[] commandArgs);
    String name();
    String description();
    String[] getAllCommandNames();
    String getHelpMessage();
}
