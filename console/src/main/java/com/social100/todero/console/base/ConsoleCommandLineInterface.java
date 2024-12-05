package com.social100.todero.console.base;

import com.social100.todero.common.Constants;
import com.social100.todero.common.config.AppConfig;

import java.io.IOException;
import java.util.Arrays;
import java.util.Scanner;

public class ConsoleCommandLineInterface implements CommandLineInterface {
    private final CommandProcessor commandProcessor;

    public ConsoleCommandLineInterface(AppConfig appConfig, boolean aiaProtocol) {
        this.commandProcessor = CommandProcessorFactory.createProcessor(appConfig, aiaProtocol);
        this.commandProcessor.open();
        this.commandProcessor.getBridge().readAsync(this::outputDataHandler);
    }

    private void outputDataHandler(byte[] data) {
        System.out.print(new String(data));
    }

    @Override
    public void run(String[] args) {
        boolean useScanner = args != null && Arrays.asList(args).contains("--useScanner");

        try {
            if (useScanner) {
                processInputWithScanner();
            } else {
                processInputWithTerminal();
            }
        } catch (IOException e) {
            System.out.println("Error during CLI execution: " + e.getMessage());
        } finally {
            commandProcessor.close();
        }
    }

    private void processInputWithScanner() {
        try (Scanner scanner = new Scanner(System.in)) {
            String line;
            System.out.print("> ");
            while (!(line = scanner.nextLine()).equals(Constants.CLI_COMMAND_EXIT)) {
                if (!line.trim().isEmpty()) {
                    commandProcessor.process(line);
                }
                System.out.print("\n> ");
            }
        }
    }

    private void processInputWithTerminal() throws IOException {
        String[] autocompleteStrings = null;
        if (commandProcessor.getCommandManager() != null) {
            autocompleteStrings = commandProcessor.getCommandManager().generateAutocompleteStrings();
        }
        TerminalInputHandler inputHandler = new TerminalInputHandler(autocompleteStrings);
        try {
            inputHandler.processInput(commandProcessor::process);
        } finally {
            inputHandler.close();
        }
    }
}