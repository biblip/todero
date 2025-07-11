package com.social100.todero;

import com.fasterxml.jackson.core.exc.StreamReadException;
import com.fasterxml.jackson.databind.DatabindException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.social100.todero.common.config.ServerType;
import com.social100.todero.console.base.ConsoleCommandLineInterface;
import com.social100.todero.common.config.AppConfig;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;

public class Console {
    static private AppConfig appConfig;

    public static void main(String[] args) {
        boolean aia = false;
        ObjectMapper yamlMapper = new ObjectMapper(new YAMLFactory());
        if (args != null && args.length > 0) {
            if (Arrays.asList(args).contains("--aia")) {
                aia = true;
            }
        }
        try {
            yamlMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
            yamlMapper.configure(DeserializationFeature.FAIL_ON_INVALID_SUBTYPE, false);
            appConfig = yamlMapper.readValue(new File("config.yaml"), AppConfig.class);
            if (aia) {
                appConfig.getApp().getServer().setPort(ServerType.AIA.getPort());
            } else {
                appConfig.getApp().getServer().setPort(ServerType.AI.getPort());
            }
            ConsoleCommandLineInterface consoleCommandLineInterface = new ConsoleCommandLineInterface(appConfig, aia ? ServerType.AIA : ServerType.AI);
            consoleCommandLineInterface.run(args);
        } catch (NullPointerException e) {
            System.out.println(e.getMessage());
        } catch (StreamReadException e) {
            System.out.println(e.getMessage());
        } catch (DatabindException e) {
            System.out.println(e.getMessage());
        } catch (IOException e) {
            System.out.println(e.getMessage());
        }
    }
}