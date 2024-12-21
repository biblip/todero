package com.social100.todero.console.base;

import com.social100.todero.common.config.AppConfig;
import com.social100.todero.common.message.MessageContainer;
import com.social100.todero.stream.PipelineStream;

public class ApiCommandLineInterface implements CommandLineInterface {
    private final CommandProcessor commandProcessor;
    private PipelineStream.ByteDataHandler outputDataHandler;

    public ApiCommandLineInterface(AppConfig appConfig, boolean aiaProtocol) {
        this.commandProcessor = CommandProcessorFactory.createProcessor(appConfig, aiaProtocol);
        this.commandProcessor.open();
    }

    @Override
    public void run(String[] args) {
        // Do nothing
    }

    public void setOutputDataHandler(PipelineStream.ByteDataHandler byteDataHandler) {
        commandProcessor.getBridge().readAsync(byteDataHandler);
    }

    public void process(MessageContainer messageContainer) {
        commandProcessor.process(messageContainer);
    }

    public void writeAsync(byte[] data) {
        this.commandProcessor.getBridge().writeAsync(data);
    }
}