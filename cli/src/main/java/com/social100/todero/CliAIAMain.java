package com.social100.todero;

import com.fasterxml.jackson.core.exc.StreamReadException;
import com.fasterxml.jackson.databind.DatabindException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.social100.todero.cli.base.Cli;
import com.social100.todero.common.config.AppConfig;

import java.io.File;
import java.io.IOException;

public class CliAIAMain {
    static private AppConfig appConfig;

    public static void main(String[] args) {
        ObjectMapper yamlMapper = new ObjectMapper(new YAMLFactory());
        try {
            yamlMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
            yamlMapper.configure(DeserializationFeature.FAIL_ON_INVALID_SUBTYPE, false);
            appConfig = yamlMapper.readValue(new File("config.yaml"), AppConfig.class);
            Cli cli = new Cli(appConfig, true);
            cli.execute(args);
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