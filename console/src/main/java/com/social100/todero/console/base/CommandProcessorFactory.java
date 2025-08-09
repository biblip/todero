package com.social100.todero.console.base;

import com.social100.todero.common.channels.EventChannel;
import com.social100.todero.common.config.AppConfig;
import com.social100.todero.common.config.ServerConfig;
import com.social100.todero.common.config.ServerType;

import java.net.InetSocketAddress;

public class CommandProcessorFactory {
    public static CommandProcessor createRemoteProcessor(ServerConfig serverConfig, EventChannel.EventListener eventListener) {
        return new AiaCommandProcessor(new InetSocketAddress(serverConfig.getHost(), serverConfig.getPort()), eventListener);
    }

    public static CommandProcessor createStaticProcessor(AppConfig appConfig, ServerType type, EventChannel.EventListener eventListener) {
        return  new CliCommandProcessor(appConfig, type, eventListener);
    }
}