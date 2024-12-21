package com.social100.todero.protocol.core;

import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class ReceiveMessageCallback {
    final private Consumer<ProtocolFrameManager.FrameMessage> consumer;
    final private BiConsumer<ProtocolFrameManager.FrameMessage, ResponderRegistry.Responder> bi_consumer;

    public ReceiveMessageCallback(Consumer<ProtocolFrameManager.FrameMessage> consumer) {
        this.consumer = consumer;
        this.bi_consumer = null;
    }

    public ReceiveMessageCallback(BiConsumer<ProtocolFrameManager.FrameMessage, ResponderRegistry.Responder> consumer) {
        this.consumer = null;
        this.bi_consumer = consumer;
    }

    public void consume(ProtocolFrameManager.FrameMessage message) {
        if (consumer != null) {
            consumer.accept(message);
        } else {
            throw new IllegalStateException("Consumer is not set.");
        }
    }

    public void consume(ProtocolFrameManager.FrameMessage message, ResponderRegistry.Responder responder) {
        if (bi_consumer != null) {
            bi_consumer.accept(message, responder);
        } else {
            throw new IllegalStateException("Consumer is not set.");
        }
    }
}
