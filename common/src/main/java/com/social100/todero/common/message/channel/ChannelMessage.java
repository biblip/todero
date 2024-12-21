package com.social100.todero.common.message.channel;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder(builderClassName = "ChannelMessageBuilder")
@AllArgsConstructor
public class ChannelMessage<T extends IPayload> {
    private final ChannelType channel;
    private final T payload;

    public ChannelMessage() {
        channel = null;
        payload = null;
    }

    // Custom builder to enforce validation
    public static class ChannelMessageBuilder<T extends IPayload> {
        public ChannelMessage<T> build() {
            // Perform validation
            validate(channel, payload);

            // Proceed with the standard build process
            return new ChannelMessage<>(channel, payload);
        }

        private void validate(ChannelType channel, T payload) {
            if (channel == null || payload == null) {
                throw new IllegalArgumentException("Channel and Payload cannot be null.");
            }

            if (!channel.getPayloadType().isInstance(payload)) {
                throw new IllegalArgumentException("Invalid payload type for channel: " + channel +
                        ". Expected: " + channel.getPayloadType().getSimpleName() +
                        ", but got: " + payload.getClass().getSimpleName());
            }

        }
    }
}
