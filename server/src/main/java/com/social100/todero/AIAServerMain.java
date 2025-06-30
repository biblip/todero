package com.social100.todero;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.social100.todero.aiaserver.AIAServer;
import com.social100.todero.common.config.AppConfig;
import com.social100.todero.common.config.Config;
import com.social100.todero.common.config.ServerConfig;
import com.social100.todero.common.config.ServerType;
import com.social100.todero.common.log.LogRedirector;
import com.social100.todero.server.RawServer;

import java.io.File;
import java.io.IOException;

public class AIAServerMain {
    static private AppConfig appConfig_ai;
    static private AppConfig appConfig_aia;

    public static void main(String[] args) throws IOException {
        try {
            LogRedirector.initialize();
        } catch (IOException e) {
            e.printStackTrace(); // fallback to console
        }
        appConfig_ai = loadAppConfig(args);
        appConfig_ai.getApp().setType(ServerType.AI);
        appConfig_ai.getApp().getServer().setPort(ServerType.AI.getPort());
        RawServer aiaServer_ai = new AIAServer(appConfig_ai, ServerType.AI);
        aiaServer_ai.start();

        appConfig_aia = loadAppConfig(args);
        appConfig_ai.getApp().setType(ServerType.AIA);
        appConfig_ai.getApp().getServer().setPort(ServerType.AIA.getPort());
        RawServer aiaServer_aia = new AIAServer(appConfig_aia, ServerType.AIA);
        aiaServer_aia.start();

        //RawServer sshServer = new SshServer(appConfig);
        //sshServer.start();
    }

    public static AppConfig loadAppConfig(String[] args) {
        String configFilePath = null;

        // Check for the "--config" parameter in the command-line arguments.
        for (int i = 0; i < args.length; i++) {
            if ("--config".equals(args[i]) && (i + 1) < args.length) {
                configFilePath = args[i + 1];
                break;
            }
        }

        // If no config file is specified via parameter, look for "config.conf" in the jar directory.
        if (configFilePath == null) {
            try {
                File jarFile = new File(AIAServerMain.class.getProtectionDomain().getCodeSource().getLocation().toURI());
                String jarDir = jarFile.getParent();
                configFilePath = jarDir + File.separator + "config.yaml";
            } catch (Exception e) {
                System.err.println("Error determining jar file location: " + e.getMessage());
                System.exit(1);
            }
        }

        // Check if the configuration file exists.
        File configFile = new File(configFilePath);
        if (!configFile.exists()) {
            System.err.println("Configuration file not found: " + configFilePath);
            //System.exit(1);
        } else {
            // Parse the configuration file using YAML mapper.
            try {
                ObjectMapper yamlMapper = new ObjectMapper(new YAMLFactory());
                yamlMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
                yamlMapper.configure(DeserializationFeature.FAIL_ON_INVALID_SUBTYPE, false);

                AppConfig appConfig = yamlMapper.readValue(configFile, AppConfig.class);
                return appConfig;
            } catch (IOException e) {
                System.err.println("Error reading configuration file: " + e.getMessage());
            }
        }

        // Load AppConfig defaults
        AppConfig appConfig = new AppConfig();
        appConfig.setApp(new Config());
        appConfig.getApp().setServer(new ServerConfig());
        appConfig.getApp().getServer().setPort(9876);
        return appConfig;
    }
}
