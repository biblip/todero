package com.social100.todero.console.senses;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

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
import java.util.function.BiConsumer;

public class WebSocketClient implements WebSocket.Listener {

  private static final int MAX_RECONNECT_DELAY_SECONDS = 30;
  private static final int BASE_DELAY_SECONDS = 2;
  private static final Random RANDOM = new Random();
  private static final Map<String, BiConsumer<String, String>> targetList = new ConcurrentHashMap<>();

  private final URI uri;
  private final HttpClient httpClient;
  private final AtomicBoolean shutdown = new AtomicBoolean(false);
  private volatile WebSocket webSocket;

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
    int retryAttempts = 0;

    while (!shutdown.get()) {
      try {
        log("Attempting connection to " + uri);
        CompletableFuture<WebSocket> future = httpClient.newWebSocketBuilder()
            .buildAsync(uri, this);
        webSocket = future.join();

        retryAttempts = 0; // Reset on successful connection
        log("Connected to WebSocket.");
        break; // exit retry loop

      } catch (Exception e) {
        log("Connection failed: " + e.getMessage());
        retryAttempts++;
      }

      int expDelay = BASE_DELAY_SECONDS * (1 << Math.min(retryAttempts, 5));
      int jitter = RANDOM.nextInt(BASE_DELAY_SECONDS);
      int delay = Math.min(MAX_RECONNECT_DELAY_SECONDS, expDelay + jitter);
      log("Reconnecting in " + delay + " seconds...");
      sleepSeconds(delay);
    }
  }

  private void reconnectAsync() {
    if (!shutdown.get()) {
      log("Scheduling reconnection...");
      new Thread(this::connectWithRetry).start();
    }
  }

  private void sleepSeconds(int seconds) {
    try {
      TimeUnit.SECONDS.sleep(seconds);
    } catch (InterruptedException ignored) {}
  }

  @Override
  public void onOpen(WebSocket webSocket) {
    log("WebSocket connection established.");
    WebSocket.Listener.super.onOpen(webSocket);
  }

  @Override
  public CompletionStage<?> onText(WebSocket webSocket, CharSequence data, boolean last) {
    String dd = "{" +
        "\"jar\": \"agent-demo.jar\"," +
        "\"agent\": \"com.shellaia.verbatim.agent.dj\"," +
        "\"command\": \"process\"," +
        "\"prompt\": \"" + data + "\"" +
        "}";

    ObjectMapper mapper = new ObjectMapper();
    try {
      log("Received message: " + data);
      JsonNode json = mapper.readTree(dd);
      String targetIndex = json.get("jar").asText() + ";" +
          json.get("agent").asText() + ";" +
          json.get("command").asText();
      BiConsumer<String, String> consumer = targetList.get(targetIndex);
      if (consumer != null) {
        consumer.accept(targetIndex, data.toString());
      }
    } catch (JsonProcessingException e) {
      log("Error parsing JSON: " + e.getMessage());
    }

    return WebSocket.Listener.super.onText(webSocket, data, last);
  }

  @Override
  public CompletionStage<?> onClose(WebSocket webSocket, int statusCode, String reason) {
    log("WebSocket closed: " + statusCode + " - " + reason);
    if (!shutdown.get()) {
      reconnectAsync();
    }
    return WebSocket.Listener.super.onClose(webSocket, statusCode, reason);
  }

  @Override
  public void onError(WebSocket webSocket, Throwable error) {
    log("WebSocket error: " + error.getMessage());
    if (!shutdown.get()) {
      reconnectAsync();
    }
    WebSocket.Listener.super.onError(webSocket, error);
  }

  public void register(String id, BiConsumer<String, String> consumer) {
    targetList.put(id, consumer);
  }

  private void log(String msg) {
    System.out.println("[WebSocketClient] " + msg);
  }
}
