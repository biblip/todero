package com.social100.todero.protocol.pipeline;

public interface PipelineStage {
    /**
     * Processes a message before sending.
     * @param message the original message
     * @param destinationId the identifier for the destination (e.g., peer/session id)
     * @return the processed message
     */
    byte[] processToSend(byte[] message, String destinationId);

    default byte[] processToSend(byte[] message) {
        return processToSend(message, null);
    }

    /**
     * Processes a message after receiving.
     * @param message the received (raw) message
     * @param sourceId the identifier for the source (e.g., peer/session id)
     * @return the processed message
     */
    byte[] processToReceive(byte[] message, String sourceId);

    default byte[] processToReceive(byte[] message) {
        return processToReceive(message, null);
    }
}

