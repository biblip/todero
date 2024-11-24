package com.social100.protocol.core;

import java.net.InetSocketAddress;
import java.util.concurrent.atomic.AtomicInteger;

public class Responder {
    private final InetSocketAddress destination;
    private final ProtocolEngine engine;
    private final AtomicInteger packetIdGenerator = new AtomicInteger(1);

    public Responder(InetSocketAddress destination, ProtocolEngine engine) {
        this.destination = destination;
        this.engine = engine;
    }

    public Integer sendMessage(String message, boolean ackRequested) throws Exception {
        return engine.sendMessage(destination, message, ackRequested);
    }
}
