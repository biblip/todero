package com.social100.todero.common.channels;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class ChannelMessageSystem {

    // Enum for channel types
    public enum ChannelType {
        CONTROL,
        PUBLIC
    }

    // Enum for message types
    public enum MessageType {
        RESPONSE,  // e.g., "restart"
        EVENT,    // e.g., "door_opened"
        MESSAGE   // e.g., arbitrary public messages
    }

    // Class representing a single channel message
    public static class ChannelMessage {
        private final ChannelType channelType;
        private final MessageType messageType;
        private final String message;

        public ChannelMessage(ChannelType channelType, MessageType messageType, String message) {
            if (channelType == null || messageType == null || message == null || message.isEmpty()) {
                throw new IllegalArgumentException("ChannelType, MessageType, and message must not be null or empty.");
            }
            this.channelType = channelType;
            this.messageType = messageType;
            this.message = message;
        }

        public ChannelType getChannelType() {
            return channelType;
        }

        public MessageType getMessageType() {
            return messageType;
        }

        public String getMessage() {
            return message;
        }

        @Override
        public String toString() {
            return "ChannelMessage{" +
                    "channelType=" + channelType +
                    ", messageType=" + messageType +
                    ", message='" + message + '\'' +
                    '}';
        }
    }

    // Class to handle storage and retrieval of ChannelMessages
    public static class MessageStorage {
        private final List<ChannelMessage> messages;

        public MessageStorage() {
            this.messages = new ArrayList<>();
        }

        // Store a single message
        public void storeMessage(ChannelMessage message) {
            messages.add(message);
        }

        // Retrieve all messages
        public List<ChannelMessage> getAllMessages() {
            return new ArrayList<>(messages);
        }

        // Retrieve messages by channel type
        public List<ChannelMessage> getMessagesByChannel(ChannelType channelType) {
            return messages.stream()
                    .filter(msg -> msg.getChannelType() == channelType)
                    .collect(Collectors.toList());
        }

        // Retrieve messages by message type
        public List<ChannelMessage> getMessagesByType(MessageType messageType) {
            return messages.stream()
                    .filter(msg -> msg.getMessageType() == messageType)
                    .collect(Collectors.toList());
        }

        // Remove a message
        public boolean removeMessage(ChannelMessage message) {
            return messages.remove(message);
        }

        // Clear all messages
        public void clearMessages() {
            messages.clear();
        }
    }

    // Example usage
    public static void main(String[] args) {
        MessageStorage storage = new MessageStorage();

        // Create individual messages
        ChannelMessage controlMessage = new ChannelMessage(ChannelType.CONTROL, MessageType.RESPONSE, "restart");
        ChannelMessage publicEvent = new ChannelMessage(ChannelType.PUBLIC, MessageType.EVENT, "door_opened");
        ChannelMessage publicMessage = new ChannelMessage(ChannelType.PUBLIC, MessageType.MESSAGE, "Hello, World!");

        // Store messages
        storage.storeMessage(controlMessage);
        storage.storeMessage(publicEvent);
        storage.storeMessage(publicMessage);

        // Retrieve and process messages
        System.out.println("All Messages:");
        storage.getAllMessages().forEach(System.out::println);

        System.out.println("\nControl Messages:");
        storage.getMessagesByChannel(ChannelType.CONTROL).forEach(System.out::println);

        System.out.println("\nEvent Messages:");
        storage.getMessagesByType(MessageType.EVENT).forEach(System.out::println);

        // Remove a message and display remaining
        storage.removeMessage(controlMessage);
        System.out.println("\nMessages after removal:");
        storage.getAllMessages().forEach(System.out::println);

        // Clear all messages
        storage.clearMessages();
        System.out.println("\nMessages after clearing:");
        System.out.println(storage.getAllMessages());
    }
}
