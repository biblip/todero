package com.social100.todero.tcpserver;

import com.social100.todero.common.config.AppConfig;
import com.social100.todero.common.config.ServerType;
import com.social100.todero.tcpserver.config.TcpServerConfig;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class TcpServer {
    private final TcpServerConfig tcpServerConfig;
    private final ExecutorService threadPool;
    private final AppConfig appConfig;
    private final ServerType type;

    public TcpServer(AppConfig appConfig, ServerType type) {
        this.appConfig = appConfig;
        this.type = type;
        this.tcpServerConfig = new TcpServerConfig(appConfig);
        this.threadPool = Executors.newFixedThreadPool(tcpServerConfig.THREAD_POOL_SIZE);
    }

    public void start() {
        try (ServerSocket serverSocket = new ServerSocket(tcpServerConfig.PORT)) {
            System.out.println("Server is listening on port " + tcpServerConfig.PORT);

            while (true) {
                Socket socket = serverSocket.accept();
                System.out.println("New client connected");
                threadPool.execute(new ClientHandler(this.appConfig, this.type, socket));
            }
        } catch (IOException ex) {
            System.err.println("Server exception: " + ex.getMessage());
            ex.printStackTrace();
        } finally {
            threadPool.shutdown();
        }
    }
}
