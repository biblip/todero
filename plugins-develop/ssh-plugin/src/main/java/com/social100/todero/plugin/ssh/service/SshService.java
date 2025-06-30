package com.social100.todero.plugin.ssh.service;

import com.social100.todero.processor.EventDefinition;
import net.schmizz.sshj.SSHClient;
import net.schmizz.sshj.connection.channel.direct.Session;
import net.schmizz.sshj.transport.verification.OpenSSHKnownHosts;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

public class SshService {
  public final static String MAIN_GROUP = "Main";

  private final ConcurrentHashMap<String, SSHClient> connections = new ConcurrentHashMap<>();

  public void connectWithKey(String id, String host, String user, String pemPath) throws IOException {
    if (connections.containsKey(id)) {
      throw new IllegalStateException("Connection with ID already exists: " + id);
    }

    File pemFile = new File(pemPath);
    if (!pemFile.exists() || !pemFile.isFile() || !pemFile.canRead()) {
      throw new IOException("PEM file not found or not readable: " + pemPath);
    }

    SSHClient ssh = new SSHClient();
    ssh.addHostKeyVerifier(new OpenSSHKnownHosts(new File(System.getProperty("user.home"), ".ssh/known_hosts")));
    ssh.connect(host);
    ssh.authPublickey(user, pemPath);
    connections.put(id, ssh);
  }

  public void connectWithPassword(String id, String host, String user, String password) throws IOException {
    if (connections.containsKey(id)) {
      throw new IllegalStateException("Connection with ID already exists: " + id);
    }

    SSHClient ssh = new SSHClient();
    ssh.addHostKeyVerifier(new OpenSSHKnownHosts(new File(System.getProperty("user.home"), ".ssh/known_hosts")));
    ssh.connect(host);
    user = user.trim().replaceAll("^['\"]|['\"]$", "");
    ssh.authPassword(user, password);
    connections.put(id, ssh);
  }

  public void uploadFile(String id, String localPath, String remotePath) throws IOException {
    SSHClient ssh = getConnection(id);
    ssh.newSFTPClient().put(localPath, remotePath);
  }

  public void deleteRemoteFile(String id, String remotePath) throws IOException {
    SSHClient ssh = getConnection(id);
    ssh.newSFTPClient().rm(remotePath);
  }

  public void disconnect(String id) throws IOException {
    SSHClient ssh = getConnection(id);
    ssh.disconnect();
    connections.remove(id);
  }

  public boolean isConnected(String id) {
    return connections.containsKey(id);
  }

  private SSHClient getConnection(String id) {
    SSHClient ssh = connections.get(id);
    if (ssh == null) {
      throw new IllegalStateException("No connection with ID: " + id);
    }
    return ssh;
  }

  public CompletableFuture<CommandResult> runCommand(
      String id,
      String command,
      Consumer<String> stdoutConsumer,
      Consumer<String> stderrConsumer,
      Duration timeout
  ) {
    Duration effectiveTimeout = (timeout != null) ? timeout : Duration.ofSeconds(30);
    SSHClient ssh = getConnection(id);
    CompletableFuture<CommandResult> resultFuture = new CompletableFuture<>();
    ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

    try {
      Session session = ssh.startSession();

      Session.Command cmd = session.exec(command);

      StringBuilder stdoutBuilder = new StringBuilder();
      StringBuilder stderrBuilder = new StringBuilder();

      Runnable timeoutTask = () -> {
        try {
          cmd.close(); // Gracefully close the session if idle
          resultFuture.complete(new CommandResult(-1, stdoutBuilder.toString(), "Timeout reached."));
        } catch (IOException e) {
          resultFuture.completeExceptionally(e);
        }
      };

      ScheduledFuture<?>[] timeoutRef = new ScheduledFuture<?>[1];
      Runnable resetTimer = () -> {
        if (timeoutRef[0] != null) timeoutRef[0].cancel(false);
        timeoutRef[0] = scheduler.schedule(timeoutTask, effectiveTimeout.toMillis(), TimeUnit.MILLISECONDS);
      };

      resetTimer.run();

      CompletableFuture<Void> stdoutFuture = CompletableFuture.runAsync(() -> {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(cmd.getInputStream()))) {
          String line;
          while ((line = reader.readLine()) != null) {
            stdoutConsumer.accept(line);
            stdoutBuilder.append(line).append("\n");
            resetTimer.run();
          }
        } catch (IOException ignored) {}
      });

      CompletableFuture<Void> stderrFuture = CompletableFuture.runAsync(() -> {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(cmd.getErrorStream()))) {
          String line;
          while ((line = reader.readLine()) != null) {
            stderrConsumer.accept(line);
            stderrBuilder.append(line).append("\n");
            resetTimer.run();
          }
        } catch (IOException ignored) {}
      });

      CompletableFuture.allOf(stdoutFuture, stderrFuture).thenRunAsync(() -> {
        try {
          int exitCode = cmd.getExitStatus();
          resultFuture.complete(new CommandResult(exitCode, stdoutBuilder.toString(), stderrBuilder.toString()));
        } catch (Exception e) {
          resultFuture.completeExceptionally(e);
        } finally {
          scheduler.shutdownNow();
          try {
            session.close();
          } catch (IOException ignored) {}
        }
      });

    } catch (IOException e) {
      scheduler.shutdownNow();
      resultFuture.completeExceptionally(e);
    }

    return resultFuture;
  }

  public enum SshEvents implements EventDefinition {
    EVENT1("A event 1" ),
    EVENT2("A event 2" ),
    EVENT3("A event 3" );

    private final String description;

    SshEvents(String description) {
      this.description = description;
    }

    @Override
    public String getDescription() {
      return description;
    }
  }
}
