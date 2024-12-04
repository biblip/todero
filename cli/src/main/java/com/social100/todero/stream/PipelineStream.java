package com.social100.todero.stream;

import java.io.IOException;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

public class PipelineStream {
    private final PipedOutputStream outputStream;
    private final PipedInputStream inputStream;
    private final ExecutorService executor;
    private final AtomicBoolean closed;

    public PipelineStream() throws IOException {
        this.outputStream = new PipedOutputStream();
        this.inputStream = new PipedInputStream(outputStream);
        this.executor = Executors.newSingleThreadExecutor();
        this.closed = new AtomicBoolean(false);
    }

    // Asynchronously write data to the output stream
    public void writeAsync(byte[] data) {
        if (closed.get()) {
            throw new IllegalStateException("Stream is already closed!");
        }

        synchronized (outputStream) {
            try {
                if (closed.get()) {
                    // Stream was closed mid-task, skip writing
                    return;
                }
                outputStream.write(data);
                outputStream.flush();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    // Asynchronously read data from the input stream
    public void readAsync(ByteDataHandler handler) {
        executor.submit(() -> {
            byte[] buffer = new byte[1024]; // Buffer size for reading
            try {
                int bytesRead;
                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    byte[] data = new byte[bytesRead];
                    System.arraycopy(buffer, 0, data, 0, bytesRead);
                    handler.handle(data); // Pass data to the handler
                }
            } catch (IOException e) {
                if (!closed.get()) {
                    e.printStackTrace();
                }
            }
        });
    }

    // Close the streams and wait for all tasks to complete
    public void close() {
        synchronized (outputStream) {
            if (closed.compareAndSet(false, true)) {
                try {
                    // Shut down the executor and wait for pending tasks
                    executor.shutdown();
                    if (!executor.isTerminated()) {
                        executor.awaitTermination(1, java.util.concurrent.TimeUnit.SECONDS);
                    }

                    // Close the streams after all tasks are finished
                    outputStream.close();
                    inputStream.close();
                } catch (IOException | InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    // Functional interface for handling received byte data
    @FunctionalInterface
    public interface ByteDataHandler {
        void handle(byte[] data);
    }
}