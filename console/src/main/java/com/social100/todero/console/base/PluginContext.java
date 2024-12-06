package com.social100.todero.console.base;

import com.social100.todero.common.model.plugin.Command;
import com.social100.todero.common.model.plugin.Component;
import com.social100.todero.common.model.plugin.Plugin;
import com.social100.todero.common.model.plugin.PluginInterface;
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
    final private Map<String, Plugin> plugins = new HashMap<>();

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
                PluginInterface pluginInstance = commandClass.getDeclaredConstructor().newInstance();
                Optional<Component> component = Optional.ofNullable(pluginInstance.getComponent());
                String componentName = "";
                String componentDescription = "";
                Map<String, Map<String, Command>> componentCommands = new HashMap<>();
                if (component.isPresent()) {
                    componentName = Optional.ofNullable(component.get().getName()).orElse("");
                    componentDescription = Optional.ofNullable(component.get().getDescription()).orElse("");
                    componentCommands = Optional.ofNullable(component.get().getCommands()).orElse(new HashMap<>());
                }

                Plugin plugin = Plugin.builder()
                        .file(pluginJar)
                        .classLoader(pluginClassLoader)
                        .pluginClass(commandClass)
                        .pluginInstance(pluginInstance)
                        .component(Component
                                .builder()
                                .name(componentName)
                                .description(componentDescription)
                                .commands(componentCommands)
                                .build())
                        .build();
                plugins.put(componentName, plugin);
            }
        }
    }

    public Map<String, Plugin> getPlugins() {
        return plugins;
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
        return true;
        //return pluginName.equals(Optional.ofNullable(p.getPluginInstance().name()).orElse(""))
        //        && Optional.ofNullable(p.getPluginInstance().hasCommand(command)).orElse(false);
    }
}