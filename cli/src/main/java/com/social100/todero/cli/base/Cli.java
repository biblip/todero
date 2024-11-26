package com.social100.todero.cli.base;

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
import java.util.regex.Pattern;

public class Cli {
    private final CommandProcessor commandProcessor;

    public Cli(AppConfig appConfig) {
        this.commandProcessor = new CliCommandProcessor(appConfig);
        this.commandProcessor.open();
    }

    public Cli(AppConfig appConfig, boolean aiaProtocol) {
        if (aiaProtocol) {
            commandProcessor = new AiaCommandProcessor(appConfig);
        } else {
            commandProcessor = new CliCommandProcessor(appConfig);
        }
        this.commandProcessor.open();
    }

    public void execute(String[] args) {
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
            Pattern pattern = Pattern.compile("([^\"]\\S*|\".+?\")\\s*");

            if (useScanner) {
                // Use Simple Scanner for input
                Scanner scanner = new Scanner(System.in);
                System.out.print(">");
                while (!(line = scanner.nextLine()).equals(Constants.CLI_COMMAND_EXIT)) {
                    commandProcessor.process(pattern, line);
                    System.out.print(">");
                }
            } else {
                terminal = TerminalBuilder.builder()
                        .jna(false)
                        .jansi(true)
                        .system(true)
                        .build();

                LineReader lineReader = LineReaderBuilder.builder()
                        .terminal(terminal)
                        .completer(new StringsCompleter(commandProcessor.getCommandManager().getAllCommandNames()))
                        .build();

                while (!(line = lineReader.readLine("> ")).equals(Constants.CLI_COMMAND_EXIT)) {
                    commandProcessor.process(pattern, line);
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
