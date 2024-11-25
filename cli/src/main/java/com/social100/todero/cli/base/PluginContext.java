package com.social100.todero.cli.base;

import com.social100.todero.common.model.plugin.Command;
import com.social100.todero.common.model.plugin.Plugin;
import com.social100.todero.common.model.plugin.PluginInterface;
import com.social100.todero.common.model.plugin.PluginSection;
import org.reflections.Reflections;
import org.reflections.scanners.SubTypesScanner;
import org.reflections.scanners.TypeAnnotationsScanner;
import org.reflections.util.ConfigurationBuilder;

import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public class PluginContext {
    final private Map<String,Plugin> plugins = new HashMap<>();
    final private PluginExecutor pluginExecutor = new PluginExecutor(3);


    public PluginContext(File pluginJar) throws Exception {
        initializePlugin(pluginJar);
    }

    private void initializePlugin(File pluginJar) throws Exception {
        // Convert the File to a URL
        URL jarUrl = pluginJar.toURI().toURL();
        URLClassLoader pluginClassLoader = new URLClassLoader(new URL[]{jarUrl}, getClass().getClassLoader());

        // Initialize Reflections with the URLClassLoader
        Reflections reflections = new Reflections(new ConfigurationBuilder()
                .addUrls(jarUrl)
                .addClassLoader(pluginClassLoader)
                .setScanners(new SubTypesScanner(), new TypeAnnotationsScanner()));

        // Discover and instantiate Command implementations
        Set<Class<? extends PluginInterface>> commandClasses = reflections.getSubTypesOf(PluginInterface.class);
        for (Class<? extends PluginInterface> commandClass : commandClasses) {
            if (!commandClass.isInterface()) {

                Plugin plugin = Plugin.builder()
                        .file(pluginJar)
                        .classLoader(pluginClassLoader)
                        .pluginClass(commandClass)
                        .pluginInstance(commandClass.getDeclaredConstructor().newInstance())
                        .build();
                plugins.put(plugin.getPluginInstance().name(), plugin);
                Map<String, Command> pluginCommandMap = new HashMap<>();

                PluginSection pluginSection = PluginSection
                        .builder()
                        .name(Optional.ofNullable(plugin.getPluginInstance().name()).orElse(""))
                        .commands(pluginCommandMap)
                        .build();
                /*
                plugin.getPluginInstance()
                        .commands()
                        .forEach(commandMethod -> {
                            Command command = Command
                                    .builder()
                                    .command(commandMethod.name())
                                    .description(commandMethod.description())
                                    .commandMethod(commandMethod)
                                    .build();
                            pluginCommandMap.put(commandMethod.name(), command);
                        });
                 */
            }
        }
    }

    public String getHelpMessage() {
        StringBuilder sb = new StringBuilder();
        plugins.values().forEach(plugin -> {
            sb.append(Optional.ofNullable(plugin.getPluginInstance().getHelpMessage()).orElse(""));
        });
        return sb.toString();
    }

    // Cleanup method to properly close the plugin class loader when no longer needed
    public void cleanup() {
        /*
        try {
            plugin.getClassLoader().close();
            pluginExecutor.shutdown();
        } catch (Exception e) {
            e.printStackTrace();
        }
         */
    }

    public Boolean hasName(String plugin ) {
        return plugins.values().stream().anyMatch(p -> plugin.equals(p.getPluginInstance().name()));
    }

    public Boolean hasCommand(String command) {
        return plugins.values().stream().anyMatch(p -> Optional.ofNullable(p.getPluginInstance().hasCommand(command)).orElse(false));
    }

    public Object execute(String pluginName, String command, String[] commandArgs) {
        Optional<Plugin> selectedPlugin = plugins
                .values()
                .stream()
                .filter(p -> isPluginAndHasCommand(pluginName, command, p))
                .findFirst();
        if (selectedPlugin.isEmpty()) {
            return "Command Not Found";
        }
        return selectedPlugin.get().getPluginInstance().execute(command, commandArgs);
    }

    private static boolean isPluginAndHasCommand(String pluginName, String command, Plugin p) {
        return pluginName.equals(Optional.ofNullable(p.getPluginInstance().name()).orElse(""))
                && Optional.ofNullable(p.getPluginInstance().hasCommand(command)).orElse(false);
    }

    public String[] getAllCommandNames() {
        //return plugins.getAllCommandNames();
        return new String[] { "Hellow!" };
    }
}