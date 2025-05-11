package com.social100.todero.protocol.transport;

import lombok.Getter;

import java.net.InetSocketAddress;

@Getter
class MessageQueueEntry {
    private final int packetId;
    private final InetSocketAddress destination;
    private final byte[] serializedMessage;
    private final boolean ackRequested;
    private long time;
    private byte retry;

    public MessageQueueEntry(InetSocketAddress destination, int packetId, byte[] serializedMessage, boolean ackRequested) {
        this.packetId = packetId;
        this.destination = destination;
        this.serializedMessage = serializedMessage;
        this.ackRequested = ackRequested;
        this.time = System.currentTimeMillis();
        this.retry = 0;
    }

    public boolean retry() {
        if (this.retry > 5) return false;
        this.time = System.currentTimeMillis();
        this.retry += 1;
        return true;
    }

    public long getTime() {
        return time;
    }
}
