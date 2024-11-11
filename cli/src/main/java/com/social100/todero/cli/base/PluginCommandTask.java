package com.social100.todero.cli.base;

import com.social100.todero.common.PluginInterface;

import java.util.concurrent.Callable;

public class PluginCommandTask implements Callable<String> {
    final PluginInterface pluginInterface;
    private final String command;
    String[] commandArgs;

    public PluginCommandTask(PluginInterface pluginInterface, String command, String[] commandArgs) {
        this.pluginInterface = pluginInterface;
        this.command = command;
        this.commandArgs = commandArgs;
    }

    @Override
    public String call() {
        try {
            return pluginInterface.execute(command, commandArgs);
        } catch (Exception e) {
            Thread.currentThread().interrupt();
            return "Error";
        }
    }
}
