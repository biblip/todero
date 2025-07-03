package com.social100.todero.agent_demo;

import com.social100.processor.AIAController;
import com.social100.processor.Action;
import com.social100.todero.common.command.CommandContext;
import com.social100.todero.common.config.ServerType;
import com.social100.todero.processor.EventDefinition;

import java.util.Arrays;
import java.util.Map;

@AIAController(name = "agent_demo",
    type = ServerType.AI,
    visible = false,
    description = "Simple Agent Demo",
    events = AgentDemoComponent.SimpleEvent.class)
public class AgentDemoComponent {
  final static String MAIN_GROUP = "Main";
  private CommandContext globalContext = null;

  public AgentDemoComponent() {
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
      command = "prompt",
      description = "Send a prompt to the agent")
  public Boolean pingCommand(CommandContext context) {
    String[] commandArgs = context.getArgs();

    CommandContext internalContext = CommandContext.builder()
        .sourceId("333")
        .args(context.getArgs())
        .build();

    System.out.println("------------------------------------------");
    context.execute("com.shellaia.verbatim.plugin.simple_plugin", "ping", context);
    System.out.println("------------------------------------------");

    context.event(SimpleEvent.SIMPLE_EVENT.name(), "No va a salir");
    context.respond("Ping Ok" + (commandArgs.length>0 ? " : " + commandArgs[0] : ""));
    return true;
  }

  @Action(group = MAIN_GROUP,
      command = "context",
      description = "setup the context for the agent")
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
