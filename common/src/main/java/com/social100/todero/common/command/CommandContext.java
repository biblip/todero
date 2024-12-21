package com.social100.todero.common.command;

import com.social100.todero.common.channels.DynamicEventChannel;
import com.social100.todero.common.channels.EventChannel;
import com.social100.todero.common.channels.ReservedEventRegistry;
import com.social100.todero.common.message.MessageContainer;
import com.social100.todero.common.message.channel.ChannelMessageFactory;
import com.social100.todero.common.message.channel.ChannelType;
import com.social100.todero.common.message.channel.impl.EventPayload;
import com.social100.todero.common.message.channel.impl.PublicDataPayload;
import com.social100.todero.processor.EventDefinition;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Builder
@AllArgsConstructor
public class CommandContext<T extends DynamicEventChannel> {
    private final String sourceId;
    @Getter
    private final String[] args;
    @Setter
    @Getter
    private T instance;

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

    public void event(EventDefinition event, String message) {
        String eventName = event.name();
        MessageContainer messageContainer = MessageContainer.builder()
                .version(MessageContainer.VERSION)
                .responderId(sourceId)
                .addChannelMessage(ChannelMessageFactory.createChannelMessage(ChannelType.EVENT, EventPayload.builder()
                        .name(eventName)
                        .message(message)
                        .build()))
                .build();
        instance.triggerEvent(eventName, messageContainer);
    }
}
