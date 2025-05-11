package com.social100.todero;

import java.util.function.Consumer;

public class ReceiveSshMessageCallback {
    final private Consumer<String> consumer;

    public ReceiveSshMessageCallback(Consumer<String> consumer) {
        this.consumer = consumer;
    }

    public void consume(String message) {
        if (consumer != null) {
            consumer.accept(message);
        } else {
            throw new IllegalStateException("Consumer is not set.");
        }
    }
}
