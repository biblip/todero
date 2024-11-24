package com.social100.protocol.pipeline;

public class ChecksumStage implements PipelineStage {
    @Override
    public String processToSend(String message) {
        int checksum = calculateChecksum(message);
        return message + "-" + checksum;
    }

    @Override
    public String processToReceive(String message) {
        String[] parts = message.split("-");
        if (parts.length != 2) {
            throw new IllegalArgumentException("Invalid message format");
        }

        String data = parts[0];
        int checksum = Integer.parseInt(parts[1]);
        if (calculateChecksum(data) != checksum) {
            throw new IllegalStateException("Checksum verification failed");
        }

        return data;
    }

    private int calculateChecksum(String data) {
        return data.chars().sum(); // Simple checksum for demonstration
    }
}

