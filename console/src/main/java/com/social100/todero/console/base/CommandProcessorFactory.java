package com.social100.todero.console.base;

import com.social100.todero.common.channels.EventChannel;
import com.social100.todero.common.config.AppConfig;
import com.social100.todero.common.config.ServerType;

import java.net.InetSocketAddress;

public class CommandProcessorFactory {
    public static CommandProcessor createRemoteProcessor(AppConfig appConfig, ServerType type, EventChannel.EventListener eventListener) {
        return new AiaCommandProcessor(new InetSocketAddress(appConfig.getApp().getServer().getHost(), appConfig.getApp().getServer().getPort()), eventListener);
    }

    public static CommandProcessor createStaticProcessor(AppConfig appConfig, ServerType type, EventChannel.EventListener eventListener) {
        return  new CliCommandProcessor(appConfig, type, eventListener);
    }
}