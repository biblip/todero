package com.social100.todero.protocol.pipeline;

public interface PipelineStage {
    /**
     * Processes a message before sending.
     * @param message the original message
     * @return the processed message
     */
    byte[] processToSend(byte[] message);

    /**
     * Processes a message after receiving.
     * @param message the received (raw) message
     * @return the processed message
     */
    byte[] processToReceive(byte[] message);
}

