package com.social100.todero.console.base;

import com.social100.todero.common.message.MessageContainer;

import java.io.IOException;

public interface CommandProcessor {
    void open();
    boolean process(MessageContainer messageContainer);
    void close() throws IOException;
    CliCommandManager getCommandManager();
    //PipelineStreamBridge.PipelineStreamBridgeShadow getBridge();
    //void handleIncomingData(byte[] data);
}
