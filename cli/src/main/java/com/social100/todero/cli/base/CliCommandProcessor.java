package com.social100.todero.cli.base;

import com.social100.todero.common.config.AppConfig;

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
    public void process(String line) {
        String output = commandManager.execute(line);
        if (!output.isEmpty()) {
            System.out.print(output);
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
