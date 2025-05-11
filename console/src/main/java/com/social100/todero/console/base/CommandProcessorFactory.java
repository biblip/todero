package com.social100.todero.console.base;

import com.social100.todero.common.channels.EventChannel;
import com.social100.todero.common.config.AppConfig;

import java.net.InetSocketAddress;

public class CommandProcessorFactory {
    public static CommandProcessor createProcessor(AppConfig appConfig, EventChannel.EventListener eventListener, boolean aiaProtocol) {
        return aiaProtocol ? new AiaCommandProcessor(new InetSocketAddress(appConfig.getApp().getServer().getHost(), appConfig.getApp().getServer().getPort()), eventListener) : new CliCommandProcessor(appConfig, eventListener);
    }
}