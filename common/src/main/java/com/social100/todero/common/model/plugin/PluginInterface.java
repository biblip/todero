package com.social100.todero.common.model.plugin;

import com.social100.todero.common.command.CommandContext;

public interface PluginInterface {
    //void subscribeEvents();
    Component getComponent();
    Boolean execute(String pluginName, String command, CommandContext context);
}
