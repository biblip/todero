package com.social100.todero.plugin;

import com.social100.todero.common.PluginInterface;

import java.util.HashMap;
import java.util.Map;

public class SimplePlugin implements PluginInterface {
    Map<String, String> commandMap = new HashMap<>();

    public SimplePlugin() {
        commandMap.put("ping", "Does the ping");
    }

    @Override
    public Boolean hasCommand(String command) {
        return command.equals("ping");
    }

    @Override
    public String execute(String command, String[] commandArgs) {
        return "Ping Ok" + (commandArgs.length>0 ? " : " + commandArgs[0] : "");
    }

    @Override
    public String name() {
        return "simple";
    }

    @Override
    public String description() {
        return "Simple Plugin";
    }

    @Override
    public String[] getAllCommandNames() {
        return commandMap.keySet().toArray(new String[0]);
    }

    @Override
    public String getHelpMessage() {
        StringBuilder helpMessage = new StringBuilder();
        for (String commandName : commandMap.keySet()) {
            helpMessage.append(String.format("-  %-15s : %s\n", commandName, commandMap.get(commandName)));
        }
        return helpMessage.toString();
    }
}