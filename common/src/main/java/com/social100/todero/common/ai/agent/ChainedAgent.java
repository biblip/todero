package com.social100.todero.common.ai.agent;

import com.social100.todero.common.ai.action.AgentAction;
import com.social100.todero.common.ai.action.PrintAction;
import com.social100.todero.common.ai.llm.LLMClient;

import java.util.ArrayList;
import java.util.List;

public class ChainedAgent implements AgentInterface {

  private final List<AgentInterface> agents = new ArrayList<>();

  public ChainedAgent(List<AgentInterface> agents) {
    this.agents.addAll(agents);
  }

  @Override
  public AgentAction process(LLMClient llm, AgentPrompt prompt, AgentContext context) throws Exception {
    for (AgentInterface agent : agents) {
      AgentAction action = agent.process(llm, prompt, context);

      // Optional: add logic to decide whether to stop early
      if (isFinalAction(action)) {
        return action;
      }

      // Optionally update context based on action (e.g., record intermediate results)
      context.set("lastActionType", action.getClass().getSimpleName());
    }

    // Fallback action if none returned final
    return new PrintAction("No agent in the chain produced a final action.");
  }

  private boolean isFinalAction(AgentAction action) {
    // You can expand this to use metadata, return types, or a richer Action model
    return !(action instanceof PrintAction && ((PrintAction) action).toString().contains("Unknown"));
  }
}
