package com.social100.todero.common.ai.llm;

public interface LLMClient {
  /**
   * Sends a prompt and context to the LLM, and expects a JSON-formatted response string.
   */
  String chat(String systemPrompt, String userPrompt, String contextJson) throws Exception;
}
