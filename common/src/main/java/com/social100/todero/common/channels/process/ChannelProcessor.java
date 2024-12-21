package com.social100.todero.common.channels.process;

import com.social100.todero.common.message.channel.ChannelType;
import com.social100.todero.common.message.channel.IPayload;

import java.util.EnumMap;
import java.util.Map;

public class ChannelProcessor {
    private final Map<ChannelType, ChannelHandler<?>> handlerMap;

    private ChannelProcessor(Map<ChannelType, ChannelHandler<?>> handlerMap) {
        this.handlerMap = handlerMap;
    }

    @SuppressWarnings("unchecked")
    public <T extends IPayload> void processChannel(ChannelType channelType, T payload) {
        ChannelHandler<?> rawHandler = handlerMap.get(channelType);
        if (rawHandler != null) {
            try {
                ChannelHandler<T> handler = (ChannelHandler<T>) rawHandler;
                handler.process(payload);
            } catch (ClassCastException e) {
                System.out.println("Handler type mismatch for ChannelType: " + channelType);
            }
        } else {
            System.out.println("No handler registered for ChannelType: " + channelType);
        }
    }

    public void processAllMessages(Map<ChannelType, ? extends IPayload> messages) {
        for (Map.Entry<ChannelType, ? extends IPayload> entry : messages.entrySet()) {
            ChannelType channelType = entry.getKey();
            IPayload payload = entry.getValue();
            processChannel(channelType, payload);
        }
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private final Map<ChannelType, ChannelHandler<?>> handlerMap = new EnumMap<>(ChannelType.class);

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

        public ChannelProcessor build() {
            return new ChannelProcessor(handlerMap);
        }

        public ChannelProcessor buildAndProcess(Map<ChannelType, ? extends IPayload> messages) {
            ChannelProcessor processor = build();
            processor.processAllMessages(messages);
            return processor;
        }
    }
}
