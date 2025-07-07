package com.social100.todero.common.command;

import com.social100.todero.common.base.PluginManagerInterface;
import com.social100.todero.common.channels.DynamicEventChannel;
import com.social100.todero.common.channels.EventChannel;
import com.social100.todero.common.channels.ReservedEventRegistry;
import com.social100.todero.common.message.MessageContainer;
import com.social100.todero.common.message.channel.ChannelMessageFactory;
import com.social100.todero.common.message.channel.ChannelType;
import com.social100.todero.common.message.channel.impl.EventPayload;
import com.social100.todero.common.message.channel.impl.PublicDataPayload;
import com.social100.todero.console.base.OutputType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;

import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

@Builder
@AllArgsConstructor
public class CommandContext {
    private final String sourceId;
    @Getter
    private final String[] args;
    @Setter
    @Getter
    private DynamicEventChannel instance;
    @Getter
    List<String>  agents;
    @Getter
    List<String>  tools;
    private final PluginManagerInterface pluginManager;

    Consumer<String> consumer;

    public void setListener(@NonNull Consumer<String> consumer) {
        this.consumer = consumer;
    }

    public void respond(String message) {
        getConsumer().ifPresentOrElse(c -> c.accept(message), () -> {
            EventChannel.ReservedEvent reservedEvent = EventChannel.ReservedEvent.RESPONSE;
            MessageContainer messageContainer = MessageContainer.builder()
                .version(MessageContainer.VERSION)
                .responderId(sourceId)
                .addChannelMessage(ChannelMessageFactory.createChannelMessage(ChannelType.PUBLIC_DATA, PublicDataPayload.builder()
                    .message(message)
                    .build()))
                .build();
            ReservedEventRegistry.trigger(reservedEvent, messageContainer);
        });
    }

    private Optional<Consumer<String>> getConsumer() {
        return Optional.ofNullable(consumer);
    }

    public void event(String eventName, String message) {
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

    public void execute(String pluginName, String command, CommandContext context) {
        pluginManager.execute(pluginName, command, context, true);
    }

    public String getHelp(String pluginName, String commandName, OutputType outputType) {
        return pluginManager.getHelp(pluginName, commandName, outputType);
    }
}
