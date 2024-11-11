package com.social100.todero.cli.base;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

public class PluginManager {
    final private List<PluginContext> pluginContextList = new ArrayList<>();
    private File pluginsDir;

    public PluginManager(File dir) {
        this.pluginsDir = dir;
        initialize();

    }

    public void initialize() {
        if (pluginsDir == null || !pluginsDir.isDirectory()) {
            System.err.println("Invalid plugins directory.");
            return;
        }

        File[] pluginFiles = pluginsDir.listFiles((_ignore, name) -> name.endsWith(".jar"));
        if (pluginFiles == null) {
            return;
        }

        for (File file : pluginFiles) {
            try {
                pluginContextList.add(new PluginContext(file));
            } catch (Exception e) {
                System.err.println("Error processing plugin JAR: " + file.getName());
                e.printStackTrace();
            }
        }
    }

    public String getHelpMessage(String command, String[] commandArgs) {
        StringBuilder helpMessage = new StringBuilder();

        pluginContextList.forEach(manager -> helpMessage.append(manager.getHelpMessage()));

        return helpMessage.toString().trim();
    }

    public String execute(String plugin, String command, String[] commandArgs) {
        for (PluginContext manager : pluginContextList) {
            if (manager.hasName(plugin)) {
                if (command == null) {
                    return manager.getHelpMessage();
                }
                if (manager.hasCommand(command)) {
                    return manager.execute(command, commandArgs);
                }
            }
        }
        return "Command Not Found Error";
    }

    public void clear() {
        for (PluginContext manager : pluginContextList) {
            manager.cleanup();
        }
        pluginContextList.clear();
    }

    public void reload() {
        clear();
        initialize();
    }

    public String[] getAllCommandNames() {
        return pluginContextList.stream()
                .flatMap(pluginContext -> Stream.of(pluginContext.getAllCommandNames()))
                .distinct().toArray(String[]::new);
    }
}
