package com.social100.todero.console.base;

import com.social100.todero.common.base.PluginManagerInterface;
import com.social100.todero.common.channels.EventChannel;
import com.social100.todero.common.command.CommandContext;
import com.social100.todero.common.config.ServerType;
import com.social100.todero.common.model.plugin.Command;
import com.social100.todero.common.model.plugin.Component;
import com.social100.todero.common.model.plugin.Plugin;
import com.social100.todero.common.model.plugin.PluginInterface;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class PluginManager implements PluginManagerInterface {
    final private Map<String, Plugin> plugins = new HashMap<>();
    final private List<PluginContext> pluginContextList = new ArrayList<>();
    private final File pluginsDir;
    private HelpWrapper helpWrapper;
    final private EventChannel.EventListener eventListener;
    final private ServerType type;

    public PluginManager(File dir, ServerType type, EventChannel.EventListener eventListener) {
        this.pluginsDir = dir;
        this.eventListener = eventListener;
        this.type = type;
        initialize();

    }

    public void initialize() {
        if (pluginsDir == null || !pluginsDir.isDirectory()) {
            System.err.println("Invalid plugins directory.");
            return;
        }

        File[] pluginDirs = pluginsDir.listFiles(File::isDirectory);
        if (pluginDirs == null) return;

        for (File pluginDir : pluginDirs) {
            File[] jarFiles = pluginDir.listFiles((dir, name) -> name.endsWith(".jar"));
            if (jarFiles == null || jarFiles.length == 0) {
                System.err.printf("Skipping %s: no .jar file found.%n", pluginDir.getName());
                continue;
            }

            if (jarFiles.length > 1) {
                System.err.printf("Skipping %s: multiple .jar files found.%n", pluginDir.getName());
                continue;
            }

            File pluginJar = jarFiles[0];
            try {
                pluginContextList.add(new PluginContext(pluginDir.toPath(), pluginJar, this.type, eventListener));
            } catch (Exception e) {
                System.err.printf("Error processing plugin in %s (%s):%n", pluginDir.getName(), pluginJar.getName());
                e.printStackTrace();
            }
        }

        for (PluginContext context : pluginContextList) {
            context.getPlugins().entrySet().stream()
                .filter(e -> Objects.equals(e.getValue().getType(), type))
                .forEach(e -> plugins.put(e.getKey(), e.getValue()));
        }

        this.helpWrapper = new HelpWrapper(plugins);
    }

    @Override
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

    @Override
    public void execute(String pluginName, String command, CommandContext context) {
        // Find the specified plugin
        Plugin plugin = plugins.get(pluginName);

        if (plugin == null) {
            context.respond("Plugin with name '" + pluginName + "' not found.");
            return;
        }

        // Get the associated Component and validate the command
        Component component = plugin.getComponent();
        if (component == null || component.getCommands() == null || component.getCommands().isEmpty()) {
            context.respond("Plugin '" + pluginName + "' has no commands defined.");
            return;
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
            context.respond("Command '" + command + "' does not exist in plugin '" + pluginName + "'.");
            return;
        }

        // Get the associated PluginContext
        PluginInterface pluginInstance = plugin.getPluginInstance();

        if (pluginInstance == null) {
            context.respond("Plugin '" + pluginName + "' has no associated Instance.");
            return;
        }

        // Call the execute method on the PluginInstance
        try {
            pluginInstance.execute(pluginName, command, context);
        } catch (Exception e) {
            context.respond("Failed to execute command '" + command + "' on plugin '" + pluginName + "': " + e.getMessage());
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

    @Override
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
