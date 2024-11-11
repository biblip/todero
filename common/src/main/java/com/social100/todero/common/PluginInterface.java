package com.social100.todero.common;

public interface PluginInterface {
    Boolean hasCommand(String command);
    String execute(String command, String[] commandArgs);
    String name();
    String description();
    String[] getAllCommandNames();
    String getHelpMessage();
}
