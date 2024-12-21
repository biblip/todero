package com.social100.todero.common.message;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.Map;

public class MessageContainerUtils {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    /**
     * Serializes a MessageContainer object to a JSON string.
     *
     * @param messageContainer The MessageContainer object to serialize.
     * @return The JSON string representation, or null if serialization fails.
     */
    public static String serialize(MessageContainer messageContainer) {
        if (messageContainer == null) {
            return null; // Avoid serializing a null object
        }
        try {
            return OBJECT_MAPPER.writeValueAsString(messageContainer);
        } catch (JsonProcessingException ignore) {
            return null; // Return null in case of an error
        }
    }

    /**
     * Serializes a MessageContainer object to a prettified JSON string.
     *
     * @param messageContainer The MessageContainer object to serialize.
     * @return The pretty-printed JSON string representation, or null if serialization fails.
     */
    public static String serializePretty(MessageContainer messageContainer) {
        if (messageContainer == null) {
            return null; // Avoid serializing a null object
        }
        try {
            return OBJECT_MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(messageContainer);
        } catch (JsonProcessingException ignore) {
            return null; // Return null in case of an error
        }
    }

    /**
     * Deserializes a JSON string to a MessageContainer object.
     *
     * @param json The JSON string to deserialize.
     * @return The MessageContainer object, or an empty MessageContainer if deserialization fails.
     */
    public static MessageContainer deserialize(String json) {
        if (json == null || json.isBlank()) {
            return createEmptyMessageContainer(); // Return an empty object if input is invalid
        }
        try {
            return OBJECT_MAPPER.readValue(json, MessageContainer.class);
        } catch (Exception e) {
            e.printStackTrace();
            return createEmptyMessageContainer(); // Return an empty object in case of an error
        }
    }

    /**
     * Creates an empty MessageContainer with default values.
     *
     * @return An empty MessageContainer.
     */
    private static MessageContainer createEmptyMessageContainer() {
        return MessageContainer.builder()
                .responderId(null)
                .messages(Map.of()) // Use an empty map
                .build();
    }
}