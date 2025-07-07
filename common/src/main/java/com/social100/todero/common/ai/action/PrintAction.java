package com.social100.todero.common.ai.action;

public class PrintAction implements AgentAction {
  private final String output;

  public PrintAction(String output) {
    this.output = output;
  }

  @Override
  public void execute() {
    System.out.println("PrintAction: " + output);
  }
}
