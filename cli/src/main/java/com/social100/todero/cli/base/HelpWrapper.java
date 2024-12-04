package com.social100.todero.cli.base;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import com.social100.todero.common.model.plugin.Command;
import com.social100.todero.common.model.plugin.Plugin;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class HelpWrapper {
    private Map<String, Plugin> plugins = new HashMap<>();

    public HelpWrapper(Map<String, Plugin> plugins) {
        this.plugins = plugins;
    }

    public String getHelp(String pluginName, String commandName, OutputType outputType) {
        Map<String, Object> helpData = new HashMap<>();

        // If pluginName is provided, filter for that specific plugin
        if (pluginName != null) {
            Plugin plugin = plugins.get(pluginName);
            if (plugin != null) {
                Map<String, Command> commands = plugin.getComponent().getCommands();

                // If commandName is provided, filter for that specific command
                if (commandName != null) {
                    Command command = commands.get(commandName);
                    if (command != null) {
                        helpData.put(pluginName, List.of(collectCommandInfo(command)));
                    }
                } else {
                    // Include all commands for the specified plugin
                    helpData.put(pluginName, collectCommandInfoList(commands));
                }
            }
        } else if (commandName != null) {
            // Search all plugins for the specified command
            for (Map.Entry<String, Plugin> entry : plugins.entrySet()) {
                String currentPluginName = entry.getKey();
                Plugin plugin = entry.getValue();
                Map<String, Command> commands = plugin.getComponent().getCommands();

                if (commands.containsKey(commandName)) {
                    helpData.put(currentPluginName, List.of(collectCommandInfo(commands.get(commandName))));
                }
            }
        } else {
            // If neither pluginName nor commandName is specified, return all help data
            for (Map.Entry<String, Plugin> entry : plugins.entrySet()) {
                helpData.put(entry.getKey(), collectCommandInfoList(entry.getValue().getComponent().getCommands()));
            }
        }

        // Format and return the output
        return formatHelpOutput(helpData, outputType);
    }

    private String formatHelpOutput(Map<String, Object> helpData, OutputType outputType) {
        try {
            switch (outputType) {
                case JSON:
                    return new ObjectMapper().writerWithDefaultPrettyPrinter().writeValueAsString(helpData);
                case YAML:
                    return new YAMLMapper().writeValueAsString(helpData);
                case TEXT:
                    return formatAsText(helpData);
                case XML:
                    return formatAsXml(helpData);
                default:
                    throw new IllegalArgumentException("Unsupported output type: " + outputType);
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate help output", e);
        }
    }

    private String formatAsText(Map<String, Object> helpData) {
        StringBuilder textBuilder = new StringBuilder();
        for (Map.Entry<String, Object> entry : helpData.entrySet()) {
            textBuilder.append("Plugin: ").append(entry.getKey()).append("\n");
            List<Map<String, String>> commands = (List<Map<String, String>>) entry.getValue();
            for (Map<String, String> commandInfo : commands) {
                textBuilder.append("  Command: ").append(commandInfo.get("command")).append("\n")
                        .append("    Description: ").append(commandInfo.get("description")).append("\n")
                        .append("    Group: ").append(commandInfo.get("group")).append("\n");
            }
            textBuilder.append("\n");
        }
        return textBuilder.toString();
    }

    private String formatAsXml(Map<String, Object> helpData) {
        StringBuilder xmlBuilder = new StringBuilder();
        xmlBuilder.append("<help>\n");
        for (Map.Entry<String, Object> entry : helpData.entrySet()) {
            xmlBuilder.append("  <plugin name=\"").append(entry.getKey()).append("\">\n");
            List<Map<String, String>> commands = (List<Map<String, String>>) entry.getValue();
            for (Map<String, String> commandInfo : commands) {
                xmlBuilder.append("    <command>\n")
                        .append("      <name>").append(commandInfo.get("command")).append("</name>\n")
                        .append("      <description>").append(commandInfo.get("description")).append("</description>\n")
                        .append("      <group>").append(commandInfo.get("group")).append("</group>\n")
                        .append("    </command>\n");
            }
            xmlBuilder.append("  </plugin>\n");
        }
        xmlBuilder.append("</help>");
        return xmlBuilder.toString();
    }

    private Map<String, String> collectCommandInfo(Command command) {
        Map<String, String> commandInfo = new HashMap<>();
        commandInfo.put("command", command.getCommand());
        commandInfo.put("description", command.getDescription());
        commandInfo.put("group", command.getGroup());
        return commandInfo;
    }

    private List<Map<String, String>> collectCommandInfoList(Map<String, Command> commands) {
        List<Map<String, String>> commandInfoList = new ArrayList<>();
        for (Command command : commands.values()) {
            commandInfoList.add(collectCommandInfo(command));
        }
        return commandInfoList;
    }
}
