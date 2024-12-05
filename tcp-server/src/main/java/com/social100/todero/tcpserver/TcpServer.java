package com.social100.todero.tcpserver;

import com.social100.todero.console.base.CliCommandManager;
import com.social100.todero.common.config.AppConfig;
import com.social100.todero.tcpserver.config.TcpServerConfig;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class TcpServer {
    private final TcpServerConfig tcpServerConfig;
    private final CliCommandManager commandManager;
    private final ExecutorService threadPool;

    public TcpServer(AppConfig appConfig) {
        tcpServerConfig = new TcpServerConfig(appConfig);
        commandManager = new CliCommandManager(appConfig);
        threadPool = Executors.newFixedThreadPool(tcpServerConfig.THREAD_POOL_SIZE);
    }

    public void start() {
        try (ServerSocket serverSocket = new ServerSocket(tcpServerConfig.PORT)) {
            System.out.println("Server is listening on port " + tcpServerConfig.PORT);

            while (true) {
                Socket socket = serverSocket.accept();
                System.out.println("New client connected");
                threadPool.execute(new ClientHandler(socket, commandManager));
            }
        } catch (IOException ex) {
            System.err.println("Server exception: " + ex.getMessage());
            ex.printStackTrace();
        } finally {
            threadPool.shutdown();
            commandManager.terminate();
        }
    }
}
