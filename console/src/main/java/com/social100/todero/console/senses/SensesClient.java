package com.social100.todero.console.senses;

import com.social100.processor.beans.Scope;
import com.social100.processor.beans.Service;

import java.util.function.BiConsumer;
import java.util.function.Consumer;

@Service(scope = Scope.SINGLETON)
public class SensesClient {
  final private WebSocketClient client;

  public SensesClient() throws Exception {
    String interactionServerHost = "io.shellaia.com";
    //String interactionServerHost = "localhost";
    int interactionServerPort = 4242;
    String interactionServerUrl =  "ws://" + interactionServerHost + ":" + interactionServerPort + "/ws";

    client = new WebSocketClient(interactionServerUrl);

    Runtime.getRuntime().addShutdownHook(new Thread(() -> {
      System.out.println("[!] Interrupted by user");
      client.shutdown();
    }));
  }

  public void start(Runnable onStart) {
    client.start();
    onStart.run();
  }

  public void register(String id, BiConsumer<String, String> consumer) {
    System.out.println("Senses Registering : " + id);
    client.register(id, consumer);
  }
}
