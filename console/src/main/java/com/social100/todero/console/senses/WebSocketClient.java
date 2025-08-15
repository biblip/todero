package com.social100.todero.console.senses;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.BiConsumer;

public class WebSocketClient implements WebSocket.Listener {

  // ==== Reconnect backoff ====
  private static final int MAX_RECONNECT_DELAY_SECONDS = 30;
  private static final int BASE_DELAY_SECONDS = 2;
  private static final Random RANDOM = new Random();

  // ==== Watchdog/health ====
  private static final Duration WATCHDOG_PERIOD = Duration.ofSeconds(5);
  private static final Duration IDLE_PING_AFTER = Duration.ofSeconds(120); // if no traffic -> ping
  private static final Duration PING_TIMEOUT = Duration.ofSeconds(10);    // no pong -> assume dead

  private static final Map<String, BiConsumer<String, String>> targetList = new ConcurrentHashMap<>();

  private final URI uri;
  private final HttpClient httpClient;
  private final AtomicBoolean shutdown = new AtomicBoolean(false);
  private final ScheduledExecutorService watchdog =
      Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "ws-watchdog");
        t.setDaemon(true);
        return t;
      });

  private volatile WebSocket webSocket;
  private final AtomicLong lastActivityNanos = new AtomicLong(System.nanoTime());
  private volatile boolean pingInFlight = false;
  private volatile long pingSentAtNanos = 0L;

  public WebSocketClient(String uri) {
    this.uri = URI.create(uri);
    this.httpClient = HttpClient.newHttpClient();
  }

  // ==== Public API ====
  public void start() {
    new Thread(this::connectWithRetry, "ws-connect").start();
    watchdog.scheduleAtFixedRate(this::watchdogTick,
        WATCHDOG_PERIOD.toSeconds(), WATCHDOG_PERIOD.toSeconds(), TimeUnit.SECONDS);
  }

  public void shutdown() {
    shutdown.set(true);
    try {
      watchdog.shutdownNow();
    } catch (Exception ignored) {}
    if (webSocket != null) {
      log("Shutting down: aborting current WebSocket.");
      webSocket.abort();
    }
  }

  public void register(String id, BiConsumer<String, String> consumer) {
    targetList.put(id, consumer);
  }

  // ==== Connection management ====
  private void connectWithRetry() {
    int retryAttempts = 0;

    while (!shutdown.get()) {
      try {
        log("Attempting connection to " + uri);
        CompletableFuture<WebSocket> future = httpClient.newWebSocketBuilder().buildAsync(uri, this);
        webSocket = future.join();

        // reset state on success
        retryAttempts = 0;
        pingInFlight = false;
        lastActivityNanos.set(System.nanoTime());

        log("Connected to WebSocket.");
        break; // exit retry loop after a successful connection

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
      new Thread(this::connectWithRetry, "ws-reconnect").start();
    }
  }

  private void forceRecover(String reason) {
    // Called by watchdog when we think the connection is unhealthy
    WebSocket ws = this.webSocket;
    log("[WATCHDOG] Forcing recovery: " + reason);
    if (ws != null) {
      try { ws.abort(); } catch (Exception ignored) {}
      this.webSocket = null;
    }
    pingInFlight = false;
    reconnectAsync();
  }

  private void sleepSeconds(int seconds) {
    try {
      TimeUnit.SECONDS.sleep(seconds);
    } catch (InterruptedException ignored) {}
  }

  // ==== Watchdog logic ====
  private void watchdogTick() {
    if (shutdown.get()) return;

    WebSocket ws = this.webSocket;
    long now = System.nanoTime();

    if (ws == null) {
      log("[WATCHDOG] No active WebSocket. Triggering recovery.");
      forceRecover("No active WebSocket instance");
      return;
    }

    // If the underlying streams are closed, recover immediately.
    if (ws.isInputClosed() || ws.isOutputClosed()) {
      log("[WATCHDOG] Detected closed I/O (inputClosed=" + ws.isInputClosed() +
          ", outputClosed=" + ws.isOutputClosed() + ").");
      forceRecover("WebSocket I/O closed");
      return;
    }

    long sinceLastActivity = Duration.ofNanos(now - lastActivityNanos.get()).getSeconds();

    // If idle and no ping in flight, send a ping
    if (!pingInFlight && sinceLastActivity >= IDLE_PING_AFTER.getSeconds()) {
      try {
        log("[WATCHDOG] Idle " + sinceLastActivity + "s. Sending ping...");
        ws.sendPing(ByteBuffer.wrap("healthcheck".getBytes(StandardCharsets.UTF_8)));
        pingInFlight = true;
        pingSentAtNanos = now;
      } catch (Exception e) {
        log("[WATCHDOG] Failed to send ping: " + e.getMessage());
        forceRecover("Ping send failed");
      }
    }

    // If a ping is in flight and we exceeded timeout, consider it dead
    if (pingInFlight &&
        Duration.ofNanos(now - pingSentAtNanos).compareTo(PING_TIMEOUT) > 0) {
      log("[WATCHDOG] Ping timeout after " + PING_TIMEOUT.getSeconds() + "s. Declaring dead.");
      forceRecover("Ping timeout");
    }
  }

  // ==== WebSocket.Listener callbacks ====
  @Override
  public void onOpen(WebSocket webSocket) {
    lastActivityNanos.set(System.nanoTime());
    pingInFlight = false;
    log("WebSocket connection established.");
    WebSocket.Listener.super.onOpen(webSocket);
  }

  @Override
  public CompletionStage<?> onText(WebSocket webSocket, CharSequence data, boolean last) {
    lastActivityNanos.set(System.nanoTime());

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
      String targetIndex = json.get("agent").asText() + ";" +
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
  public CompletionStage<?> onBinary(WebSocket webSocket, ByteBuffer data, boolean last) {
    lastActivityNanos.set(System.nanoTime());
    return WebSocket.Listener.super.onBinary(webSocket, data, last);
  }

  @Override
  public CompletionStage<?> onPing(WebSocket webSocket, ByteBuffer message) {
    lastActivityNanos.set(System.nanoTime());
    log("Received PING from server. Responding with PONG.");
    // Echo back as PONG
    try {
      webSocket.sendPong(message);
    } catch (Exception e) {
      log("Failed to send PONG: " + e.getMessage());
    }
    return WebSocket.Listener.super.onPing(webSocket, message);
  }

  @Override
  public CompletionStage<?> onPong(WebSocket webSocket, ByteBuffer message) {
    lastActivityNanos.set(System.nanoTime());
    if (pingInFlight) {
      pingInFlight = false;
      log("Received PONG (watchdog ping acknowledged). Connection healthy.");
    } else {
      log("Received unsolicited PONG.");
    }
    return WebSocket.Listener.super.onPong(webSocket, message);
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

  // ==== Logging ====
  private void log(String msg) {
    System.out.println("[WebSocketClient] " + msg);
  }
}