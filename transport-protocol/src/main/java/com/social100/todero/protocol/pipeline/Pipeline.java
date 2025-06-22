package com.social100.todero.protocol.pipeline;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

public class Pipeline {
    private final List<PipelineStage> stages = new ArrayList<>();
    private final List<PipelineStage> reversedStages = new ArrayList<>();

    /**
     * Adds a stage to the pipeline.
     * @param stage the stage to add
     */
    public void addStage(PipelineStage stage) {
        stages.add(stage);
        reversedStages.add(0, stage); // Add at the beginning to maintain reverse order
    }

    /**
     * Processes the message through all stages for sending (forward order).
     * @param message the original message
     * @return the fully processed message
     */
    public byte[] processToSend(byte[] message, String destinationId) {
        byte[] processedMessage = message;
        for (PipelineStage stage : stages) {
            processedMessage = stage.processToSend(processedMessage, destinationId);
        }
        return processedMessage;
    }

    /**
     * Processes the message through all stages for receiving (reverse order).
     * @param message the raw received message
     * @return the fully processed message
     */
    public byte[] processToReceive(byte[] message, String sourceId) {
        byte[] processedMessage = message;
        for (PipelineStage stage : reversedStages) {
            processedMessage = stage.processToReceive(processedMessage, sourceId);
        }
        return processedMessage;
    }

    /**
     * Returns an unmodifiable list of stages.
     * @return all pipeline stages
     */
    public List<PipelineStage> getStages() {
        return Collections.unmodifiableList(stages);
    }

    /**
     * Updates a stage of the given type using the provided function.
     * Useful for injecting keys into encryption stages, etc.
     */
    @SuppressWarnings("unchecked")
    public <T extends PipelineStage> void updateStage(Class<T> type, Consumer<T> updater) {
        for (PipelineStage stage : stages) {
            if (type.isInstance(stage)) {
                updater.accept((T) stage);
            }
        }
    }
}
