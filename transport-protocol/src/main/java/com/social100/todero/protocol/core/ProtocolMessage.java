package com.social100.todero.protocol.core;

import lombok.Data;

@Data
public class ProtocolMessage {
    /*private final int packetId;
    private final byte[] payload;
    private final boolean ackRequested;
    private final boolean isAck;
     */

    /**
     * Constructor for regular data messages with acknowledgment requested by default.
     */
    /*public ProtocolMessage(int packetId, byte[] payload) {
        this(packetId, payload, true, false);
    }*/

    /**
     * Constructor for regular data messages, specifying acknowledgment requirement.
     */
    /*public ProtocolMessage(int packetId, byte[] payload, boolean ackRequested) {
        this(packetId, payload, ackRequested, false);
    }*/

    /**
     * Constructor for internal use, supporting all fields explicitly.
     */
    /*private ProtocolMessage(int packetId, byte[] payload, boolean ackRequested, boolean isAck) {
        this.packetId = packetId;
        this.payload = payload;
        this.ackRequested = ackRequested;
        this.isAck = isAck;
    }*/

    /**
     * Factory method to create acknowledgment messages.
     *
     * @param packetId The ID of the packet being acknowledged.
     * @return A new ProtocolMessage instance representing an acknowledgment.
     */
    /*public static ProtocolMessage createAck(int packetId) {
        return new ProtocolMessage(packetId, null, false, true);
    }*/

    /**
     * Factory method to create acknowledgment messages with a payload.
     *
     * @param packetId The ID of the packet being acknowledged.
     * @param payload  Additional information included in the acknowledgment.
     * @return A new ProtocolMessage instance representing an acknowledgment with a payload.
     */
    /*public static ProtocolMessage createAck(int packetId, byte[] payload) {
        return new ProtocolMessage(packetId, payload, false, true);
    }*/
}
