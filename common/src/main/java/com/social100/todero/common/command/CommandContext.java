package com.social100.todero.common.command;

import com.social100.todero.common.channels.EventChannel;
import com.social100.todero.common.channels.ReservedEventRegistry;
import com.social100.todero.common.message.MessageContainer;
import com.social100.todero.common.message.MessageContainerUtils;
import com.social100.todero.common.message.channel.ChannelMessageFactory;
import com.social100.todero.common.message.channel.ChannelType;
import com.social100.todero.common.message.channel.impl.PublicDataPayload;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Builder
@AllArgsConstructor
public class CommandContext {
    private final String sourceId;
    @Getter
    private final String[] args;

    public void respond(String message) {
        EventChannel.ReservedEvent reservedEvent = EventChannel.ReservedEvent.RESPONSE;
        MessageContainer messageContainer = MessageContainer.builder()
                .version(MessageContainer.VERSION)
                .responderId(sourceId)
                .addChannelMessage(ChannelMessageFactory.createChannelMessage(ChannelType.PUBLIC_DATA, PublicDataPayload.builder()
                        .message(message)
                        .build()))
                .build();
        ReservedEventRegistry.trigger(reservedEvent, messageContainer);
    }

}
