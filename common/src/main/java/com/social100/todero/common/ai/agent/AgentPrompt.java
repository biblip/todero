package com.social100.todero.common.ai.agent;

import lombok.Getter;

@Getter
public class AgentPrompt {
  private final String message;

  public AgentPrompt(String message) {
    this.message = message;
  }

}
