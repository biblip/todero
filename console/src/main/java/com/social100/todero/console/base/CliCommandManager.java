package com.social100.todero.console.base;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.social100.todero.common.Constants;
import com.social100.todero.common.channels.EventChannel;
import com.social100.todero.common.command.CommandContext;
import com.social100.todero.common.config.AppConfig;
import com.social100.todero.common.message.MessageContainer;
import com.social100.todero.common.message.channel.ChannelMessageFactory;
import com.social100.todero.common.message.channel.ChannelType;
import com.social100.todero.common.message.channel.impl.PublicDataPayload;
import com.social100.todero.common.model.plugin.Component;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CliCommandManager implements CommandManager {
    final static private ObjectMapper objectMapper = new ObjectMapper();
    private final EventChannel.EventListener eventListener;
    PluginManager pluginManager;
    private OutputType outputType = OutputType.JSON; // Default output type
    private final Map<String, String> properties = new HashMap<>(); // Stores properties

    public CliCommandManager(AppConfig appConfig, EventChannel.EventListener eventListener) {
        String pluginDirectory = Optional.ofNullable(appConfig.getApp().getPlugins().getDir())
                .orElseThrow(() -> new IllegalArgumentException("Wrong value in plugin directory"));
        properties.put("output", outputType.name());
        pluginManager = new PluginManager(new File(pluginDirectory), eventListener);
        this.eventListener = eventListener;
    }

    @Override
    public String getHelpMessage(String plugin, String command) {
        return pluginManager.getHelp(plugin, command, outputType);
    }

    public Component getComponent() {
        return pluginManager.getComponent();
    }

    @Override
    public boolean process(final MessageContainer messageContainer) {
        // TODO: procesa public_data, pero envia a ejecucion todo el MessageContainer o un derivado de el.
        //       por ahora se esta enviando, el contenido del canal public_data parseado.
        final PublicDataPayload publicDataPayload = (PublicDataPayload)messageContainer.getMessages().get(ChannelType.PUBLIC_DATA);
        final String line = publicDataPayload.getMessage();
        if (line == null || line.isBlank()) {
            return true; // Early exit if input is null or empty
        }

        // Regex pattern to extract arguments (handles quoted strings correctly)
        Pattern pattern = Pattern.compile("([^\"]\\S*|\".+?\")\\s*");
        Matcher matcher = pattern.matcher(line);

        ArrayList<String> arguments = new ArrayList<>();
        while (matcher.find()) {
            arguments.add(matcher.group(1).replace("\"", ""));
        }

        if (arguments.isEmpty()) {
            return true; // No arguments found, nothing to execute
        }

        String pluginOrCommandName = arguments.remove(0);
        String subCommand = null;
        String[] commandArgs = null;

        // Reserved command logic
        switch (pluginOrCommandName) {
            case Constants.CLI_COMMAND_COMPONENT:
                this.eventListener.onEvent("command", MessageContainer.builder()
                        .addChannelMessage(ChannelMessageFactory.createChannelMessage(ChannelType.PUBLIC_DATA,
                                PublicDataPayload.builder()
                                        .message(toJsonComponent(getComponent()))
                                        .build()))
                        .build());
                return true;
            case Constants.CLI_COMMAND_HELP:
                // Pass both the plugin name and sub-command (or null if missing)
                subCommand = arguments.isEmpty() ? null : arguments.remove(0);
                String commandName = arguments.isEmpty() ? null : arguments.remove(0);
                this.eventListener.onEvent("command", MessageContainer.builder()
                        .addChannelMessage(ChannelMessageFactory.createChannelMessage(ChannelType.PUBLIC_DATA,
                                PublicDataPayload.builder()
                                        .message(formatOutput(getHelpMessage(subCommand, commandName)))
                                        .build()))
                        .build());
                return true;
            case Constants.CLI_COMMAND_LOAD:
                this.eventListener.onEvent("command", MessageContainer.builder()
                        .addChannelMessage(ChannelMessageFactory.createChannelMessage(ChannelType.PUBLIC_DATA,
                                PublicDataPayload.builder()
                                        .message(formatOutput(load()))
                                        .build()))
                        .build());
                return true;
            case Constants.CLI_COMMAND_UNLOAD:
                this.eventListener.onEvent("command", MessageContainer.builder()
                        .addChannelMessage(ChannelMessageFactory.createChannelMessage(ChannelType.PUBLIC_DATA,
                                PublicDataPayload.builder()
                                        .message(formatOutput(unload()))
                                        .build()))
                        .build());
                return true;
            case Constants.CLI_COMMAND_RELOAD:
                this.eventListener.onEvent("command", MessageContainer.builder()
                        .addChannelMessage(ChannelMessageFactory.createChannelMessage(ChannelType.PUBLIC_DATA,
                                PublicDataPayload.builder()
                                        .message(formatOutput(reload()))
                                        .build()))
                        .build());
                return true;
            case Constants.CLI_COMMAND_SET:
                if (arguments.size() < 2) {
                    this.eventListener.onEvent("error", MessageContainer.builder()
                            .addChannelMessage(ChannelMessageFactory.createChannelMessage(ChannelType.PUBLIC_DATA,
                                    PublicDataPayload.builder()
                                            .message("Error: 'set' command requires a property and a value.")
                                            .build()))
                            .build());
                    return false;
                }
                String property = arguments.get(0).toLowerCase();
                String value = arguments.get(1);
                this.eventListener.onEvent("command", MessageContainer.builder()
                        .addChannelMessage(ChannelMessageFactory.createChannelMessage(ChannelType.PUBLIC_DATA,
                                PublicDataPayload.builder()
                                        .message(handleSetCommand(property, value))
                                        .build()))
                        .build());
                return true;
            default:
                // If it's not a reserved command, treat it as a plugin name
                subCommand = arguments.isEmpty() ? null : arguments.remove(0);
                commandArgs = arguments.toArray(new String[0]);
                pluginManager.execute(pluginOrCommandName, subCommand, CommandContext.builder()
                        .sourceId(messageContainer.getResponderId())
                        .args(commandArgs)
                        .build());
                //this.eventListener.onEvent("command",formatOutput(output));
                return true;
        }
    }

    @Override
    public boolean process(MessageContainer messageContainer, Consumer<String> consumer) {
        process(messageContainer);
        return true;
    }

    // Handle the `set` command to update properties dynamically
    private String handleSetCommand(final String property, final String value) {
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

    private String toJsonComponent(Component obj) {
        ObjectMapper objectMapper = new ObjectMapper();
        String jsonString = null;
        try {
            System.out.println("serianlizing Component: " + obj.toString());
            jsonString = objectMapper.writeValueAsString(obj);
            System.out.println("jsonString: " + jsonString);
        } catch (JsonProcessingException ignore) {

        }
        return jsonString;
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