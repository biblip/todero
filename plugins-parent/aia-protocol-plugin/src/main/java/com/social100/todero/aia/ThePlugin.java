package com.social100.todero.aia;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.social100.todero.common.model.plugin.Component;
import com.social100.todero.common.model.plugin.PluginInterface;
import com.social100.todero.console.base.ApiCommandLineInterface;

public class ThePlugin implements PluginInterface {
    final ApiCommandLineInterface apiCommandLineInterface;
    Component component;

    public ThePlugin() {
        this.apiCommandLineInterface = new ApiCommandLineInterface(null, true);
        this.apiCommandLineInterface.setOutputDataHandler(this::outputDataHandler);
        this.apiCommandLineInterface.writeAsync("component\n".getBytes());
    }

    public void outputDataHandler(byte[] data) {
        String json = new String(data);
        System.out.println("Arriving Data:");
        System.out.println(json);
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            component = objectMapper.readValue(json, Component.class);
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
        System.out.println("deserializado");
        System.out.println(component);
        synchronized (apiCommandLineInterface) {
            apiCommandLineInterface.notifyAll();
        }
    }

    @Override
    public Component getComponent() {
        synchronized (apiCommandLineInterface) {
            try {
                apiCommandLineInterface.wait(2000);
            } catch (InterruptedException ignore) {
            }
        }
        /*Component.builder()
                .name(componentName)
                .description(componentDescription)
                .commands()*/
        return component;
        // Build the component with the available/supported commands according to the help returned and local policies.
    }

    @Override
    public Object execute(String command, String[] commandArgs) {
        // re-arm the line to be sent
        this.apiCommandLineInterface.writeAsync(command.getBytes());
        return null;
    }
}
