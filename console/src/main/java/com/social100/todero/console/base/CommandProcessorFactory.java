package com.social100.todero.console.base;

import com.social100.todero.common.config.AppConfig;

public class CommandProcessorFactory {
    public static CommandProcessor createProcessor(AppConfig appConfig, boolean aiaProtocol) {
        return aiaProtocol ? new AiaCommandProcessor(appConfig) : new CliCommandProcessor(appConfig);
    }
}