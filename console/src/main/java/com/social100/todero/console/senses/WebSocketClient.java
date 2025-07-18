package com.social100.todero.console.senses;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

public class WebSocketClient implements WebSocket.Listener {

  private static final int MAX_RECONNECT_DELAY_SECONDS = 30;
  private static final int BASE_DELAY_SECONDS = 2;
  private static final Random RANDOM = new Random();
  private static final Map<String, Consumer<String>> targetList = new ConcurrentHashMap<>();

  private final URI uri;
  private final HttpClient httpClient;
  private final AtomicBoolean shutdown = new AtomicBoolean(false);
  private WebSocket webSocket;

  public WebSocketClient(String uri) {
    this.uri = URI.create(uri);
    this.httpClient = HttpClient.newHttpClient();
  }

  public void start() {
    new Thread(this::connectWithRetry).start();
  }

  public void shutdown() {
    shutdown.set(true);
    if (webSocket != null) {
      webSocket.abort();
    }
  }

  private void connectWithRetry() {
    while (!shutdown.get()) {
      try {
        log("Connecting to " + uri);
        CompletableFuture<WebSocket> future = httpClient.newWebSocketBuilder()
            .buildAsync(uri, this);
        webSocket = future.join();

        // Wait until closed
        while (!shutdown.get()) {
          Thread.sleep(1000);
        }
        break;

      } catch (Exception e) {
        log("Connection failed: " + e.getMessage());
      }

      int expDelay = BASE_DELAY_SECONDS * (1 << RANDOM.nextInt(5));
      double jitter = RANDOM.nextDouble();
      int delay = Math.min(MAX_RECONNECT_DELAY_SECONDS, expDelay + (int) jitter);
      log("Reconnecting in " + delay + " seconds...");
      sleepSeconds(delay);
    }
  }

  private void sleepSeconds(int seconds) {
    try {
      TimeUnit.SECONDS.sleep(seconds);
    } catch (InterruptedException ignored) {
    }
  }

  @Override
  public void onOpen(WebSocket webSocket) {
    log("WebSocket connected.");
    WebSocket.Listener.super.onOpen(webSocket);
  }

  @Override
  public CompletionStage<?> onText(WebSocket webSocket, CharSequence data, boolean last) {
    log("Received message: " + data);
    // TODO: dispatch based on message type
    Consumer<String> consumer = targetList.get("valorlargoyidentificabledeformaunica");
    if (consumer != null) {
      consumer.accept(data.toString());
    }
    return WebSocket.Listener.super.onText(webSocket, data, last);
  }

  @Override
  public CompletionStage<?> onClose(WebSocket webSocket, int statusCode, String reason) {
    log("Connection closed: " + statusCode + " - " + reason);
    return WebSocket.Listener.super.onClose(webSocket, statusCode, reason);
  }

  @Override
  public void onError(WebSocket webSocket, Throwable error) {
    log("WebSocket error: " + error.getMessage());
    WebSocket.Listener.super.onError(webSocket, error);
  }

  private void log(String msg) {
    System.out.println("[WebSocketClient] " + msg);
  }

  public void register(String id, Consumer<String> consumer) {
    targetList.put(id, consumer);
  }
}