package com.social100.todero.aiaserver;

import com.social100.todero.cli.base.CommandManager;
import org.jline.utils.InputStreamReader;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.Socket;

class ClientHandler implements Runnable {

    private final Socket socket;
    private final CommandManager commandManager;

    public ClientHandler(Socket socket, CommandManager commandManager) {
        this.socket = socket;
        this.commandManager = commandManager;
    }

    @Override
    public void run() {
        try (InputStream input = socket.getInputStream();
             OutputStream output = socket.getOutputStream();
             BufferedReader reader = new BufferedReader(new InputStreamReader(input));
             PrintWriter writer = new PrintWriter(output, true)) {

            String line;
            while ((line = reader.readLine()) != null) {
                if (line.equalsIgnoreCase("exit")) {
                    break;
                }
                String outputLine = commandManager.execute(line);
                writer.print(outputLine.replace("\n", "\r\n"));
            }
        } catch (IOException ignore) {
            //System.err.println("Client handler exception: " + ex.getMessage());
            //ex.printStackTrace();
        } finally {
            try {
                socket.close();
            } catch (IOException ex) {
                System.err.println("Could not close socket: " + ex.getMessage());
                ex.printStackTrace();
            }
            System.out.println("Client disconnected");
        }
    }
}