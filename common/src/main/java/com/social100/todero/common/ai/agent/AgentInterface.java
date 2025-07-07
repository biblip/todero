package com.social100.todero.common.ai.agent;

import com.social100.todero.common.ai.action.AgentAction;
import com.social100.todero.common.ai.llm.LLMClient;

public interface AgentInterface {
  /**
   * Processes a prompt with context and returns an executable Action.
   */
  AgentAction process(LLMClient llm, AgentPrompt prompt, AgentContext context) throws Exception;
}
