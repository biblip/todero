package com.social100.todero.common.ai.action;

public class FunctionCallAction implements AgentAction {
  private final Runnable function;

  public FunctionCallAction(Runnable function) {
    this.function = function;
  }

  @Override
  public void execute() {
    function.run();
  }
}
