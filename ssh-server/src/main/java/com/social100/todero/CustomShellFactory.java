package com.social100.todero;

import org.apache.sshd.server.Environment;
import org.apache.sshd.server.ExitCallback;
import org.apache.sshd.server.channel.ChannelSession;
import org.apache.sshd.server.command.Command;
import org.apache.sshd.server.shell.ShellFactory;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class CustomShellFactory implements ShellFactory {
    private final ReceiveSshMessageCallback receiveSshMessageCallback;

    public CustomShellFactory(ReceiveSshMessageCallback receiveSshMessageCallback) {
        this.receiveSshMessageCallback = receiveSshMessageCallback;
    }

    @Override
    public Command createShell(ChannelSession channel) throws IOException {
        return new EchoShell(receiveSshMessageCallback);
    }

    private static class EchoShell implements Command, Runnable {
        private final ReceiveSshMessageCallback receiveSshMessageCallback;

        private InputStream in;
        private OutputStream out;
        private OutputStream err;
        private ExitCallback exitCallback;
        private final ExecutorService executor = Executors.newSingleThreadExecutor();

        public EchoShell(ReceiveSshMessageCallback receiveSshMessageCallback) {
            this.receiveSshMessageCallback = receiveSshMessageCallback;
        }

        @Override
        public void setInputStream(InputStream in) {
            this.in = in;
        }

        @Override
        public void setOutputStream(OutputStream out) {
            this.out = out;
        }

        @Override
        public void setErrorStream(OutputStream err) {
            this.err = err;
        }

        @Override
        public void setExitCallback(ExitCallback callback) {
            this.exitCallback = callback;
        }

        @Override
        public void start(ChannelSession channel, Environment env) throws IOException {
            executor.submit(this);
        }

        @Override
        public void destroy(ChannelSession channel) throws Exception {
            executor.shutdownNow();
        }

        @Override
        public void run() {
            try {
                out.write("Character echo shell. Type 'exit' to quit.\r\n".getBytes());
                out.flush();

                ByteArrayOutputStream buffer = new ByteArrayOutputStream();
                int ch;
                while ((ch = in.read()) != -1) {
                    // Echo back character
                    out.write(ch);
                    out.flush();

                    if (ch == '\r' || ch == '\n') {
                        String line = buffer.toString().trim();

                        if (!line.isEmpty()) {
                            receiveSshMessageCallback.consume(line);
                        }

                        if ("exit".equalsIgnoreCase(line)) {
                            break;
                        }
                        out.write(("\r\nYou typed: " + line + "\r\n").getBytes());
                        out.flush();
                        buffer.reset();
                    } else {
                        buffer.write(ch);
                    }
                }

            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                if (exitCallback != null) {
                    exitCallback.onExit(0);
                }
            }
        }
    }
}