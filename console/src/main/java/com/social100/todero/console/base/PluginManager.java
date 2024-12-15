package com.social100.todero.console.base;

import com.social100.todero.common.channels.EventChannel;
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
    final private EventChannel.EventListener eventListener;

    public PluginManager(File dir, EventChannel.EventListener eventListener) {
        this.pluginsDir = dir;
        this.eventListener = eventListener;
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
                EventChannel.EventListener localEventListener = new EventChannel.EventListener() {
                    @Override
                    public void onEvent(String eventName, String message) {
                        System.out.println("Observer in PluginManager: " + eventName + " -> " + message);
                        eventListener.onEvent(eventName, message);
                    }
                };
                pluginContextList.add(new PluginContext(file, localEventListener));

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

    /**
     *
     *
     * @return
     */
    public Component getComponent() {
        return plugins.keySet()
                .stream()
                .filter(key -> key != null && !key.isEmpty())
                .map(plugins::get)
                .findFirst()
                .get().getComponent();
    }

    public Object execute(String pluginName, String command, String[] commandArgs) {
        // Find the specified plugin
        Plugin plugin = plugins.get(pluginName);

        if (plugin == null) {
            return "Plugin with name '" + pluginName + "' not found.";
        }

        // Get the associated Component and validate the command
        Component component = plugin.getComponent();
        if (component == null || component.getCommands() == null || component.getCommands().isEmpty()) {
            return "Plugin '" + pluginName + "' has no commands defined.";
        }

        // Search for the command in the nested structure
        Command foundCommand = null;
        for (Map.Entry<String, Map<String, Command>> groupEntry : component.getCommands().entrySet()) {
            Map<String, Command> groupCommands = groupEntry.getValue();
            if (groupCommands.containsKey(command)) {
                foundCommand = groupCommands.get(command);
                break;
            }
        }

        if (foundCommand == null) {
            return "Command '" + command + "' does not exist in plugin '" + pluginName + "'.";
        }

        // Get the associated PluginContext
        PluginInterface pluginInstance = plugin.getPluginInstance();

        if (pluginInstance == null) {
            return "Plugin '" + pluginName + "' has no associated Instance.";
        }

        // Call the execute method on the PluginInstance
        try {
            return pluginInstance.execute(pluginName, command, commandArgs);
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
                // Iterate over groups
                for (Map.Entry<String, Map<String, Command>> groupEntry : component.getCommands().entrySet()) {
                    String groupName = groupEntry.getKey();
                    Map<String, Command> groupCommands = groupEntry.getValue();

                    // Iterate over commands within the group
                    for (Map.Entry<String, Command> commandEntry : groupCommands.entrySet()) {
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
        }

        return completions;
    }
}
