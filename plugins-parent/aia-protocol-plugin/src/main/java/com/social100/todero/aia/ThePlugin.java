package com.social100.todero.aia;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.social100.todero.common.model.plugin.Component;
import com.social100.todero.common.model.plugin.PluginInterface;
import com.social100.todero.common.observer.Observer;
import com.social100.todero.common.observer.PublisherManager;
import com.social100.todero.console.base.ApiCommandLineInterface;

public class ThePlugin extends PublisherManager implements PluginInterface {
    final ApiCommandLineInterface apiCommandLineInterface;
    Component component;

    public ThePlugin(Observer observer) {
        this.addObserver(observer);
        this.apiCommandLineInterface = new ApiCommandLineInterface(null, true);
        this.apiCommandLineInterface.setOutputDataHandler(this::outputDataHandler);
        this.apiCommandLineInterface.writeAsync("component\n".getBytes());
    }

    public void outputDataHandler(byte[] data) {
        String json = new String(data);
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            component = objectMapper.readValue(json, Component.class);

            this.publish(String.valueOf(component));
        } catch (Exception e) {
            System.out.println(e.getMessage());
            this.publish(json);
        }
    }

    @Override
    public Component getComponent() {
        /*Component.builder()
                .name(componentName)
                .description(componentDescription)
                .commands()*/
        return null;
        // Build the component with the available/supported commands according to the help returned and local policies.
    }

    @Override
    public Object execute(String pluginName, String command, String[] commandArgs) {
        // Validate input parameters
        if (pluginName == null || pluginName.isEmpty()) {
            throw new IllegalArgumentException("Plugin name cannot be null or empty");
        }
        if (command == null || command.isEmpty()) {
            throw new IllegalArgumentException("Command cannot be null or empty");
        }

        // Build the command string
        StringBuilder sb = new StringBuilder();
        sb.append(pluginName).append(" ").append(command);

        if (commandArgs != null) {
            for (String arg : commandArgs) {
                sb.append(" ").append(arg);
            }
        }

        String fullCommand = sb.toString();

        try {
            // Send the command via the API
            this.apiCommandLineInterface.writeAsync(fullCommand.getBytes());
            // Optionally return a success indicator
            return null;
        } catch (Exception ignore) {
            return null;
        }
    }
}
