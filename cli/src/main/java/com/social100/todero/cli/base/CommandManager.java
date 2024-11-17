package com.social100.todero.cli.base;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.social100.todero.common.Constants;
import com.social100.todero.common.config.AppConfig;

import java.io.File;
import java.util.Optional;

public class CommandManager {
    final static private ObjectMapper objectMapper = new ObjectMapper();
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
        Object output = switch (rootCommand) {
            case Constants.CLI_COMMAND_HELP -> getHelpMessage(command, commandArgs).trim();
            case Constants.CLI_COMMAND_LOAD -> load();
            case Constants.CLI_COMMAND_UNLOAD -> unload();
            case Constants.CLI_COMMAND_RELOAD -> reload();
            default -> pluginManager.execute(rootCommand, command, commandArgs);
        };
        if (output instanceof String) {
            return (String)output;
        }
        return toJson(output);
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

    private static String toJson(Object result) {
        try {
            return objectMapper.writeValueAsString(result);
        } catch (Exception e) {
            throw new RuntimeException("Failed to serialize result to JSON", e);
        }
    }
}