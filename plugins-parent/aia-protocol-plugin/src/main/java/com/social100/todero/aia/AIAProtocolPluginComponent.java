package com.social100.todero.aia;

import com.social100.processor.AIAController;
import com.social100.processor.Action;
import com.social100.todero.aia.service.ApiAIAProtocolService;
import com.social100.todero.common.channels.EventChannel;
import com.social100.todero.common.command.CommandContext;
import com.social100.todero.common.message.MessageContainer;
import com.social100.todero.common.message.channel.ChannelType;
import com.social100.todero.common.message.channel.impl.PublicDataPayload;
import com.social100.todero.net.UrlParser;
import com.social100.todero.processor.EventDefinition;

import java.net.InetSocketAddress;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;


@AIAController(name = "aia",
        type = "",
        description = "AIA Protocol Plugin",
        events = AIAProtocolPluginComponent.AIAProtocolEvents.class)
public class AIAProtocolPluginComponent {
    ApiAIAProtocolService apiAIAProtocolService = new ApiAIAProtocolService();
    CommandContext context;

    EventChannel.EventListener eventListener = new EventChannel.EventListener() {
        @Override
        public void onEvent(String eventName, MessageContainer message) {
            String payload = ((PublicDataPayload)message.getMessages().get(ChannelType.PUBLIC_DATA)).getMessage();
            context.respond(payload);
        }
    };

    public AIAProtocolPluginComponent() {
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
            command = "initiate",
            description = "Initiate a relationship with an aia server")
    public Boolean initiateCommand(CommandContext context) {
        this.context = context;
        String[] commandArgs = context.getArgs();
        if (commandArgs.length > 0) {

            context.respond("Connecting ... '" + commandArgs[0] + "'");

            UrlParser.HostInfo hostInfo = UrlParser.parse(commandArgs[0]);

            InetSocketAddress serverAddress = new InetSocketAddress(hostInfo.getHost(), hostInfo.getPort());

            apiAIAProtocolService.initiate(serverAddress, eventListener);

            context.respond("Relationship now active with '" + hostInfo.getHost() + " : " + hostInfo.getPort() + "'");
        } else {
            context.respond("host missing");
        }

        return true;
    }

    @Action(group = ApiAIAProtocolService.MAIN_GROUP,
            command = "conclude",
            description = "Conclude the relationship with the aia server")
    public Boolean concludeCommand(CommandContext context) {
        this.context = context;

        apiAIAProtocolService.conclude();

        context.respond("Relationship is no longer active");

        return true;
    }

    @Action(group = ApiAIAProtocolService.MAIN_GROUP,
            command = "exec",
            description = "Execute a command into the remote console")
    public Boolean execCommand(CommandContext context) {
        this.context = context;

        Set<String> stopWords = Set.of("exec", "aia", "initiate");

        // Initialize a variable to store the offended word
        final String[] offendingWord = {null};

        // Convert the array to a Stream, process it, and join the result
        String line = Arrays.stream(context.getArgs())
                .takeWhile(arg -> {
                    if (stopWords.contains(arg)) {
                        offendingWord[0] = arg; // Log the offended word
                        return false;         // Stop processing
                    }
                    return true;
                })
                .collect(Collectors.joining(" "));

        if (offendingWord[0] != null) {
            String warningMessage = "Offending word " + offendingWord[0] + " in '" +  String.join(" ", context.getArgs()) + "'";
            System.out.println(warningMessage);
            context.respond(warningMessage);
        }

        apiAIAProtocolService.exec(line);

        return true;
    }

    @Action(group = ApiAIAProtocolService.RESERVED_GROUP,
            command = "subscribe",
            description = "Subscribe to an event in this component")
    public Boolean subscribeToEvent(CommandContext context) {
        this.context = context;
        context.respond("Done");
        return true;
    }

    @Action(group = ApiAIAProtocolService.RESERVED_GROUP,
            command = "unsubscribe",
            description = "Unubscribe from an event in this component")
    public Boolean unsubscribeFromEvent(CommandContext context) {
        this.context = context;
        context.respond("Done");
        return true;
    }

    /*
    This action can be automatically added while generating the Pluginclass.
    not need to create an actual additional method, just use the subscribeToEvent we already
    have in this DynamicEventChannel class ( extended ).

    additinal check while creating actions:   no command "subscribe" could be added because it is a reserved word.

    @Action(group = MAIN_GROUP,
    command = "subscribe",
    description = "Subscribe to a ")
    public String subscribeToComponentEvent(String[] commandArgs) {
        this.subscribeToEvent("door_open", listener);
        return "";
    }

     */

}
