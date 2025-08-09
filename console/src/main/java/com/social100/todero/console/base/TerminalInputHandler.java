package com.social100.todero.console.base;

import com.social100.todero.common.Constants;
import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.reader.impl.completer.StringsCompleter;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;

import java.io.IOException;
import java.util.function.Consumer;

public class TerminalInputHandler {
    private final Terminal terminal;
    private final LineReader lineReader;

    public TerminalInputHandler(String[] autocompleteStrings) throws IOException {
        boolean isWindows = System.getProperty("os.name").toLowerCase().contains("win");

        this.terminal = TerminalBuilder.builder()
                .system(true)
                .jna(!isWindows)       // enable JNA only on Unix-like
                .jansi(isWindows)      // enable JANSI only on Windows
                .build();

        LineReaderBuilder builder = LineReaderBuilder.builder().terminal(this.terminal);
        if (autocompleteStrings != null) {
            StringsCompleter completer = new StringsCompleter(autocompleteStrings);
            builder.completer(completer);
        }
        this.lineReader = builder.build();
    }

    public void processInput(Consumer<String> callback) throws IOException {
        String line;
        while (!(line = lineReader.readLine("> ")).equals(Constants.CLI_COMMAND_EXIT)) {
            callback.accept(line);
        }
    }

    public void close() throws IOException {
        terminal.close();
    }
}
