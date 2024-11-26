package com.social100.todero.cli.base;

import com.social100.todero.common.config.AppConfig;

import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CliCommandProcessor implements CommandProcessor {
    final private AppConfig appConfig;
    private CommandManager commandManager;

    public CliCommandProcessor(AppConfig appConfig) {
        this.appConfig = appConfig;
    }

    @Override
    public void open() {
        if (this.commandManager == null) {
            this.commandManager = new CommandManager(this.appConfig);
        } else {
            throw new RuntimeException("CommandManager already created");
        }
    }

    @Override
    public void process(Pattern pattern, String line) {
        Matcher matcher = pattern.matcher(line);
        ArrayList<String> arguments = new ArrayList<>();

        while (matcher.find()) {
            arguments.add(matcher.group(1).replace("\"", ""));
        }

        if (!arguments.isEmpty()) {
            String pluginName = arguments.remove(0);
            String command = null;
            String[] commandArgs = {};
            if (!arguments.isEmpty()) {
                command = arguments.remove(0);
            }
            if (!arguments.isEmpty()) {
                commandArgs = arguments.toArray(new String[0]);
            }
            String output = commandManager.execute(pluginName, command, commandArgs);
            System.out.println(output);
        }
    }

    @Override
    public void close() {
        this.commandManager.terminate();
    }

    @Override
    public CommandManager getCommandManager() {
        return this.commandManager;
    }
}
