package com.social100.todero.util;

import java.util.HashMap;
import java.util.Map;

/**
 * A utility class for parsing command-line arguments.
 * This parser supports named arguments, flags, and default values.
 * Arguments can be validated using custom rules.
 */
public class ArgumentParser {

    private final Map<String, Rule> rules = new HashMap<>();
    private final Map<String, String> defaults = new HashMap<>();
    private final Map<String, String> parsedArguments = new HashMap<>();
    private String errorMessage = null;

    /**
     * A functional interface representing a validation rule for an argument.
     */
    @FunctionalInterface
    public interface Rule {
        /**
         * Validates the given argument value.
         *
         * @param value The value to validate.
         * @return true if the value is valid, false otherwise.
         */
        boolean validate(String value);
    }

    /**
     * Adds a named argument with a validation rule and an optional default value.
     *
     * @param argumentName The name of the argument.
     * @param rule The validation rule for the argument.
     * @param defaultValue The default value for the argument (can be null).
     */
    public void addRule(String argumentName, Rule rule, String defaultValue) {
        rules.put(argumentName, rule);
        defaults.put(argumentName, defaultValue);
    }

    /**
     * Adds a flag argument. Flags are boolean arguments that default to false
     * and are set to true if present in the input.
     *
     * @param flagName The name of the flag.
     */
    public void addFlag(String flagName) {
        rules.put(flagName, value -> value.equals("true") || value.equals("false"));
        defaults.put(flagName, "false");
    }

    /**
     * Parses the input arguments and validates them against the defined rules.
     *
     * @param args The array of command-line arguments to parse.
     * @return true if parsing and validation succeed, false otherwise.
     */
    public boolean parse(String[] args) {
        StringBuilder currentValue = new StringBuilder();
        String currentKey = null;

        for (String arg : args) {
            if (arg.startsWith("--")) {
                // Finish the current key-value pair
                if (currentKey != null) {
                    parsedArguments.put(currentKey, currentValue.toString().trim());
                    currentValue.setLength(0);
                }

                currentKey = arg.replaceFirst("--", ""); // New key
                if (!rules.containsKey(currentKey)) {
                    errorMessage = "Unknown argument: '" + currentKey + "'";
                    return false;
                }

                // Check if the key is a flag
                if (defaults.containsKey(currentKey) && "false".equals(defaults.get(currentKey))) {
                    parsedArguments.put(currentKey, "true"); // Set flag to true
                    currentKey = null; // Flags do not have values
                }
            } else {
                currentValue.append(arg).append(" ");
            }
        }

        // Add the last key-value pair
        if (currentKey != null) {
            parsedArguments.put(currentKey, currentValue.toString().trim());
        }

        // Validate and set defaults
        for (String key : rules.keySet()) {
            if (!parsedArguments.containsKey(key)) {
                parsedArguments.put(key, defaults.get(key));
            } else if (!rules.get(key).validate(parsedArguments.get(key))) {
                errorMessage = "Validation failed for argument: '" + key + "' with value '" + parsedArguments.get(key) + "'";
                return false;
            }
        }

        return true;
    }

    /**
     * Retrieves the value of a named argument.
     *
     * @param key The name of the argument.
     * @return The value of the argument, or null if not provided and no default exists.
     */
    public String getArgument(String key) {
        return parsedArguments.get(key);
    }

    /**
     * Retrieves the value of a flag argument.
     *
     * @param key The name of the flag.
     * @return true if the flag is present, false otherwise.
     */
    public boolean getFlag(String key) {
        return Boolean.parseBoolean(parsedArguments.get(key));
    }

    /**
     * Retrieves the error message if parsing failed.
     *
     * @return The error message, or null if no error occurred.
     */
    public String errorMessage() {
        return errorMessage;
    }

    /**
     * Main method for demonstration and testing of the ArgumentParser class.
     *
     * @param args The command-line arguments.
     */
    public static void main(String[] args) {
        ArgumentParser parser = new ArgumentParser();

        // Define rules with default values
        parser.addRule("url", value -> value.startsWith("http://") || value.startsWith("https://"), "http://default.com");
        parser.addRule("type", value -> value.equals("alias"), "alias");
        parser.addRule("alias", value -> value != null && !value.trim().isEmpty(), "defaultAlias");

        // Define a rule without a default value
        parser.addRule("description", value -> value != null && value.length() > 0, null);

        // Define a flag
        parser.addFlag("verbose");

        // Example input
        String[] commandArgs = {
                "--url", "https://example.com",
                "--verbose",
                "--description", "This is a test description with spaces"
        };

        // Parse arguments
        if (parser.parse(commandArgs)) {
            System.out.println("URL: " + parser.getArgument("url"));
            System.out.println("Type: " + parser.getArgument("type"));
            System.out.println("Alias: " + parser.getArgument("alias"));
            System.out.println("Description: " + parser.getArgument("description"));
            System.out.println("Verbose: " + parser.getFlag("verbose"));
        } else {
            System.err.println("Failed to parse arguments: " + parser.errorMessage());
        }
    }
}
