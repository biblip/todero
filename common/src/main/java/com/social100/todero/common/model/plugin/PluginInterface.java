package com.social100.todero.common.model.plugin;

public interface PluginInterface {
    Component getComponent();
    Object execute(String pluginName, String command, String[] commandArgs);
}
