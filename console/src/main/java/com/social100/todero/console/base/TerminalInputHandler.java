package com.social100.todero.console.base;

import com.social100.todero.common.Constants;
import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.reader.impl.completer.StringsCompleter;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;

import java.io.IOException;

public class TerminalInputHandler {
    private final Terminal terminal;
    private final LineReader lineReader;

    public TerminalInputHandler(CommandManager commandManager) throws IOException {
        this.terminal = TerminalBuilder.builder()
                .jna(false)
                .jansi(true)
                .system(true)
                .build();

        LineReaderBuilder builder = LineReaderBuilder.builder().terminal(this.terminal);
        if (commandManager != null) {
            StringsCompleter completer = new StringsCompleter(commandManager.generateAutocompleteStrings());
            builder.completer(completer);
        }
        this.lineReader = builder.build();
    }

    public void processInput(CommandProcessor commandProcessor) throws IOException {
        String line;
        while (!(line = lineReader.readLine("> ")).equals(Constants.CLI_COMMAND_EXIT)) {
            commandProcessor.process(line);
        }
    }

    public void close() throws IOException {
        terminal.close();
    }
}
