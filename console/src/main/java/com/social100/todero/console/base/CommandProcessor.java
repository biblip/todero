package com.social100.todero.console.base;

import com.social100.todero.common.message.MessageContainer;
import com.social100.todero.stream.PipelineStreamBridge;

public interface CommandProcessor {
    void open();
    boolean process(MessageContainer messageContainer);
    void close();
    CliCommandManager getCommandManager();
    PipelineStreamBridge.PipelineStreamBridgeShadow getBridge();
    void handleIncomingData(byte[] data);
}
