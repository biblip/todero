package com.social100.todero.console.base;

import com.social100.todero.stream.PipelineStreamBridge;

public interface CommandProcessor {
    void open();
    void process(String line);
    void close();
    CliCommandManager getCommandManager();
    PipelineStreamBridge.PipelineStreamBridgeShadow getBridge();
    void handleIncomingData(byte[] data);
}
