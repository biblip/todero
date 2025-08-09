package aia.plugin.com.social100.agent_demo;

import com.social100.processor.AIAController;
import com.social100.processor.Action;
import com.social100.todero.common.ai.action.CommandAction;
import com.social100.todero.common.ai.agent.Agent;
import com.social100.todero.common.ai.agent.AgentContext;
import com.social100.todero.common.ai.agent.AgentDefinition;
import com.social100.todero.common.ai.agent.AgentPrompt;
import com.social100.todero.common.ai.llm.LLMClient;
import com.social100.todero.common.ai.llm.OllamaLLM;
import com.social100.todero.common.command.CommandContext;
import com.social100.todero.common.config.ServerType;
import com.social100.todero.processor.EventDefinition;
import io.github.cdimascio.dotenv.Dotenv;

import java.util.List;
import java.util.Optional;

import static com.social100.todero.console.base.ArgumentParser.parseArguments;

@AIAController(name = "com.shellaia.verbatim.agent.dj",
    type = ServerType.AI,
    visible = true,
    description = "Simple Agent Demo",
    events = AgentDJComponent.SimpleEvent.class)
public class AgentDJComponent {
  final static String MAIN_GROUP = "Main";
  private CommandContext globalContext = null;
  final AgentDefinition agentDefinition;
  final String openApiKey;

  public AgentDJComponent() {
    agentDefinition = AgentDefinition.builder()
        .name("DJ Agent")
        .role("Assistant")
        .description("handle a music playback system")
        //.model("gpt-4.1-nano")
        //.model("qwen3:4b")
        .model("gemma3:4b")
        //.model("gemma3:12b")
        .systemPrompt(AgentDefinition.loadSystemPromptFromResource("prompts/default-system-prompt.txt"))
        .build();

    agentDefinition.setMetadata("region", "US");

    Dotenv dotenv = Dotenv.configure().filename(".env").load();
    this.openApiKey = dotenv.get("OPENAI_API_KEY","openai_api_key_value");
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
      description = "Send a prompt to the agent to process it")
  public Boolean agentProcess(CommandContext context) {
    String prompt = String.join(" ", context.getArgs());

    AgentContext agentContext = new AgentContext();
    //context.set("name", "Arturo");
    //context.set("goal", "greet the user and confirm last command");
    //context.set("lastCommand", "restart nginx");

    // Agent 1: Planner (e.g. decomposes task)
    LLMClient llm = new OllamaLLM("http://11.11.11.1:11434", agentDefinition.getModel());
    //LLMClient llm = new OllamaAiLLM(this.openApiKey, agentDefinition.getModel());

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
            .sourceId("333")
            .args(commandArgs)
            .build();

        internalContext.setListener(context::respond);

        context.execute("com.shellaia.verbatim.plugin.vlc", command, internalContext);
        //context.execute("vlc", command, internalContext);
      });

    } catch (Exception e) {
      throw new RuntimeException(e);
    }

    return true;
  }
}
