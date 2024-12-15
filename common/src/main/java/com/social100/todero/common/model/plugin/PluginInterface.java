package com.social100.todero.common.model.plugin;

public interface PluginInterface {
    //void subscribeEvents();
    Component getComponent();
    Boolean execute(String pluginName, String command, String[] commandArgs);
}
