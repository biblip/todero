package aia.plugin.com.social100.agent_demo;

import com.social100.processor.AIAController;
import com.social100.processor.Action;
import com.social100.todero.common.ai.action.CommandAction;
import com.social100.todero.common.ai.agent.Agent;
import com.social100.todero.common.ai.agent.AgentContext;
import com.social100.todero.common.ai.agent.AgentDefinition;
import com.social100.todero.common.ai.agent.AgentPrompt;
import com.social100.todero.common.ai.llm.LLMClient;
import com.social100.todero.common.ai.llm.OpenAiLLM;
import com.social100.todero.common.command.CommandContext;
import com.social100.todero.common.config.ServerType;
import com.social100.todero.console.base.OutputType;
import com.social100.todero.processor.EventDefinition;

import java.util.List;
import java.util.Optional;

import static com.social100.todero.console.base.ArgumentParser.parseArguments;

@AIAController(name = "com.shellaia.verbatim.agent.demo",
    type = ServerType.AI,
    visible = true,
    description = "Simple Agent Demo",
    events = AgentDemoComponent.SimpleEvent.class)
public class AgentDemoComponent {
  final static String MAIN_GROUP = "Main";
  private CommandContext globalContext = null;
  final AgentDefinition agentDefinition;

  public AgentDemoComponent() {
    agentDefinition = AgentDefinition.builder()
        .name("DJ Agent")
        .role("Assistant")
        .description("handle a music playback system")
        .model("gpt-4.1-nano")
        .systemPrompt(AgentDefinition.loadSystemPromptFromResource("prompts/tool-list-generator.txt"))
        .build();

    agentDefinition.setMetadata("region", "US");
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
      command = "process",
      description = "Send a prompt to the agent")
  public Boolean agentProcess(CommandContext context) {
    String prompt = context.getHelp("", "", OutputType.JSON); //String.join(" ", context.getArgs());

    //  ******************************************************************
    //  ******************************************************************
    //  ******************************************************************

    AgentContext agentContext = new AgentContext();
    //context.set("name", "Arturo");
    //context.set("goal", "greet the user and confirm last command");
    //context.set("lastCommand", "restart nginx");

    // Agent 1: Planner (e.g. decomposes task)
    LLMClient llm = new OpenAiLLM(System.getenv("OPENAI_API_KEY"), agentDefinition.getModel());

    Agent planner = new Agent(agentDefinition);

    AgentPrompt agentPrompt = new AgentPrompt(prompt);

    try {
      CommandAction ss = (CommandAction) planner.process(llm, agentPrompt, agentContext);
      Optional<String> action = ss.getCommand();
      action.ifPresent(line -> {
        line = line.strip();
        List<String> arguments = parseArguments(line);
        String command = arguments.isEmpty() ? null : arguments.remove(0);
        String[] commandArgs = arguments.toArray(new String[0]);

        CommandContext internalContext = CommandContext.builder()
            .args(commandArgs)
            .build();

        internalContext.setListener(context::respond);

        context.execute("com.shellaia.verbatim.plugin.vlc_plugin", command, internalContext);
      });

    } catch (Exception e) {
      throw new RuntimeException(e);
    }

    return true;
  }
}
