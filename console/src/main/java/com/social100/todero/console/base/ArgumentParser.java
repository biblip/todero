package com.social100.todero.console.base;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ArgumentParser {

  private static final Pattern ARGUMENT_PATTERN = Pattern.compile("(\\$\\{[^}]+}|\"[^\"]+\"|\\S+)");

  /**
   * Parses a line into arguments, supporting quoted strings and placeholders.
   * Quoted strings will have quotes removed. Placeholders like ${...} are preserved as-is.
   *
   * @param line The input string to parse.
   * @return List of parsed arguments. Returns empty list if input is null, blank, or has no matches.
   */
  public static List<String> parseArguments(String line) {
    if (line == null || line.isBlank()) {
      return List.of();
    }

    Matcher matcher = ARGUMENT_PATTERN.matcher(line);
    List<String> arguments = new ArrayList<>();

    while (matcher.find()) {
      String arg = matcher.group(1);
      if (arg.startsWith("\"") && arg.endsWith("\"")) {
        arg = arg.substring(1, arg.length() - 1); // Remove quotes
      }
      arguments.add(arg);
    }

    return arguments;
  }
}
