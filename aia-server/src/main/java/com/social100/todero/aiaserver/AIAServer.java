package com.social100.todero.aiaserver;

import com.social100.todero.aiaserver.config.AIAServerConfig;
import com.social100.todero.cli.base.CommandManager;
import com.social100.todero.common.config.AppConfig;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AIAServer {
    private final AIAServerConfig tcpServerConfig;
    private final CommandManager commandManager;
    private final ExecutorService threadPool;

    public AIAServer(AppConfig appConfig) {
        tcpServerConfig = new AIAServerConfig(appConfig);
        commandManager = new CommandManager(appConfig);
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
