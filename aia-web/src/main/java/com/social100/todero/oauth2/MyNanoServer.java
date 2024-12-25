package com.social100.todero.oauth2;

import fi.iki.elonen.NanoHTTPD;

import java.io.IOException;

public class MyNanoServer extends NanoHTTPD {

    private final WebSocketManager webSocketManager;

    public MyNanoServer(int port) {
        super(port);
        this.webSocketManager = new WebSocketManager(port + 1); // WebSocket on a different port
    }

    @Override
    public Response serve(IHTTPSession session) {
        String uri = session.getUri();
        switch (uri) {
            case "/index":
                return serveIndexPage();
            default:
                return newFixedLengthResponse(Response.Status.NOT_FOUND, "text/plain", "404 Not Found");
        }
    }

    private Response serveIndexPage() {
        String html = """
                
                """;
        return newFixedLengthResponse(Response.Status.OK, "text/html", html);
    }

    public static void main(String[] args) {
        try {
            MyNanoServer server = new MyNanoServer(8080);
            server.start(NanoHTTPD.SOCKET_READ_TIMEOUT, false);

            WebSocketManager wsManager = new WebSocketManager(8081);
            wsManager.start(30_000, false);

            System.out.println("HTTP server is running on http://localhost:8080/");
            System.out.println("WebSocket server is running on ws://localhost:8081/");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
