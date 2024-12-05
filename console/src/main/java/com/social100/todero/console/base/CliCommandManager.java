package com.social100.todero.console.base;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.social100.todero.common.Constants;
import com.social100.todero.common.config.AppConfig;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CliCommandManager implements CommandManager {
    final static private ObjectMapper objectMapper = new ObjectMapper();
    PluginManager pluginManager;
    private OutputType outputType = OutputType.JSON; // Default output type
    private final Map<String, String> properties = new HashMap<>(); // Stores properties

    public CliCommandManager(AppConfig appConfig) {
        String pluginDirectory = Optional.ofNullable(appConfig.getApp().getPlugins().getDir())
                .orElseThrow(() -> new IllegalArgumentException("Wrong value in plugin directory"));
        properties.put("output", outputType.name());
        pluginManager = new PluginManager(new File(pluginDirectory));
    }

    @Override
    public String getHelpMessage(String plugin, String command) {
        return pluginManager.getHelp(plugin, command, outputType);
    }

    @Override
    public String execute(String line) {
        if (line == null || line.isBlank()) {
            return ""; // Early exit if input is null or empty
        }

        // Regex pattern to extract arguments (handles quoted strings correctly)
        Pattern pattern = Pattern.compile("([^\"]\\S*|\".+?\")\\s*");
        Matcher matcher = pattern.matcher(line);

        ArrayList<String> arguments = new ArrayList<>();
        while (matcher.find()) {
            arguments.add(matcher.group(1).replace("\"", ""));
        }

        if (arguments.isEmpty()) {
            return ""; // No arguments found, nothing to execute
        }

        String command = arguments.remove(0);
        if ("set".equalsIgnoreCase(command)) {
            return handleSetCommand(arguments);
        }

        String pluginOrCommandName = command;
        String subCommand = arguments.isEmpty() ? null : arguments.remove(0);
        String[] commandArgs = arguments.toArray(new String[0]);

        // Reserved command logic
        switch (pluginOrCommandName) {
            case Constants.CLI_COMMAND_HELP:
                // Pass both the plugin name and sub-command (or null if missing)
                String pluginName = subCommand;
                String commandName = arguments.isEmpty() ? null : arguments.remove(0);
                return formatOutput(getHelpMessage(pluginName, commandName));
            case Constants.CLI_COMMAND_LOAD:
                return formatOutput(load());
            case Constants.CLI_COMMAND_UNLOAD:
                return formatOutput(unload());
            case Constants.CLI_COMMAND_RELOAD:
                return formatOutput(reload());
            default:
                // If it's not a reserved command, treat it as a plugin name
                Object output = pluginManager.execute(pluginOrCommandName, subCommand, commandArgs);
                return formatOutput(output);
        }
    }

    // Handle the `set` command to update properties dynamically
    private String handleSetCommand(ArrayList<String> arguments) {
        if (arguments.size() < 2) {
            return "Error: 'set' command requires a property and a value.";
        }

        String property = arguments.get(0).toLowerCase();
        String value = arguments.get(1);

        // Update the property in the map
        properties.put(property, value);

        // Special handling for specific properties
        if ("output".equals(property)) {
            if (!setOutputType(value)) {
                return "Error: Invalid output type. Valid options are: " +
                        Arrays.toString(OutputType.values());
            }
        }

        return "Property '" + property + "' set to '" + value + "'.";
    }

    // Helper method to set the output type
    private boolean setOutputType(String type) {
        try {
            this.outputType = OutputType.valueOf(type.trim().toUpperCase());
            properties.put("output", outputType.name()); // Sync property map with outputType
            return true;
        } catch (IllegalArgumentException e) {
            return false; // Invalid output type
        }
    }

    // Formats output according to the selected OutputType
    private String formatOutput(Object output) {
        if (output == null) {
            return null;
        }

        switch (outputType) {
            case JSON:
                return toJson(output);
            case YAML:
                return toYaml(output);
            case TEXT:
                return output.toString();
            case XML:
                return toXml(output);
            default:
                throw new IllegalStateException("Unexpected output type: " + outputType);
        }
    }

    // Placeholder methods for serialization
    private String toJson(Object obj) {
        return obj.toString();
    }

    private String toYaml(Object obj) {
        return obj.toString();
    }

    private String toXml(Object obj) {
        return obj.toString();
    }

    @Override
    public String reload() {
        pluginManager.reload();
        return "Ok";
    }

    @Override
    public String unload() {
        pluginManager.clear();
        return "Ok";
    }

    @Override
    public String load() {
        return "Not implemented yet";
    }

    @Override
    public void terminate() {
        pluginManager.clear();
    }

    @Override
    public String[] generateAutocompleteStrings() {
        return pluginManager.generateAutocompleteStrings().toArray(new String[0]);
    }
}