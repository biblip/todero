package com.social100.todero.protocol.pipeline;

import java.util.ArrayList;
import java.util.List;

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
    public String processToSend(String message) {
        String processedMessage = message;
        for (PipelineStage stage : stages) {
            processedMessage = stage.processToSend(processedMessage);
        }
        return processedMessage;
    }

    /**
     * Processes the message through all stages for receiving (pre-reversed order).
     * @param message the raw received message
     * @return the fully processed message
     */
    public String processToReceive(String message) {
        String processedMessage = message;
        for (PipelineStage stage : reversedStages) {
            processedMessage = stage.processToReceive(processedMessage);
        }
        return processedMessage;
    }
}

