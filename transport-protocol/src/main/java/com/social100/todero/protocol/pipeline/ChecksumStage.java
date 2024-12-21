package com.social100.todero.protocol.pipeline;

import java.nio.ByteBuffer;

public class ChecksumStage implements PipelineStage {

    @Override
    public byte[] processToSend(byte[] message) {
        if (message == null) {
            message = new byte[0];
        }

        int checksum = calculateChecksum(message);
        // Create a new byte[] = original data + 4 bytes for checksum
        ByteBuffer buffer = ByteBuffer.allocate(message.length + 4);
        buffer.put(message);
        buffer.putInt(checksum);
        return buffer.array();
    }

    @Override
    public byte[] processToReceive(byte[] message) {
        if (message == null || message.length < 4) {
            throw new IllegalArgumentException("Invalid or empty message");
        }

        // Separate the payload from the checksum
        int payloadLength = message.length - 4;
        ByteBuffer buffer = ByteBuffer.wrap(message);

        // Read everything but the last 4 bytes as the payload
        byte[] payload = new byte[payloadLength];
        buffer.get(payload);
        // Last 4 bytes is the transmitted checksum
        int transmittedChecksum = buffer.getInt();

        // Recompute the checksum of payload
        int computedChecksum = calculateChecksum(payload);
        if (computedChecksum != transmittedChecksum) {
            throw new IllegalStateException("Checksum verification failed");
        }

        return payload;
    }

    private int calculateChecksum(byte[] data) {
        // Simple checksum by summing all bytes (unsigned)
        int sum = 0;
        for (byte b : data) {
            sum += (b & 0xFF);
        }
        return sum;
    }
}
