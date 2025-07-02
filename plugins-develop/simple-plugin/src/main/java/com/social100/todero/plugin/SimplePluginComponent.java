package com.social100.todero.plugin;


import com.social100.processor.AIAController;
import com.social100.processor.Action;
import com.social100.todero.common.command.CommandContext;
import com.social100.todero.common.config.ServerType;
import com.social100.todero.processor.EventDefinition;

import java.util.Arrays;
import java.util.Map;

@AIAController(name = "simple",
        type = ServerType.AIA,
        description = "Simple Plugin",
        events = SimplePluginComponent.SimpleEvent.class)
public class SimplePluginComponent {
    final static String MAIN_GROUP = "Main";
    private CommandContext globalContext = null;

    public SimplePluginComponent() {
    }

    public enum SimpleEvent implements EventDefinition {
        SIMPLE_EVENT("A event to demo"),
        OTHER_EVENT("Other event to demo");

        private final String description;

        SimpleEvent(String description) {
            this.description = description;
        }

        @Override
        public String getDescription() {
            return description;
        }
    }

    @Action(group = MAIN_GROUP,
            command = "ping",
            description = "Does the ping")
    public Boolean pingCommand(CommandContext context) {
        String[] commandArgs = context.getArgs();
        context.event(SimpleEvent.SIMPLE_EVENT.name(), "No va a salir");
        context.respond("Ping Ok" + (commandArgs.length>0 ? " : " + commandArgs[0] : ""));
        return true;
    }

    @Action(group = MAIN_GROUP,
            command = "hello",
            description = "Does a friendly hello")
    public Boolean instanceMethod(CommandContext context) {
        String[] commandArgs = context.getArgs();
        Map<String, Object> mm = Map.of(
                "message", "Hello from instanceMethod",
                "args", Arrays.toString(commandArgs),
                "metadata", Map.of("key1", "value1", "key2", "value2")
        );
        context.event(SimpleEvent.OTHER_EVENT.name(), "Aja, aqui va!");
        context.respond(mm.toString());
        return true;
    }

    @Action(group = MAIN_GROUP,
            command = "events",
            description = "Start / Stop Sending events. Usage: events ON|OFF")
    public Boolean eventsCommand(CommandContext context) {
        String[] args = context.getArgs();
        if (args.length == 0) {
            context.respond(context.getInstance().getAvailableEvents().toString());
        } else {
            boolean eventsOn = "on".equalsIgnoreCase(args[0]);
            if (eventsOn) {
                this.globalContext = context;
                context.respond("events are now ON");
            } else {
                context.respond("events are now OFF");
                this.globalContext = null;
            }
        }
        return true;
    }
}
