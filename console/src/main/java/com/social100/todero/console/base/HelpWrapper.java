package com.social100.todero.console.base;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import com.social100.todero.common.model.plugin.Command;
import com.social100.todero.common.model.plugin.Plugin;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class HelpWrapper {
    private Map<String, Plugin> plugins = new HashMap<>();

    public HelpWrapper(Map<String, Plugin> plugins) {
        this.plugins = plugins;
    }

    public String getHelp(String pluginName, String commandName, OutputType outputType) {
        Map<String, Object> helpData = new LinkedHashMap<>();

        // If pluginName is provided, filter for that specific plugin
        if (pluginName != null) {
            Plugin plugin = plugins.get(pluginName);
            if (plugin != null) {
                Map<String, Map<String, Command>> commandsByGroup = plugin.getComponent().getCommands();
                Map<String, Object> pluginHelp = buildPluginHelp(commandsByGroup, commandName);
                if (!pluginHelp.isEmpty()) {
                    helpData.put(pluginName, pluginHelp);
                }
            }
        } else if (commandName != null) {
            // Search all plugins for the specified command
            for (Map.Entry<String, Plugin> entry : plugins.entrySet()) {
                String currentPluginName = entry.getKey();
                Plugin plugin = entry.getValue();
                Map<String, Map<String, Command>> commandsByGroup = plugin.getComponent().getCommands();

                Command command = findCommandByName(commandsByGroup, commandName);
                if (command != null) {
                    Map<String, Object> pluginHelp = buildPluginHelp(commandsByGroup, commandName);
                    if (!pluginHelp.isEmpty()) {
                        helpData.put(currentPluginName, pluginHelp);
                    }
                }
            }
        } else {
            // If neither pluginName nor commandName is specified, return all help data
            for (Map.Entry<String, Plugin> entry : plugins.entrySet()) {
                String currentPluginName = entry.getKey();
                Plugin plugin = entry.getValue();
                Map<String, Map<String, Command>> commandsByGroup = plugin.getComponent().getCommands();

                Map<String, Object> pluginHelp = buildPluginHelp(commandsByGroup, null);
                if (!pluginHelp.isEmpty()) {
                    helpData.put(currentPluginName, pluginHelp);
                }
            }
        }

        // Format and return the output
        return formatHelpOutput(helpData, outputType);
    }

    // Helper method to build plugin help data for hierarchical output
    private Map<String, Object> buildPluginHelp(Map<String, Map<String, Command>> commandsByGroup, String filterCommandName) {
        Map<String, Object> pluginHelp = new LinkedHashMap<>();

        for (Map.Entry<String, Map<String, Command>> groupEntry : commandsByGroup.entrySet()) {
            String groupName = groupEntry.getKey();
            Map<String, Command> groupCommands = groupEntry.getValue();

            // Filter and collect command info
            List<Map<String, String>> commandInfoList = groupCommands.values().stream()
                    .filter(command -> filterCommandName == null || command.getCommand().equals(filterCommandName))
                    .map(this::collectCommandInfo)
                    .collect(Collectors.toList());

            if (!commandInfoList.isEmpty()) {
                pluginHelp.put(groupName, commandInfoList);
            }
        }

        return pluginHelp;
    }

    // Helper method to collect command info
    private Map<String, String> collectCommandInfo(Command command) {
        Map<String, String> commandInfo = new LinkedHashMap<>();
        commandInfo.put("command", command.getCommand());
        commandInfo.put("description", command.getDescription());
        return commandInfo;
    }

    // Helper method to find a specific command by name
    private Command findCommandByName(Map<String, Map<String, Command>> commandsByGroup, String commandName) {
        for (Map<String, Command> groupCommands : commandsByGroup.values()) {
            if (groupCommands.containsKey(commandName)) {
                return groupCommands.get(commandName);
            }
        }
        return null;
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

        for (Map.Entry<String, Object> pluginEntry : helpData.entrySet()) {
            String pluginName = pluginEntry.getKey();
            Map<String, Object> groups = (Map<String, Object>) pluginEntry.getValue();

            // Add plugin header
            textBuilder.append("- Plugin: ").append(pluginName).append(":\n");

            for (Map.Entry<String, Object> groupEntry : groups.entrySet()) {
                String groupName = groupEntry.getKey();
                List<Map<String, String>> commands = (List<Map<String, String>>) groupEntry.getValue();

                // Add group header
                textBuilder.append("    - Group: ").append(groupName).append("\n");

                for (Map<String, String> commandInfo : commands) {
                    // Add command details
                    textBuilder.append("        - Command: ").append(commandInfo.get("command")).append("\n");
                    textBuilder.append("            - Description: ").append(commandInfo.get("description")).append("\n");
                }
            }
        }

        // Remove any trailing newlines
        while (textBuilder.length() > 0 && textBuilder.charAt(textBuilder.length() - 1) == '\n') {
            textBuilder.setLength(textBuilder.length() - 1);
        }

        return textBuilder.toString();
    }

    private String formatAsXml(Map<String, Object> helpData) {
        StringBuilder xmlBuilder = new StringBuilder();
        xmlBuilder.append("<help>\n");

        for (Map.Entry<String, Object> pluginEntry : helpData.entrySet()) {
            String pluginName = pluginEntry.getKey();
            Map<String, Object> groups = (Map<String, Object>) pluginEntry.getValue();

            xmlBuilder.append("  <plugin name=\"").append(pluginName).append("\">\n");

            for (Map.Entry<String, Object> groupEntry : groups.entrySet()) {
                String groupName = groupEntry.getKey();
                List<Map<String, String>> commands = (List<Map<String, String>>) groupEntry.getValue();

                xmlBuilder.append("    <group name=\"").append(groupName).append("\">\n");

                for (Map<String, String> commandInfo : commands) {
                    xmlBuilder.append("      <command>\n")
                            .append("        <name>").append(commandInfo.get("command")).append("</name>\n")
                            .append("        <description>").append(commandInfo.get("description")).append("</description>\n")
                            .append("      </command>\n");
                }

                xmlBuilder.append("    </group>\n");
            }

            xmlBuilder.append("  </plugin>\n");
        }

        xmlBuilder.append("</help>");
        return xmlBuilder.toString();
    }
}
