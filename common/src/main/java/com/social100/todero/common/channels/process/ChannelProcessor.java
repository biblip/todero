package com.social100.todero.common.channels.process;

import com.social100.todero.common.message.channel.ChannelType;
import com.social100.todero.common.message.channel.IPayload;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;

public class ChannelProcessor {
    private final Map<ChannelType, ChannelHandler<?>> handlerMap;

    private ChannelProcessor(Map<ChannelType, ChannelHandler<?>> handlerMap) {
        this.handlerMap = Collections.unmodifiableMap(new EnumMap<>(handlerMap));
    }

    /**
     * Processes a single channel with the given payload.
     *
     * @param channelType the type of channel to process
     * @param payload     the payload to process, must extend IPayload
     * @param <T>         the type of the payload
     */
    @SuppressWarnings("unchecked")
    public <T extends IPayload> void processChannel(ChannelType channelType, T payload) {
        ChannelHandler<?> rawHandler = handlerMap.get(channelType);
        if (rawHandler != null) {
            try {
                ChannelHandler<T> handler = (ChannelHandler<T>) rawHandler;
                handler.process(payload);
            } catch (ClassCastException e) {
                System.err.println("Handler type mismatch for ChannelType: " + channelType + ", Error: " + e.getMessage());
                e.printStackTrace();
            }
        } else {
            System.out.println("No handler registered for ChannelType: " + channelType);
        }
    }

    /**
     * Processes all messages in the given map.
     *
     * @param messages the map of channel types to payloads to process
     */
    public void processAllMessages(Map<ChannelType, ? extends IPayload> messages) {
        if (messages == null) {
            throw new IllegalArgumentException("Messages map cannot be null");
        }
        for (Map.Entry<ChannelType, ? extends IPayload> entry : messages.entrySet()) {
            ChannelType channelType = entry.getKey();
            IPayload payload = entry.getValue();
            if (channelType == null || payload == null) {
                System.err.println("Skipping null channelType or payload");
                continue;
            }
            processChannel(channelType, payload);
        }
    }

    /**
     * Creates a new Builder instance for ChannelProcessor.
     *
     * @return a new Builder instance
     */
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private final Map<ChannelType, ChannelHandler<?>> handlerMap = new EnumMap<>(ChannelType.class);

        /**
         * Registers a handler for a specific channel type.
         *
         * @param channelType the channel type to register
         * @param handler     the handler for the channel type
         * @param <T>         the type of the payload handled by the handler
         * @return this builder instance
         */
        public <T extends IPayload> Builder registerHandler(ChannelType channelType, ChannelHandler<T> handler) {
            if (handler == null) {
                throw new IllegalArgumentException("Handler cannot be null for ChannelType: " + channelType);
            }

            // Validate handler's payload type compatibility
            Class<? extends IPayload> expectedPayloadType = channelType.getPayloadType();
            if (!expectedPayloadType.isAssignableFrom(handler.getPayloadType())) {
                throw new IllegalArgumentException("Handler type mismatch: " +
                        "Expected " + expectedPayloadType.getName() + " but got " + handler.getPayloadType().getName());
            }

            handlerMap.put(channelType, handler);
            return this;
        }

        /**
         * Builds a ChannelProcessor with the registered handlers.
         *
         * @return a new ChannelProcessor instance
         */
        public ChannelProcessor build() {
            return new ChannelProcessor(handlerMap);
        }

        /**
         * Builds a ChannelProcessor and immediately processes the provided messages.
         *
         * @param messages the messages to process
         * @return the ChannelProcessor instance
         */
        public ChannelProcessor buildAndProcess(Map<ChannelType, ? extends IPayload> messages) {
            ChannelProcessor processor = build();
            processor.processAllMessages(messages);
            return processor;
        }
    }
}