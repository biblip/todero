package com.social100.todero.cli.base;

import com.social100.todero.common.PluginInterface;
import org.reflections.Reflections;
import org.reflections.scanners.SubTypesScanner;
import org.reflections.scanners.TypeAnnotationsScanner;
import org.reflections.util.ConfigurationBuilder;

import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

public class PluginContext {
    private URLClassLoader pluginClassLoader;
    private PluginInterface registeredPlugin;
    final private PluginExecutor pluginExecutor = new PluginExecutor(3);


    public PluginContext(File pluginJar) throws Exception {
        initializePlugin(pluginJar);
    }

    private void initializePlugin(File pluginJar) throws Exception {
        // Convert the File to a URL
        URL jarUrl = pluginJar.toURI().toURL();
        this.pluginClassLoader = new URLClassLoader(new URL[]{jarUrl}, getClass().getClassLoader());

        // Initialize Reflections with the URLClassLoader
        Reflections reflections = new Reflections(new ConfigurationBuilder()
                .addUrls(jarUrl)
                .addClassLoader(pluginClassLoader)
                .setScanners(new SubTypesScanner(), new TypeAnnotationsScanner()));

        // Discover and instantiate Command implementations
        Set<Class<? extends PluginInterface>> commandClasses = reflections.getSubTypesOf(PluginInterface.class);
        for (Class<? extends PluginInterface> commandClass : commandClasses) {
            if (!commandClass.isInterface()) {
                registeredPlugin = commandClass.getDeclaredConstructor().newInstance();
            }
        }
    }

    public String getHelpMessage() {
        StringBuilder helpMessage = new StringBuilder();

        helpMessage.append(String.format("%-15s   - %s\n", registeredPlugin.name().toUpperCase(), registeredPlugin.description().toUpperCase()));
        helpMessage.append(registeredPlugin.getHelpMessage());

        return helpMessage.toString().trim(); // Trim to remove the last newline character
    }

    // Cleanup method to properly close the plugin class loader when no longer needed
    public void cleanup() {
        try {
            pluginClassLoader.close();
            pluginExecutor.shutdown();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public Boolean hasName(String plugin ) {
        return registeredPlugin.name().equals(plugin);
    }

    public Boolean hasCommand(String command) {
        return registeredPlugin.hasCommand(command);
    }

    public String execute(String command, String[] commandArgs) {
        if (registeredPlugin.hasCommand(command)) {
            Future<String> future = pluginExecutor.executePluginTask(new PluginCommandTask(registeredPlugin, command, commandArgs));
            try {
                return future.get();
            } catch (InterruptedException | ExecutionException e) {
                return "Error: " + e.getMessage();
            }
        }
        return "Command Not Found";
    }

    public String[] getAllCommandNames() {
        return registeredPlugin.getAllCommandNames();
    }
}