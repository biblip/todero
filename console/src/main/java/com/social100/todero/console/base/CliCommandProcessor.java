package com.social100.todero.console.base;

import com.social100.todero.common.channels.EventChannel;
import com.social100.todero.common.config.AppConfig;
import com.social100.todero.common.config.ServerType;
import com.social100.todero.common.message.MessageContainer;

public class CliCommandProcessor implements CommandProcessor {
    private final AppConfig appConfig;
    private final EventChannel.EventListener eventListener;
    private CliCommandManager commandManager;
    private final ServerType type;

    public CliCommandProcessor(AppConfig appConfig, ServerType type, EventChannel.EventListener eventListener) {
        this.appConfig = appConfig;
        this.eventListener = eventListener;
        this.type = type;
    }

    @Override
    public void open() {
        if (this.commandManager == null) {
            this.commandManager = new CliCommandManager(this.appConfig, type, this.eventListener);
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
    }

    @Override
    public CliCommandManager getCommandManager() {
        return this.commandManager;
    }

    /**
     * Handle incoming data from the bridge.
     *
     * @param data The received data as a byte array.
     */
    /*@Override
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
    }*/
}