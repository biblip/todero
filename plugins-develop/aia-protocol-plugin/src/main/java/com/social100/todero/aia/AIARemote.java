package com.social100.todero.aia;

import com.social100.processor.AIAController;
import com.social100.processor.Action;
import com.social100.todero.aia.service.ApiAIAProtocolService;
import com.social100.todero.common.channels.EventChannel;
import com.social100.todero.common.channels.process.ChannelHandler;
import com.social100.todero.common.channels.process.ChannelProcessor;
import com.social100.todero.common.command.CommandContext;
import com.social100.todero.common.config.ServerType;
import com.social100.todero.common.message.MessageContainer;
import com.social100.todero.common.message.channel.ChannelType;
import com.social100.todero.common.message.channel.impl.ControlPayload;
import com.social100.todero.common.message.channel.impl.EventPayload;
import com.social100.todero.common.message.channel.impl.HiddenDataPayload;
import com.social100.todero.common.message.channel.impl.NotificationPayload;
import com.social100.todero.common.message.channel.impl.PublicDataPayload;
import com.social100.todero.net.UrlParser;
import com.social100.todero.processor.EventDefinition;
import com.social100.todero.util.ArgumentParser;

import java.net.InetSocketAddress;
import java.util.Arrays;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@AIAController(name = "com.shellaia.verbatim.plugin.aia",
    type = ServerType.AIA,
    visible = true,
    description = "AIA Protocol Plugin",
    events = AIARemote.AIAProtocolEvents.class)
public class AIARemote {
    Map<String, ApiAIAProtocolService> stringApiAIAProtocolServiceMap = new ConcurrentHashMap<>();
    final CommandContext[] context = {null};

    ChannelProcessor processor = ChannelProcessor.builder()
            .registerHandler(ChannelType.PUBLIC_DATA, new PublicDataHandler())
            .registerHandler(ChannelType.HIDDEN_DATA, new HiddenDataHandler())
            .registerHandler(ChannelType.NOTIFICATION, new NotificationHandler())
            .registerHandler(ChannelType.EVENT, new EventHandler())
            .registerHandler(ChannelType.CONTROL, new ControlHandler())
            .build();

    public AIARemote() {
    }

    public enum AIAProtocolEvents implements EventDefinition  {
        DOOR_OPEN("The door_open description"),
        WINDOW_OPEN("The window_broken description"),
        HIGH_TEMP("The high_temp description");

        private final String description;

        AIAProtocolEvents(String description) {
            this.description = description;
        }

        @Override
        public String getDescription() {
            return description;
        }
    }

    @Action(group = ApiAIAProtocolService.MAIN_GROUP,
            command = "list",
            description = "List all registrations")
    public Boolean listCommand(CommandContext context) {
        context.respond("[" + stringApiAIAProtocolServiceMap.entrySet().stream()
                .map(entry -> entry.getKey() + " : " + entry.getValue().getServer() + " -> " + entry.getValue().getStatus())
                .collect(Collectors.joining("\n")) + "]");
        return true;
    }

    @Action(group = ApiAIAProtocolService.MAIN_GROUP,
            command = "register",
            description = "Register a aia server   register --url <url> --name <name>")
    public Boolean registerCommand(CommandContext context) {
        this.context[0] = context;
        ArgumentParser parser = new ArgumentParser();

        // Define rules with default values
        parser.addRule("url", value -> value.contains(":") || value.startsWith("aia://"), "localhost:9876");
        parser.addRule("name", value -> value != null && !value.trim().isEmpty(), "defaultName");

        if (parser.parse(context.getArgs())) {
            String url = parser.getArgument("url");
            String name = parser.getArgument("name").toLowerCase(Locale.ROOT);

            if (stringApiAIAProtocolServiceMap.containsKey(name)) {
                context.respond("name already used ... '" + url + "'   name: " + name);
                return false;
            }

            context.respond("Connecting ... '" + url + "'   called: " + name);

            UrlParser.HostInfo hostInfo = UrlParser.parse(url);

            InetSocketAddress serverAddress = new InetSocketAddress(hostInfo.getHost(), hostInfo.getPort());

            EventChannel.EventListener eventListener = new EventChannel.EventListener() {
                @Override
                public void onEvent(String eventName, MessageContainer message) {
                    processor.processAllMessages(message.getMessages());
                    //String payload = ((PublicDataPayload)message.getMessages().get(ChannelType.PUBLIC_DATA)).getMessage();
                    //context.respond(payload);
                }
            };

            ApiAIAProtocolService service = new ApiAIAProtocolService(serverAddress, eventListener);

            stringApiAIAProtocolServiceMap.put(name, service);

            context.respond("Registration active with '" + hostInfo.getHost() + " : " + hostInfo.getPort() + "'");
        } else {
            context.respond(parser.errorMessage());
        }
        return true;
    }

    @Action(group = ApiAIAProtocolService.MAIN_GROUP,
            command = "unregister",
            description = "De register the aia server   unregister --name <name>")
    public Boolean unregisterCommand(CommandContext context) {
        this.context[0] = context;
        ArgumentParser parser = new ArgumentParser();

        // Define rules with default values
        parser.addRule("name", value -> value != null && !value.trim().isEmpty(), "defaultName");

        if (parser.parse(context.getArgs())) {
            String name = parser.getArgument("name").toLowerCase(Locale.ROOT);

            context.respond("Unregister ... '" + name + "'");

            ApiAIAProtocolService service = stringApiAIAProtocolServiceMap.remove(name);

            service.unregister();

            context.respond("server is no longer active name : '" + name + "'");
        } else {
            context.respond(parser.errorMessage());
        }
        return true;
    }

    @Action(group = ApiAIAProtocolService.MAIN_GROUP,
            command = "exec",
            description = "Execute a command into the remote console  exec --name <name> command...")
    public Boolean execCommand(CommandContext context) {
        this.context[0] = context;
        ArgumentParser parser = new ArgumentParser();

        // Define rules with default values
        parser.addRule("name", value -> value != null && !value.trim().isEmpty(), "defaultName");

        if (context.getArgs().length < 2) {
            context.respond("Not enough arguments  exec --name <name> command...");
        }

        String[] subArray = Arrays.copyOfRange(context.getArgs(), 0, 2);
        String[] rest = Arrays.copyOfRange(context.getArgs(), 2, context.getArgs().length);

        if (parser.parse(subArray)) {
            String name = parser.getArgument("name").toLowerCase(Locale.ROOT);

            Set<String> stopWords = Set.of("exec", "aia", "register");

            // Initialize a variable to store the offended word
            final String[] offendingWord = {null};

            // Convert the array to a Stream, process it, and join the result
            String line = Arrays.stream(rest)
                    .takeWhile(arg -> {
                        if (stopWords.contains(arg)) {
                            offendingWord[0] = arg; // Log the offended word
                            return false;         // Stop processing
                        }
                        return true;
                    })
                    .collect(Collectors.joining(" "));

            if (offendingWord[0] != null) {
                String warningMessage = "Offending word " + offendingWord[0] + " in '" +  String.join(" ", rest) + "'";
                System.out.println(warningMessage);
                context.respond(warningMessage);
                return true;
            }

            ApiAIAProtocolService service = stringApiAIAProtocolServiceMap.get(name);

            if (service != null) {
                service.exec(line);
                return true;
            }
            context.respond("name not found:  name='" + name + "'");
            return false;
        } else {
            context.respond(parser.errorMessage());
        }
        return true;
    }

    @Action(group = ApiAIAProtocolService.RESERVED_GROUP,
            command = "subscribe",
            description = "Subscribe to an event in this component")
    public Boolean subscribeToEvent(CommandContext context) {
        this.context[0] = context;
        context.respond("Done");
        return true;
    }

    @Action(group = ApiAIAProtocolService.RESERVED_GROUP,
            command = "unsubscribe",
            description = "Unubscribe from an event in this component")
    public Boolean unsubscribeFromEvent(CommandContext context) {
        this.context[0] = context;
        context.respond("Done");
        return true;
    }

    /**
     * Channel Processor Classes
     */
    private class PublicDataHandler implements ChannelHandler<PublicDataPayload> {
        @Override
        public void process(PublicDataPayload payload) {
            context[0].respond(payload.getMessage());
            //System.out.println(payload.getMessage());
        }

        @Override
        public Class<PublicDataPayload> getPayloadType() {
            return PublicDataPayload.class;
        }
    }

    private class HiddenDataHandler implements ChannelHandler<HiddenDataPayload> {
        @Override
        public void process(HiddenDataPayload payload) {
            context[0].respond(payload.getMessage());
            //System.out.println("Processing Hidden Data: " + payload.getMessage());
        }

        @Override
        public Class<HiddenDataPayload> getPayloadType() {
            return HiddenDataPayload.class;
        }
    }

    private class NotificationHandler implements ChannelHandler<NotificationPayload> {
        @Override
        public void process(NotificationPayload payload) {
            context[0].respond(payload.getMessage());
            //System.out.println("Processing Notification: " + payload.getMessage());
        }

        @Override
        public Class<NotificationPayload> getPayloadType() {
            return NotificationPayload.class;
        }
    }

    private class EventHandler implements ChannelHandler<EventPayload> {
        @Override
        public void process(EventPayload payload) {
            context[0].respond(payload.getMessage());
            //System.out.println("Processing Event: " + payload.getName() + " : " + payload.getMessage());
        }

        @Override
        public Class<EventPayload> getPayloadType() {
            return EventPayload.class;
        }
    }

    private class ControlHandler implements ChannelHandler<ControlPayload> {
        @Override
        public void process(ControlPayload payload) {
            context[0].respond(payload.getMessage());
            //System.out.println("Processing Control: " + payload.getMessage());
        }

        @Override
        public Class<ControlPayload> getPayloadType() {
            return ControlPayload.class;
        }
    }
}
