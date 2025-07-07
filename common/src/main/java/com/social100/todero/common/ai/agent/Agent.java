package com.social100.todero.common.ai.agent;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.social100.todero.common.ai.action.AgentAction;
import com.social100.todero.common.ai.action.CommandAction;
import com.social100.todero.common.ai.llm.LLMClient;
import com.social100.todero.common.ai.util.JsonUtils;

import java.util.Optional;

public class Agent implements AgentInterface {

  private final AgentDefinition definition;
  private final ObjectMapper mapper = new ObjectMapper();

  public Agent(AgentDefinition definition) {
    this.definition = definition;
  }

  @Override
  public AgentAction process(LLMClient llm, AgentPrompt prompt, AgentContext context) throws Exception {
    String systemPrompt = definition.getSystemPrompt();
    String contextJson = mapper.writeValueAsString(context.getAll());

    String raw = llm.chat(systemPrompt, prompt.getMessage(), contextJson);

    JsonNode root = JsonUtils.parse(raw);

    Optional<String> action = JsonUtils.getValue(root, "plan.action");

    return new CommandAction(action);
  }
}
