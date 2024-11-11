package com.social100.todero.cli.base;

import com.social100.todero.common.Constants;
import com.social100.todero.common.config.AppConfig;
import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.reader.impl.completer.StringsCompleter;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;


import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Cli {
    private final CommandManager commandManager;

    public Cli(AppConfig appConfig) {
        commandManager = new CommandManager(appConfig);
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
                    processLine(pattern, line);
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
                        .completer(new StringsCompleter(commandManager.getAllCommandNames()))
                        .build();

                while (!(line = lineReader.readLine("> ")).equals(Constants.CLI_COMMAND_EXIT)) {
                    processLine(pattern, line);
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
            commandManager.terminate();
        }
    }

    private void processLine(Pattern pattern, String line) {
        Matcher matcher = pattern.matcher(line);
        ArrayList<String> arguments = new ArrayList<>();

        while (matcher.find()) {
            arguments.add(matcher.group(1).replace("\"", ""));
        }

        if (!arguments.isEmpty()) {
            String firstParam = arguments.remove(0);
            String secondParam = null;
            String[] commandArgs = {};
            if (!arguments.isEmpty()) {
                secondParam = arguments.remove(0);
            }
            if (!arguments.isEmpty()) {
                commandArgs = arguments.toArray(new String[0]);
            }
            String output = commandManager.execute(firstParam, secondParam, commandArgs);
            System.out.println(output);
        }
    }
}
