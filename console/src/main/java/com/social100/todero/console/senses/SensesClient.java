package com.social100.todero.console.senses;

import com.social100.processor.beans.Scope;
import com.social100.processor.beans.Service;

import java.util.function.Consumer;

@Service(scope = Scope.SINGLETON)
public class SensesClient {
  final private WebSocketClient client;

  public SensesClient() {
    client = new WebSocketClient("ws://localhost:5353/ws");

    Runtime.getRuntime().addShutdownHook(new Thread(() -> {
      System.out.println("[!] Interrupted by user");
      client.shutdown();
    }));
  }

  public void start() {
    client.start();
  }

  public void register(String id, Consumer<String> consumer) {
    client.register(id, consumer);
  }
}
