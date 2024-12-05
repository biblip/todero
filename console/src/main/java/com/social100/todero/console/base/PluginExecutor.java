package com.social100.todero.console.base;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

public class PluginExecutor {

    private final ExecutorService executorService;

    public PluginExecutor(int numberOfThreads) {
        // Initialize the thread pool with a fixed number of threads
        this.executorService = Executors.newFixedThreadPool(numberOfThreads);
    }

    public Future<String> executePluginTask(Callable<String> pluginTask) {
        // Submit a plugin task for execution
        return executorService.submit(pluginTask);
    }

    public void shutdown() {
        // Initiate a graceful shutdown
        executorService.shutdown();
        try {
            // Wait for existing tasks to terminate
            if (!executorService.awaitTermination(60, TimeUnit.SECONDS)) {
                // Cancel currently executing tasks and shut down immediately
                executorService.shutdownNow();
                // Wait again for tasks to respond to being cancelled
                if (!executorService.awaitTermination(60, TimeUnit.SECONDS)) {
                    System.err.println("PluginExecutor did not terminate.");
                }
            }
        } catch (InterruptedException ie) {
            // Preserve interrupt status
            Thread.currentThread().interrupt();
            // Cancel if current thread also interrupted
            executorService.shutdownNow();
        }
    }
}
