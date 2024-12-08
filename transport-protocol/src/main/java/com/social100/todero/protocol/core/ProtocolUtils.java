package com.social100.todero.protocol.core;

import java.util.function.Function;

public class ProtocolUtils {
    public static String serialize(ProtocolMessage message) {
        return serialize(message, null);
    }
    /**
     * Serialize a ProtocolMessage to a string format.
     * Format: packetId|TR|ACK|ackPort (for transport messages)
     *         packetId|DT|payload|ackPort (for regular messages)
     */
    public static String serialize(ProtocolMessage message, Function<String, String> prepare) {
        if (message.isAck()) {
            return message.getPacketId() + "|TR|ACK";
        } else {
            String preparedMessage = message.getPayload();
            if (prepare != null) {
                preparedMessage = prepare.apply(message.getPayload());
            }
            if (message.isAckRequested()) {
                return message.getPacketId() + "|DT|AR|" + preparedMessage;
            } else {
                return message.getPacketId() + "|DT|--|" + preparedMessage;
            }
        }
    }

    /**
     * Deserialize a string to a ProtocolMessage.
     * Handles both transport messages and regular data messages.
     */
    public static ProtocolMessage deserialize(String data, Function<String, String> process) {
        // Expecting 5 parts 3 parts for ack: packetId, TRansport/DaTa, AckRequest/--, payload, transportPort
        String[] parts = data.split("\\|", 4);
        if (parts.length < 3) return null;

        try {
            int packetId = Integer.parseInt(parts[0]);
            if ("TR".equals(parts[1])) {
                if ("ACK".equals(parts[2])) {
                    return ProtocolMessage.createAck(packetId);
                } else {
                    return null; // Invalid transport message
                }
            } else {
                boolean ackRequested = "AR".equals(parts[2]);
                String processedMessage = parts[3];
                if (process != null) {
                    processedMessage = process.apply(parts[3]);
                }
                return new ProtocolMessage(packetId, processedMessage, ackRequested);
            }
        } catch (NumberFormatException e) {
            e.printStackTrace();
            return null; // Invalid format
        }
    }
}