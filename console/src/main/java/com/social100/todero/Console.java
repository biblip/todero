package com.social100.todero;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.social100.todero.common.config.AppConfig;
import com.social100.todero.common.config.ServerConfig;
import com.social100.todero.console.base.ConsoleCommandLineInterface;

import java.util.Arrays;

import static com.social100.todero.common.config.Utils.loadAppConfig;

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
            appConfig = loadAppConfig(args);

            ServerConfig serverConfig = aia ? appConfig.getApp().getAia_server() : appConfig.getApp().getAi_server();

            ConsoleCommandLineInterface consoleCommandLineInterface = new ConsoleCommandLineInterface(serverConfig);
            consoleCommandLineInterface.run(args);
        } catch (NullPointerException e) {
            System.out.println(e.getMessage());
        }
    }
}