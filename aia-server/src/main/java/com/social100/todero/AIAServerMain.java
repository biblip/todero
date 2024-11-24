package com.social100.todero;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.social100.todero.common.config.AppConfig;
import com.social100.todero.aiaserver.AIAServer;

import java.io.File;

public class AIAServerMain {
    static private AppConfig appConfig;

    public static void main(String[] args) {
        ObjectMapper yamlMapper = new ObjectMapper(new YAMLFactory());
        try {
            yamlMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
            yamlMapper.configure(DeserializationFeature.FAIL_ON_INVALID_SUBTYPE, false);

            appConfig = yamlMapper.readValue(new File("config.yaml"), AppConfig.class);
            AIAServer server = new AIAServer(appConfig);
            server.start();
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }
}
