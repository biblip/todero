package com.social100.todero.aiaserver.config;

import com.social100.todero.common.config.AppConfig;

import java.util.Map;

public class AIAServerConfig {
    final public int PORT;
    final public int THREAD_POOL_SIZE;

    @SuppressWarnings("unchecked")
    public AIAServerConfig(AppConfig appConfig) {
        String TCP_SERVER_CONFIG = "aiaserver";
        int port = 8070;
        int thread_pool_size = 10;
        Object tcpServerConfigObject = appConfig.getApp().getParameters().get(TCP_SERVER_CONFIG);
        if (tcpServerConfigObject instanceof Map<? , ?>) {
            Map<String, Object> tcpServerConfig = (Map<String, Object>) tcpServerConfigObject;
            port = (tcpServerConfig.get("port") instanceof Integer)
                    ? (int) tcpServerConfig.get("port") : port;
            thread_pool_size = (tcpServerConfig.get("thread_pool_size") instanceof Integer)
                    ? (int) tcpServerConfig.get("thread_pool_size") : thread_pool_size;
        }
        PORT = port;
        THREAD_POOL_SIZE = thread_pool_size;
    }
}
