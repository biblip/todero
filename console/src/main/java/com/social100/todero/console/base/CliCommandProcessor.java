package com.social100.todero.console.base;

import com.social100.todero.common.config.AppConfig;
import com.social100.todero.common.message.MessageContainer;
import com.social100.todero.common.message.channel.ChannelMessage;
import com.social100.todero.common.message.channel.ChannelType;
import com.social100.todero.common.message.channel.impl.PublicDataPayload;
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
            this.commandManager = new CliCommandManager(this.appConfig, (eventName, message) -> {
                System.out.println(eventName + " --> " + message);
                bridge.writeAsync(message.getBytes());
            });
        } else {
            throw new RuntimeException("CommandManager already created");
        }
    }

    @Override
    public boolean process(MessageContainer messageContainer) {
        return commandManager.process(messageContainer);
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
        // TODO: this data contains already a MessageContainer?
        String line = new String(data);
        System.out.println("TODO: this data contains already a MessageContainer?");
        System.out.println(line);
        MessageContainer messageContainer = MessageContainer.builder()
                .addChannelMessage(ChannelMessage.builder()
                        .channel(ChannelType.PUBLIC_DATA)
                        .payload(PublicDataPayload.builder()
                                .message(line)
                                .build())
                        .build())
                .build();
        process(messageContainer); // Process the received data as if it were passed to `process(String line)`
    }
}