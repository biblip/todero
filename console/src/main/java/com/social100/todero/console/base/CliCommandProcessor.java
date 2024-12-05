package com.social100.todero.console.base;

import com.social100.todero.common.config.AppConfig;
import com.social100.todero.stream.PipelineStreamBridge;

import java.io.IOException;

public class CliCommandProcessor implements CommandProcessor {
    private final AppConfig appConfig;
    private CliCommandManager commandManager;
    private final PipelineStreamBridge bridge;

    public CliCommandProcessor(AppConfig appConfig) {
        this.appConfig = appConfig;

        // Build the bridge with an internal onReceive handler
        try {
            this.bridge = new PipelineStreamBridge.Builder()
                    .onReceive(this::handleIncomingData)
                    .build();
        } catch (IOException e) {
            throw new RuntimeException("Failed to initialize PipelineStreamBridge", e);
        }
    }

    @Override
    public void open() {
        if (this.commandManager == null) {
            this.commandManager = new CliCommandManager(this.appConfig);
        } else {
            throw new RuntimeException("CommandManager already created");
        }
    }

    @Override
    public void process(String line) {
        String output = commandManager.execute(line);
        if (!output.isEmpty()) {
            // Send the output through the bridge
            bridge.writeAsync(output.getBytes());
        }
    }

    @Override
    public void close() {
        if (this.commandManager != null) {
            this.commandManager.terminate();
        }
        this.bridge.close();
    }

    @Override
    public CliCommandManager getCommandManager() {
        return this.commandManager;
    }

    /**
     * Get the PipelineStreamBridge for external communication.
     *
     * @return the PipelineStreamBridge instance
     */
    @Override
    public PipelineStreamBridge.PipelineStreamBridgeShadow getBridge() {
        return this.bridge.getBridge();
    }

    /**
     * Handle incoming data from the bridge.
     *
     * @param data The received data as a byte array.
     */
    @Override
    public void handleIncomingData(byte[] data) {
        String line = new String(data);
        process(line); // Process the received data as if it were passed to `process(String line)`
    }
}