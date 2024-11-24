package com.social100.protocol.core;

import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class ReceiveMessageCallback {
    final private Consumer<ProtocolMessage> consumer;
    final private BiConsumer<ProtocolMessage, Responder> bi_consumer;

    public ReceiveMessageCallback(Consumer<ProtocolMessage> consumer) {
        this.consumer = consumer;
        this.bi_consumer = null;
    }

    public ReceiveMessageCallback(BiConsumer<ProtocolMessage, Responder> consumer) {
        this.consumer = null;
        this.bi_consumer = consumer;
    }

    public void consume(ProtocolMessage message) {
        if (consumer != null) {
            consumer.accept(message);
        } else {
            throw new IllegalStateException("Consumer is not set.");
        }
    }

    public void consume(ProtocolMessage message, Responder responder) {
        if (bi_consumer != null) {
            bi_consumer.accept(message, responder);
        } else {
            throw new IllegalStateException("Consumer is not set.");
        }
    }
}
