package com.social100.todero.common.base;

import com.social100.todero.common.command.CommandContext;
import com.social100.todero.console.base.OutputType;

import java.util.List;

public interface PluginManagerInterface {
  List<String> generateAutocompleteStrings();
  String getHelp(String pluginName, String commandName, OutputType outputType);
  void execute(String pluginName, String command, CommandContext context);
}
