package com.social100.todero.console.base;

import com.social100.todero.common.Constants;
import com.social100.todero.common.config.AppConfig;
import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.reader.impl.completer.StringsCompleter;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;

import java.io.IOException;
import java.util.Arrays;
import java.util.Scanner;

public class ConsoleCommandLineInterface implements CommandLineInterface {
    protected final AppConfig appConfig;
    private final CommandProcessor commandProcessor;

    public ConsoleCommandLineInterface(AppConfig appConfig) {
        this.appConfig = appConfig;
        this.commandProcessor = new CliCommandProcessor(this.appConfig);
        this.commandProcessor.open();
    }

    public ConsoleCommandLineInterface(AppConfig appConfig, boolean aiaProtocol) {
        this.appConfig = appConfig;
        if (aiaProtocol) {
            commandProcessor = new AiaCommandProcessor(this.appConfig);
        } else {
            commandProcessor = new CliCommandProcessor(this.appConfig);
        }
        this.commandProcessor.open();
    }

    public void run(String[] args) {
        boolean useScanner = false; // Default to not using JLine

        // Check command-line arguments
        if (args != null && args.length > 0) {
            if (Arrays.asList(args).contains("--useScanner")) {
                useScanner = true;
            }
        }

        Terminal terminal = null;
        try {
            String line;

            if (useScanner) {
                System.out.println("using scanner");
                // Use Simple Scanner for input
                Scanner scanner = new Scanner(System.in);
                System.out.print(">");
                while (!(line = scanner.nextLine()).equals(Constants.CLI_COMMAND_EXIT)) {
                    if (!line.trim().isEmpty()) {
                        commandProcessor.process(line);
                    }
                    System.out.print("\n>");
                }
            } else {
                terminal = TerminalBuilder.builder()
                        .jna(false)
                        .jansi(true)
                        .system(true)
                        .build();

                LineReader lineReader = LineReaderBuilder.builder()
                        .terminal(terminal)
                        .completer(new StringsCompleter(commandProcessor.getCommandManager().generateAutocompleteStrings()))
                        .build();

                while (!(line = lineReader.readLine("> ")).equals(Constants.CLI_COMMAND_EXIT)) {
                    commandProcessor.process(line);
                }
            }
        } catch (IOException e) {
            System.out.println(e.getMessage());
            e.printStackTrace();
        } finally {
            if (terminal != null) {
                try {
                    terminal.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            commandProcessor.close();
        }
    }
}
