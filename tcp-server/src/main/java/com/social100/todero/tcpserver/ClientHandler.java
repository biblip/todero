package com.social100.todero.tcpserver;

import com.social100.todero.cli.base.CommandManager;
import org.jline.utils.InputStreamReader;

import java.io.*;
import java.net.Socket;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
            Pattern pattern = Pattern.compile("([^\"]\\S*|\".+?\")\\s*");

            while ((line = reader.readLine()) != null) {

                if (line.equalsIgnoreCase("exit")) {
                    break;
                }

                Matcher matcher = pattern.matcher(line);
                ArrayList<String> arguments = new ArrayList<>();

                while (matcher.find()) {
                    arguments.add(matcher.group(1).replace("\"", ""));
                }

                if (!arguments.isEmpty()) {
                    String firstParam = arguments.remove(0);
                    String secondParam = null;
                    String[] commandArgs = {};
                    if (!arguments.isEmpty()) {
                        secondParam = arguments.remove(0);
                    }
                    if (!arguments.isEmpty()) {
                        commandArgs = arguments.toArray(new String[0]);
                    }
                    String outputLine = commandManager.execute(firstParam, secondParam, commandArgs);
                    writer.println(outputLine.replace("\n", "\r\n"));
                }
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