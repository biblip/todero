package com.social100.todero.aia.service;

import com.social100.todero.common.channels.EventChannel;
import com.social100.todero.common.message.MessageContainer;
import com.social100.todero.common.message.channel.ChannelMessageFactory;
import com.social100.todero.common.message.channel.ChannelType;
import com.social100.todero.common.message.channel.impl.PublicDataPayload;
import com.social100.todero.console.base.ApiCommandLineInterface;

import java.net.InetSocketAddress;

public class ApiAIAProtocolService {
    public final static String MAIN_GROUP = "Main";
    public final static String RESERVED_GROUP = "Reserved";

    ApiCommandLineInterface apiCommandLineInterface = null;

    public void initiate(InetSocketAddress serverAddress, EventChannel.EventListener eventListener) {
        if (apiCommandLineInterface == null) {
            apiCommandLineInterface = new ApiCommandLineInterface(serverAddress, eventListener);
        } else {
            throw new RuntimeException("Already Open");
        }
    }

    public void exec(String line) {
        MessageContainer messageContainer = MessageContainer.builder()
                .addChannelMessage(ChannelMessageFactory.createChannelMessage(ChannelType.PUBLIC_DATA, PublicDataPayload.builder()
                        .message(line)
                        .build()))
                .build();
        apiCommandLineInterface.process(messageContainer);
    }

    public void conclude() {
        if (apiCommandLineInterface != null) {
            apiCommandLineInterface = null;
        }
    }

}
