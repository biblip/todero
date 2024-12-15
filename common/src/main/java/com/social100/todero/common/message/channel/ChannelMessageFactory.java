package com.social100.todero.common.message.channel;

public class ChannelMessageFactory {

    /**
     * Creates a ChannelMessage based on the channel type and payload.
     *
     * @param channel the type of the channel
     * @param payload     the payload for the channel
     * @return the created ChannelMessage
     * @throws IllegalArgumentException if the payload type does not match the channel type
     */
    public static <T extends IPayload> ChannelMessage<T> createChannelMessage(ChannelType channel, T payload) {
        if (channel == null || payload == null) {
            throw new IllegalArgumentException("Channel and Payload cannot be null.");
        }

        // Validate the payload type using the enum's associated class
        if (!channel.getPayloadType().isInstance(payload)) {
            throw new IllegalArgumentException("Invalid payload type for channel: " + channel +
                    ". Expected: " + channel.getPayloadType().getSimpleName() +
                    ", but got: " + payload.getClass().getSimpleName());
        }

        // Create the ChannelMessage
        return ChannelMessage.<T>builder()
                .channel(channel)
                .payload(payload)
                .build();
    }
}

