package com.social100.todero.common.message;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.social100.todero.common.message.channel.ChannelMessage;
import com.social100.todero.common.message.channel.ChannelType;
import com.social100.todero.common.message.channel.IPayload;
import com.social100.todero.common.profile.Profile;
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
    private final Profile profile;

    @Builder.Default
    private final Map<ChannelType, IPayload> messages = new HashMap<>();

    // Add this constructor
    public MessageContainer() {
        this.version = VERSION; // Default version
        this.profile = null;
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
                    version != null ? version : "V1", // Default version if not provided
                    profile,
                    messages
            );
        }
    }
}
