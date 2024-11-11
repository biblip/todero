package com.social100.todero.aia;

import com.social100.todero.common.PluginInterface;

import java.util.HashMap;
import java.util.Map;

public class AIAProtocolPlugin implements PluginInterface {
    Map<String, String> commandMap = new HashMap<>();

    public AIAProtocolPlugin() {
        commandMap.put("test", "Does the test");
    }

    @Override
    public Boolean hasCommand(String command) {
        return command.equals("test");
    }

    @Override
    public String execute(String command, String[] commandArgs) {
        return "Test Ok" + (commandArgs.length>0 ? " : " + commandArgs[0] : "");
    }

    @Override
    public String name() {
        return "aia";
    }

    @Override
    public String description() {
        return "AIA Protocol Plugin";
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
