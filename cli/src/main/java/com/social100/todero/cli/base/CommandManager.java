package com.social100.todero.cli.base;

import com.social100.todero.common.Constants;
import com.social100.todero.common.config.AppConfig;

import java.io.File;
import java.util.Optional;

public class CommandManager {

    PluginManager pluginManager;

    public CommandManager(AppConfig appConfig) {
        String pluginDirectory = Optional.ofNullable(appConfig.getApp().getPlugins().getDir())
                .orElseThrow(() -> new IllegalArgumentException("Wrong value in plugin directory"));
        pluginManager = new PluginManager(new File(pluginDirectory));
    }

    public String getHelpMessage(String command, String[] commandArgs) {
        return pluginManager.getHelpMessage(command, commandArgs);
    }

    public String execute(String rootCommand, String command, String[] commandArgs) {
        switch (rootCommand) {
            case Constants.CLI_COMMAND_HELP:
                return getHelpMessage(command, commandArgs).trim();
            case Constants.CLI_COMMAND_LOAD:
                return load();
            case Constants.CLI_COMMAND_UNLOAD:
                return unload();
            case Constants.CLI_COMMAND_RELOAD:
                return reload();
            default:
                return pluginManager.execute(rootCommand, command, commandArgs);
        }
    }

    public String reload() {
        pluginManager.reload();
        return "Ok";
    }

    public String unload() {
        pluginManager.clear();
        return "Ok";
    }

    public String load() {
        return "Not implemented yet";
    }

    public void terminate() {
        pluginManager.clear();
    }

    public String[] getAllCommandNames() {
        return pluginManager.getAllCommandNames();
    }
}