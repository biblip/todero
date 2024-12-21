package com.social100.todero.console.base;

import com.social100.todero.common.channels.EventChannel;
import com.social100.todero.common.config.AppConfig;
import com.social100.todero.common.message.MessageContainer;
import com.social100.todero.stream.PipelineStream;

public class ApiCommandLineInterface implements CommandLineInterface {
    private final CommandProcessor commandProcessor;
    private PipelineStream.ByteDataHandler outputDataHandler;

    public ApiCommandLineInterface(AppConfig appConfig, EventChannel.EventListener eventListener, boolean aiaProtocol) {
        this.commandProcessor = CommandProcessorFactory.createProcessor(appConfig, eventListener, aiaProtocol);
        this.commandProcessor.open();
    }

    @Override
    public void run(String[] args) {
        // Do nothing
    }

    public void process(MessageContainer messageContainer) {
        commandProcessor.process(messageContainer);
    }
}