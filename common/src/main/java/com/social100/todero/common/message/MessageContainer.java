package com.social100.todero.common.message;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.social100.todero.common.message.channel.ChannelMessage;
import com.social100.todero.common.message.channel.ChannelType;
import com.social100.todero.common.message.channel.IPayload;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.HashMap;
import java.util.Map;

@Getter
@Builder(builderClassName = "MessageContainerBuilder")
@JsonIgnoreProperties(ignoreUnknown = true) // To ignore unknown fields for backward compatibility
@AllArgsConstructor
public class MessageContainer {
    public static final String VERSION = "V1";
    private final String version;

    @JsonIgnore
    private final String responderId;

    @Builder.Default
    private final Map<ChannelType, IPayload> messages = new HashMap<>();

    // Add this constructor
    public MessageContainer() {
        this.version = VERSION; // Default version
        this.responderId = null;
        this.messages = new HashMap<>();
    }

    /**
     * Add a channel message to the container.
     * This enforces that the channel and its payload are consistent.
     */
    public <T extends IPayload> void addChannelMessage(ChannelMessage<T> channelMessage) {
        messages.put(channelMessage.getChannel(), channelMessage.getPayload());
    }

    // Custom builder method
    public static class MessageContainerBuilder {
        private final Map<ChannelType, IPayload> messages = new HashMap<>();

        /**
         * Custom builder method to add all messages from a map.
         */
        public MessageContainerBuilder addAllMessages(Map<ChannelType, IPayload> messagesToAdd) {
            if (messagesToAdd != null) {
                this.messages.putAll(messagesToAdd);
            }
            return this;
        }

        /**
         * Custom builder method to add a channel message.
         * This enforces that the channel and its payload are consistent.
         */
        public <T extends IPayload> MessageContainerBuilder addChannelMessage(ChannelMessage<T> channelMessage) {
            this.messages.put(channelMessage.getChannel(), channelMessage.getPayload());
            return this;
        }

        /**
         * Custom build method to ensure `messages` is properly passed.
         */
        public MessageContainer build() {
            return new MessageContainer(
                    version != null ? version : VERSION, // Default version if not provided
                    responderId,
                    messages
            );
        }
    }
}
