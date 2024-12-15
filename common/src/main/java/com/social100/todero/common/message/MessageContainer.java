package com.social100.todero.common.message;

import com.social100.todero.common.message.channel.ChannelMessage;
import com.social100.todero.common.message.channel.ChannelType;
import com.social100.todero.common.message.channel.IPayload;
import com.social100.todero.common.profile.Profile;
import lombok.Builder;
import lombok.Getter;

import java.util.HashMap;
import java.util.Map;

@Getter
@Builder(builderClassName = "MessageContainerBuilder")
public class MessageContainer {
    private final Profile profile;

    @Builder.Default
    private final Map<ChannelType, ChannelMessage<? extends IPayload>> messages = new HashMap<>();

    /**
     * Add a channel message to the container.
     * This enforces that the channel and its payload are consistent.
     */
    public <T extends IPayload> void addChannelMessage(ChannelMessage<T> channelMessage) {
        messages.put(channelMessage.getChannel(), channelMessage);
    }

    // Custom builder method
    public static class MessageContainerBuilder {
        private Map<ChannelType, ChannelMessage<? extends IPayload>> messages = new HashMap<>();

        /**
         * Custom builder method to add a channel message.
         * This enforces that the channel and its payload are consistent.
         */
        public <T extends IPayload> MessageContainerBuilder addChannelMessage(ChannelMessage<T> channelMessage) {
            this.messages.put(channelMessage.getChannel(), channelMessage);
            return this;
        }
    }

}
