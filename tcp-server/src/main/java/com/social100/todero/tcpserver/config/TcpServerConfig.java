package com.social100.todero.tcpserver.config;

import com.social100.todero.common.config.AppConfig;
import lombok.Data;
import lombok.Getter;

import java.util.Map;

public class TcpServerConfig {
    final public int PORT;
    final public int THREAD_POOL_SIZE;

    @SuppressWarnings("unchecked")
    public TcpServerConfig(AppConfig appConfig) {
        String TCP_SERVER_CONFIG = "tcpserver";
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
