package com.social100.todero.common.ai.action;

import java.util.Optional;

public class CommandAction implements AgentAction {
    private final Optional<String> command;

    public CommandAction(Optional<String> command) {
      this.command = command;
    }

    @Override
    public void execute() {
      // ignore
    }

    public Optional<String> getCommand() {
      return command;
    }
  }