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
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class PluginManager implements PluginManagerInterface {
    final private Map<String, Plugin> plugins = new HashMap<>();
    final private Map<String, Plugin> pluginsAll = new HashMap<>();
    final List<PluginContext> pluginContextList = new ArrayList<>();
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

        int threadCount = Math.max(2, Runtime.getRuntime().availableProcessors());
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);

        List<CompletableFuture<Optional<PluginContext>>> futures = new ArrayList<>();

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

            // Launch async task
            CompletableFuture<Optional<PluginContext>> future = CompletableFuture.supplyAsync(() -> {
                try {
                    PluginContext context = new PluginContext(pluginDir.toPath(), pluginJar, this.type, eventListener,
                        (id, line) -> {
                        String[] mm = id.split(";");
                        String pluginName = mm[1];
                        String command = mm[2];

                        System.out.println("-----------------------------------");
                        System.out.println("jar: " + id);
                        System.out.println("component: " + pluginName);
                        System.out.println("command: " + command);
                        System.out.println("line: " + line);
                        System.out.println("-----------------------------------");

                        String[] commandArgs = List.of(line).toArray(new String[]{});

                        CommandContext commandContext = CommandContext.builder()
                            .sourceId("333")
                            .args(commandArgs)
                            .agents(getAgents())
                            .tools(getTools())
                            .pluginManager(this)
                            .build();

                        //commandContext.setListener(context::respond);

                        execute(pluginName, command, commandContext, true);
                    });
                    return Optional.of(context);
                } catch (Exception e) {
                    System.err.printf("Error processing plugin in %s (%s):%n", pluginDir.getName(), pluginJar.getName());
                    e.printStackTrace();
                    return Optional.empty();
                }
            }, executor);

            futures.add(future);
        }

        // Wait for all to complete, then process
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
            .thenRun(() -> {
                for (CompletableFuture<Optional<PluginContext>> future : futures) {
                    future.join().ifPresent(context -> {
                        pluginContextList.add(context);
                        context.getPlugins().entrySet().stream()
                            .filter(e -> Objects.equals(e.getValue().getType(), type))
                            .filter(e -> e.getValue().isVisible())
                            .forEach(e -> plugins.put(e.getKey(), e.getValue()));
                        pluginsAll.putAll(context.getPlugins());
                    });
                }

                this.helpWrapper = new HelpWrapper(plugins);
                executor.shutdown();
            })
            .exceptionally(ex -> {
                System.err.println("Unexpected error during plugin initialization: " + ex.getMessage());
                ex.printStackTrace();
                executor.shutdown();
                return null;
            });
    }

    @Override
    public String getHelp(String pluginName, String commandName, OutputType outputType) {
        return helpWrapper.getHelp(pluginName, commandName, outputType);
    }

    public List<String> getAgents() {
        return pluginsAll.entrySet().stream()
            .filter(e -> ServerType.AI.equals(e.getValue().getType()))
            .map(Map.Entry::getKey)
            .toList();
    }

    public List<String> getTools() {
        return pluginsAll.entrySet().stream()
            .filter(e -> ServerType.AIA.equals(e.getValue().getType()))
            .map(Map.Entry::getKey)
            .toList();
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
    public void execute(String pluginName, String command, CommandContext context, boolean usePluginsAll) {
        // Find the specified plugin
        Plugin plugin = (usePluginsAll ? pluginsAll : plugins).get(pluginName);

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
