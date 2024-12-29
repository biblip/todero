package com.social100.todero.web;

import fi.iki.elonen.NanoWSD;

import java.io.IOException;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class WebSocketManager extends NanoWSD {

    private static final Set<MyWebSocket> webSockets = Collections.newSetFromMap(new ConcurrentHashMap<>());

    public WebSocketManager(int port) {
        super(port);
    }

    @Override
    protected WebSocket openWebSocket(IHTTPSession handshake) {
        MyWebSocket webSocket = new MyWebSocket(handshake);
        webSockets.add(webSocket);
        return webSocket;
    }

    private static class MyWebSocket extends WebSocket {

        private volatile long lastInteractionTime = System.currentTimeMillis(); // Tracks last interaction time
        private volatile boolean keepAliveRunning = true; // Control flag for the timer thread

        public MyWebSocket(IHTTPSession handshakeRequest) {
            super(handshakeRequest);
        }

        @Override
        protected void onOpen() {
            resetInteractionTime();
            startKeepAliveTimer();
        }

        private void startKeepAliveTimer() {
            new Thread(() -> {
                while (isOpen() && keepAliveRunning) {
                    try {
                        // Check time since last interaction
                        long timeSinceLastInteraction = System.currentTimeMillis() - lastInteractionTime;
                        if (timeSinceLastInteraction >= 25_000) { // 25 seconds
                            sendFrame(new WebSocketFrame(WebSocketFrame.OpCode.Ping, true, new byte[0]));
                        }
                        Thread.sleep(5_000); // Check every 5 seconds
                    } catch (Exception e) {
                        e.printStackTrace();
                        break;
                    }
                }
            }).start();
        }

        @Override
        protected void onClose(WebSocketFrame.CloseCode code, String reason, boolean initiatedByRemote) {
            webSockets.remove(this);
            keepAliveRunning = false;
        }

        @Override
        protected void onMessage(WebSocketFrame message) {
            try {
                resetInteractionTime();
                String payload = message.getTextPayload();
                System.out.println("Received: " + payload);

                // Handle message
                String response = handleInteraction(payload);
                send(response);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        @Override
        protected void onPong(WebSocketFrame pong) {
            resetInteractionTime();
        }

        private void resetInteractionTime() {
            lastInteractionTime = System.currentTimeMillis();
        }

        private String handleInteraction(String payload) {
            // Parse and process the payload
            return payload;
        }

        @Override
        protected void onException(IOException e) {
            try {
                close(WebSocketFrame.CloseCode.InternalServerError, "Internal error", false);
            } catch (IOException closeException) {
                closeException.printStackTrace();
            }
        }
    }

    // Broadcast a message to all connected clients
    public void broadcast(String message) {
        for (MyWebSocket webSocket : webSockets) {
            try {
                webSocket.send(message);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}