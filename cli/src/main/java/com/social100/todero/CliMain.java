package com.social100.todero;

import com.fasterxml.jackson.core.exc.StreamReadException;
import com.fasterxml.jackson.databind.DatabindException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.social100.todero.cli.base.Cli;
import com.social100.todero.common.config.AppConfig;
import com.social100.todero.common.config.Config;
import com.social100.todero.common.config.PluginsConfig;
import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.reader.impl.completer.StringsCompleter;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Scanner;

public class CliMain {
    static private AppConfig appConfig;

    public static void main(String[] args) {
        ObjectMapper yamlMapper = new ObjectMapper(new YAMLFactory());
        try {
            yamlMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
            yamlMapper.configure(DeserializationFeature.FAIL_ON_INVALID_SUBTYPE, false);
            appConfig = yamlMapper.readValue(new File("config.yaml"), AppConfig.class);
            Cli cli = new Cli(appConfig);
            cli.execute(args);
            cli.wait(60000);
        } catch (NullPointerException e) {
            System.out.println(e.getMessage());
        } catch (StreamReadException e) {
            System.out.println(e.getMessage());
        } catch (DatabindException e) {
            System.out.println(e.getMessage());
        } catch (IOException e) {
            System.out.println(e.getMessage());
        } catch (InterruptedException e) {
            System.out.println(e.getMessage());
        }
    }

    /*
    public static void main(String[] args) throws IOException {
        boolean isIDE = System.getProperty("java.class.path").contains("idea_rt.jar");
        System.out.println(System.getProperty("java.class.path"));
        if (isIDE) {
            // Use Scanner when running inside an IDE
            Scanner scanner = new Scanner(System.in);
            System.out.print("Enter input Scanner> ");
            String line = scanner.nextLine();
            System.out.println("You entered: " + line);
        } else {
            // Use JLine when running outside the IDE
            Terminal terminal = TerminalBuilder.builder()
                    .jna(false)
                    .jansi(true)
                    .system(true)
                    .build();

            LineReader lineReader = LineReaderBuilder.builder()
                    .terminal(terminal)
                    .build();

            String line = lineReader.readLine("Enter input> ");
            System.out.println("You entered: " + line);
        }
    }*/
}