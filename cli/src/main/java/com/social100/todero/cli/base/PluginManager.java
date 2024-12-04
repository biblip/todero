package com.social100.todero.cli.base;

import com.social100.todero.common.model.plugin.Command;
import com.social100.todero.common.model.plugin.Component;
import com.social100.todero.common.model.plugin.Plugin;
import com.social100.todero.common.model.plugin.PluginInterface;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PluginManager {
    final private Map<String, Plugin> plugins = new HashMap<>();
    final private List<PluginContext> pluginContextList = new ArrayList<>();
    private final File pluginsDir;
    private HelpWrapper helpWrapper;

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

        for (PluginContext context : pluginContextList) {
            plugins.putAll(context.getPlugins());
        }

        this.helpWrapper = new HelpWrapper(plugins);
    }

    public String getHelp(String pluginName, String commandName, OutputType outputType) {
        return helpWrapper.getHelp(pluginName, commandName, outputType);
    }

    public Object execute(String pluginName, String command, String[] commandArgs) {
        // Find the specified plugin
        Plugin plugin = plugins.get(pluginName);

        if (plugin == null) {
            return "Plugin with name '" + pluginName + "' not found.";
        }

        // Get the associated Component and validate the command
        Component component = plugin.getComponent();
        if (component == null || component.getCommands() == null) {
            return "Plugin '" + pluginName + "' has no commands defined.";
        }

        if (!component.getCommands().containsKey(command)) {
            return "Command '" + command + "' does not exist in plugin '" + pluginName + "'.";
        }

        // Get the associated PluginContext
        PluginInterface pluginInstance = plugin.getPluginInstance();

        if (pluginInstance == null) {
            return "Plugin '" + pluginName + "' has no associated Instance.";
        }

        // Call the execute method on the PluginContext
        try {
            return pluginInstance.execute(command, commandArgs);
        } catch (Exception e) {
            return "Failed to execute command '" + command + "' on plugin '" + pluginName + "': " + e.getMessage();
        }
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

    public List<String> generateAutocompleteStrings() {
        List<String> completions = new ArrayList<>();

        // Iterate over all plugins
        for (Map.Entry<String, Plugin> pluginEntry : plugins.entrySet()) {
            String pluginName = pluginEntry.getKey();
            Plugin plugin = pluginEntry.getValue();

            // Add the plugin name as a standalone completion
            completions.add(pluginName);

            // Iterate over the commands in the plugin's component
            Component component = plugin.getComponent();
            if (component != null && component.getCommands() != null) {
                for (Map.Entry<String, Command> commandEntry : component.getCommands().entrySet()) {
                    String commandName = commandEntry.getKey();
                    Command command = commandEntry.getValue();

                    // Add plugin -> command suggestion
                    completions.add(pluginName + " " + commandName);

                    // Add detailed command suggestion (if needed)
                    if (command.getDescription() != null) {
                        completions.add(pluginName + " " + commandName + " - " + command.getDescription());
                    }
                }
            }
        }

        // Return the list of completions
        return completions;
    }
}
