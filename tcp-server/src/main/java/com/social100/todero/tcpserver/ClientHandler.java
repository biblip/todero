package com.social100.todero.tcpserver;

import com.social100.todero.common.channels.EventChannel;
import com.social100.todero.common.config.AppConfig;
import com.social100.todero.console.base.CliCommandManager;
import org.jline.utils.InputStreamReader;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.Socket;

class ClientHandler implements Runnable {

    private final Socket socket;
    private final AppConfig appConfig;

    public ClientHandler(AppConfig appConfig, Socket socket) {
        this.appConfig = appConfig;
        this.socket = socket;
    }

    @Override
    public void run() {
        CliCommandManager commandManager = null;
        try (InputStream input = socket.getInputStream();
             OutputStream output = socket.getOutputStream();
             BufferedReader reader = new BufferedReader(new InputStreamReader(input));
             PrintWriter writer = new PrintWriter(output, true)) {

            EventChannel.EventListener eventListener = new EventChannel.EventListener() {
                @Override
                public void onEvent(String eventName, String message) {
                    writer.print(message.replace("\n", "\r\n"));
                    writer.flush();
                }
            };
            commandManager = new CliCommandManager(this.appConfig, eventListener);

            String line;
            while ((line = reader.readLine()) != null) {
                if (line.equalsIgnoreCase("exit")) {
                    break;
                }
                commandManager.process(line);
            }
        } catch (IOException ignore) {
        } finally {
            try {
                socket.close();
            } catch (IOException ex) {
                System.err.println("Could not close socket: " + ex.getMessage());
                ex.printStackTrace();
            }
            if (commandManager != null) {
                commandManager.terminate();
            }
            System.out.println("Client disconnected");
        }
    }
}