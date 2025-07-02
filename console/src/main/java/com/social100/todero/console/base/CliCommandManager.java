package com.social100.todero.console.base;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.social100.todero.common.Constants;
import com.social100.todero.common.channels.EventChannel;
import com.social100.todero.common.command.CommandContext;
import com.social100.todero.common.config.AppConfig;
import com.social100.todero.common.config.ServerType;
import com.social100.todero.common.message.MessageContainer;
import com.social100.todero.common.message.channel.ChannelType;
import com.social100.todero.common.message.channel.impl.PublicDataPayload;
import com.social100.todero.common.model.plugin.Component;
import com.social100.todero.console.workspace.Workspace;
import com.social100.todero.console.workspace.WorkspaceManager;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CliCommandManager implements CommandManager {
    final static private ObjectMapper objectMapper = new ObjectMapper();
    private final EventChannel.EventListener eventListener;
    PluginManager pluginManager;
    WorkspaceManager workspaceManager;
    private OutputType outputType = OutputType.JSON; // Default output type
    private final Map<String, String> properties = new HashMap<>(); // Stores properties

    public CliCommandManager(AppConfig appConfig, ServerType type, EventChannel.EventListener eventListener) {
        //String pluginDirectory = Optional.ofNullable(appConfig.getApp().getPlugins().getDir())
        //        .orElseThrow(() -> new IllegalArgumentException("Wrong value in plugin directory"));

        properties.put("output", outputType.name());

        workspaceManager = new WorkspaceManager();
        Workspace userWorkspace = workspaceManager.getWorkspace("guest");

        if (ServerType.AI.equals(type)) {
            pluginManager = new PluginManager(new File(userWorkspace.getBeingsDir().getAbsolutePath()), type, eventListener);
        } else if (ServerType.AIA.equals(type)) {
            pluginManager = new PluginManager(new File(userWorkspace.getPluginsDir().getAbsolutePath()), type, eventListener);
        } else {
            throw new IllegalArgumentException("Wrong Server Type");
        }

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
        final PublicDataPayload publicDataPayload = (PublicDataPayload)messageContainer.getMessages().get(ChannelType.PUBLIC_DATA);
        final String line = publicDataPayload.getMessage();
        if (line == null || line.isBlank()) {
            return true; // Early exit if input is null or empty
        }

        // Regex pattern to extract arguments:
        // - Quoted strings: "some phrase"
        // - Placeholders: ${some placeholder}
        // - Unquoted words: play, loud, etc.
        Pattern pattern = Pattern.compile("(\\$\\{[^}]+}|\"[^\"]+\"|\\S+)");
        Matcher matcher = pattern.matcher(line);

        ArrayList<String> arguments = new ArrayList<>();
        while (matcher.find()) {
            String arg = matcher.group(1);
            // Remove quotes (but keep placeholders as-is)
            if (arg.startsWith("\"") && arg.endsWith("\"")) {
                arg = arg.substring(1, arg.length() - 1);
            }
            arguments.add(arg);
        }

        if (arguments.isEmpty()) {
            return true; // No arguments found, nothing to execute
        }

        String pluginOrCommandName = arguments.remove(0);
        String subCommand = null;
        String[] commandArgs = null;

        // Build the command context at this stage 'commandArgs' is not relevant because it is null.
        CommandContext context = CommandContext.builder()
                .sourceId(messageContainer.getResponderId())
                //.args(commandArgs)
                .build();

        // Reserved command logic
        switch (pluginOrCommandName) {
            case Constants.CLI_COMMAND_COMPONENT:
                context.respond(toJsonComponent(getComponent()));
                /*
                this.eventListener.onEvent("command", MessageContainer.builder()
                        .responderId(messageContainer.getResponderId())
                        .addChannelMessage(ChannelMessageFactory.createChannelMessage(ChannelType.PUBLIC_DATA,
                                PublicDataPayload.builder()
                                        .message(toJsonComponent(getComponent()))
                                        .build()))
                        .build());
                 */
                return true;
            case Constants.CLI_COMMAND_HELP:
                // Pass both the plugin name and sub-command (or null if missing)
                subCommand = arguments.isEmpty() ? null : arguments.remove(0);
                String commandName = arguments.isEmpty() ? null : arguments.remove(0);
                context.respond(formatOutput(getHelpMessage(subCommand, commandName)));
                /*
                this.eventListener.onEvent("command", MessageContainer.builder()
                        .responderId(messageContainer.getResponderId())
                        .addChannelMessage(ChannelMessageFactory.createChannelMessage(ChannelType.PUBLIC_DATA,
                                PublicDataPayload.builder()
                                        .message(formatOutput(getHelpMessage(subCommand, commandName)))
                                        .build()))
                        .build());
                 */
                return true;
            case Constants.CLI_COMMAND_LOAD:
                context.respond(formatOutput(load()));
                /*
                this.eventListener.onEvent("command", MessageContainer.builder()
                        .responderId(messageContainer.getResponderId())
                        .addChannelMessage(ChannelMessageFactory.createChannelMessage(ChannelType.PUBLIC_DATA,
                                PublicDataPayload.builder()
                                        .message(formatOutput(load()))
                                        .build()))
                        .build());*/
                return true;
            case Constants.CLI_COMMAND_RELOAD:
                context.respond(formatOutput(reload()));
                /*
                this.eventListener.onEvent("command", MessageContainer.builder()
                        .responderId(messageContainer.getResponderId())
                        .addChannelMessage(ChannelMessageFactory.createChannelMessage(ChannelType.PUBLIC_DATA,
                                PublicDataPayload.builder()
                                        .message(formatOutput(reload()))
                                        .build()))
                        .build());
                 */
                return true;
            case Constants.CLI_COMMAND_SET:
                if (arguments.size() < 2) {
                    context.respond("Error: 'set' command requires a property and a value.");
                    /*
                    this.eventListener.onEvent("error", MessageContainer.builder()
                            .responderId(messageContainer.getResponderId())
                            .addChannelMessage(ChannelMessageFactory.createChannelMessage(ChannelType.PUBLIC_DATA,
                                    PublicDataPayload.builder()
                                            .message("Error: 'set' command requires a property and a value.")
                                            .build()))
                            .build());*/
                    return false;
                }
                String property = arguments.get(0).toLowerCase();
                String value = arguments.get(1);
                context.respond(handleSetCommand(property, value));
                /*
                this.eventListener.onEvent("command", MessageContainer.builder()
                        .responderId(messageContainer.getResponderId())
                        .addChannelMessage(ChannelMessageFactory.createChannelMessage(ChannelType.PUBLIC_DATA,
                                PublicDataPayload.builder()
                                        .message(handleSetCommand(property, value))
                                        .build()))
                        .build());*/
                return true;
            case Constants.CLI_COMMAND_UNLOAD:
                context.respond(formatOutput(unload()));
                /*
                this.eventListener.onEvent("command", MessageContainer.builder()
                        .responderId(messageContainer.getResponderId())
                        .addChannelMessage(ChannelMessageFactory.createChannelMessage(ChannelType.PUBLIC_DATA,
                                PublicDataPayload.builder()
                                        .message(formatOutput(unload()))
                                        .build()))
                        .build());*/
                return true;
            case Constants.CLI_COMMAND_WORKSPACE:
                context.respond(formatOutput(unload()));
                /*
                this.eventListener.onEvent("command", MessageContainer.builder()
                        .responderId(messageContainer.getResponderId())
                        .addChannelMessage(ChannelMessageFactory.createChannelMessage(ChannelType.PUBLIC_DATA,
                                PublicDataPayload.builder()
                                        .message(formatOutput(unload()))
                                        .build()))
                        .build());*/
                return true;
            default:
                // If it's not a reserved command, treat it as a plugin name
                subCommand = arguments.isEmpty() ? null : arguments.remove(0);
                commandArgs = arguments.toArray(new String[0]);
                // Rebuild Command Context to use commandArgs for commands
                context = CommandContext.builder()
                        .sourceId(messageContainer.getResponderId())
                        .args(commandArgs)
                        .pluginManager(pluginManager)
                        .build();
                pluginManager.execute(pluginOrCommandName, subCommand, context);
                //this.eventListener.onEvent("command",formatOutput(output));
                return true;
        }
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