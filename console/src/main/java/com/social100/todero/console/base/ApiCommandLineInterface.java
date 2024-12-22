package com.social100.todero.console.base;

import com.social100.todero.common.channels.EventChannel;
import com.social100.todero.common.message.MessageContainer;
import com.social100.todero.stream.PipelineStream;

import java.net.InetSocketAddress;

public class ApiCommandLineInterface implements CommandLineInterface {
    private final CommandProcessor commandProcessor;
    private PipelineStream.ByteDataHandler outputDataHandler;

    public ApiCommandLineInterface(InetSocketAddress serverAddress, EventChannel.EventListener eventListener) {
        this.commandProcessor = new AiaCommandProcessor(serverAddress, eventListener);
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