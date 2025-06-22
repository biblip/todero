package com.social100.todero.protocol.core;

import lombok.Builder;
import lombok.Getter;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.function.Function;
import java.util.zip.CRC32;

public class ProtocolFrameManager {

    // Flags bitmask
    private static final int FLAG_TRANSPORT     = 1 << 0;  // e.g., 0x01
    private static final int FLAG_ACK           = 1 << 1;  // e.g., 0x02
    private static final int FLAG_ACK_REQUESTED = 1 << 2;  // e.g., 0x04
    private static final int FLAG_RESERVED_8    = 1 << 3;  // e.g., 0x08
    private static final int FLAG_RESERVED_16   = 1 << 4;  // e.g., 0x16
    private static final int FLAG_RESERVED_32   = 1 << 5;  // e.g., 0x32
    private static final int FLAG_RESERVED_64   = 1 << 6;  // e.g., 0x64
    private static final int FLAG_RESERVED_128  = 1 << 7;  // e.g., 0x128

    private static byte setFlag(byte original, int flag, boolean value) {
        if (value) {
            return (byte) (original | flag);
        } else {
            return (byte) (original & ~flag);
        }
    }

    private static boolean isFlagSet(byte flags, int flag) {
        return (flags & flag) != 0;
    }

    /**
     * A hypothetical data class for the message/frame.
     * This might incorporate chunking info (chunkIdx, totalChunks).
     */
    @Getter
    @Builder
    public static class FrameMessage {
        private final int messageId;
        private final String responderId;
        private final int chunkIndex;
        private final int totalChunks;
        private final boolean transport;   // i.e. TR
        private final boolean ack;
        private final boolean ackRequested;

        private final byte[] payload;      // raw original payload
        private byte[] transformedPayload; // payload after transform (compression/encryption)

    }

    /**
     * Serialize a FrameMessage into a binary protocol frame with:
     *   version(1), flags(1), messageId(4), chunkIndex(4), totalChunks(4),
     *   payLength(4), payTransf(4), payload(N), checksum(4).
     *
     * transformFn optionally transforms the payload (e.g., compression or encryption).
     */
    public static byte[] serialize(FrameMessage msg, Function<byte[], byte[]> transformFn) {
        // version: for future-proofing (set to 1)
        byte version = 0x01;

        // Build flags
        byte flags = 0;
        flags = setFlag(flags, FLAG_TRANSPORT, msg.isTransport());
        flags = setFlag(flags, FLAG_ACK, msg.isAck());
        flags = setFlag(flags, FLAG_ACK_REQUESTED, msg.isAckRequested());

        // Original payload
        byte[] originalPayload = (msg.getPayload() == null) ? new byte[0] : msg.getPayload();
        int payLength = originalPayload.length;

        // Transform the payload (encrypt, compress, etc.)
        byte[] transformedPayload = (transformFn != null && !isFlagSet(flags, FLAG_ACK))
                ? transformFn.apply(originalPayload)
                : originalPayload;
        if (transformedPayload == null) {
            transformedPayload = new byte[0];
        }
        int payTransf = transformedPayload.length;

        // Allocate buffer for the full frame.
        // Calculate size: 1 + 1 + (4 * 5) + payTransf + 4
        // Explanation:
        //   - 1 byte  (version)
        //   - 1 byte  (flags)
        //   - 4 bytes (messageId)
        //   - 4 bytes (chunkIndex)
        //   - 4 bytes (totalChunks)
        //   - 4 bytes (payLength)
        //   - 4 bytes (payTransf)
        //   - payTransf bytes (transformed payload)
        //   - 4 bytes (checksum)
        int frameSize = 2 + (4 * 5) + payTransf + 4;
        ByteBuffer buffer = ByteBuffer.allocate(frameSize);
        buffer.order(ByteOrder.BIG_ENDIAN); // or LITTLE_ENDIAN if desired

        // Write header
        buffer.put(version);
        buffer.put(flags);
        buffer.putInt(msg.getMessageId());
        buffer.putInt(msg.getChunkIndex());
        buffer.putInt(msg.getTotalChunks());
        buffer.putInt(payLength);
        buffer.putInt(payTransf);

        // Write transformed payload
        buffer.put(transformedPayload);

        // Compute CRC32 for everything except the 4 bytes of the checksum itself
        CRC32 crc32 = new CRC32();
        crc32.update(buffer.array(), 0, frameSize - 4); // everything except the last 4
        long checksumValue = crc32.getValue();

        // Write checksum (4 bytes)
        buffer.putInt((int) checksumValue);

        return buffer.array();
    }

    /**
     * Deserialize a binary frame back into a FrameMessage.
     *
     * transformFn is the *reverse* transform (e.g. decrypt, decompress) for the payload.
     */
    public static FrameMessage deserialize(byte[] data, String responderId, Function<byte[], byte[]> transformFn) {
        if (data == null || data.length < 2 + (4 * 5) + 4) {
            // Minimum size: 2 bytes (version+flags) + 20 bytes (5 ints) + 4 bytes (checksum) = 26
            return null;
        }

        // First, verify checksum
        // The last 4 bytes of 'data' are the CRC32. We compute a CRC over [0..length-5].
        ByteBuffer verifyBuf = ByteBuffer.wrap(data);
        verifyBuf.order(ByteOrder.BIG_ENDIAN);
        int dataLen = data.length;

        // The stored checksum is in the last 4 bytes
        int storedChecksum = verifyBuf.getInt(dataLen - 4);

        // Compute our own checksum over everything except those 4 bytes
        CRC32 crc32 = new CRC32();
        crc32.update(data, 0, dataLen - 4);
        long computedChecksum = crc32.getValue();

        if (storedChecksum != (int) computedChecksum) {
            System.err.println("Checksum mismatch! Packet is corrupted.");
            return null;
        }

        // Now parse the frame
        ByteBuffer buffer = ByteBuffer.wrap(data);
        buffer.order(ByteOrder.BIG_ENDIAN);

        byte version = buffer.get();
        byte flags   = buffer.get();
        int messageId   = buffer.getInt();
        int chunkIndex  = buffer.getInt();
        int totalChunks = buffer.getInt();
        int payLength   = buffer.getInt(); // original payload length
        int payTransf   = buffer.getInt(); // transformed payload length

        // Now read the transformed payload
        if (payTransf < 0 || payTransf > buffer.remaining() - 4) {
            // Something's off if payTransf is negative or bigger than what's left minus the checksum.
            // Remember the last 4 bytes are the checksum already consumed in verification.
            return null;
        }

        byte[] transformedPayload = new byte[payTransf];
        buffer.get(transformedPayload);

        // The final 4 bytes in the buffer is the checksum (already read above), so skip them
        // or confirm buffer.position() == dataLen.

        // Reverse transform the payload if needed
        byte[] originalPayload = (transformFn != null && !isFlagSet(flags, FLAG_ACK))
                ? transformFn.apply(transformedPayload)
                : transformedPayload;
        if (originalPayload == null) {
            originalPayload = new byte[0];
        }

        // Determine flags
        boolean isTransport    = isFlagSet(flags, FLAG_TRANSPORT);
        boolean isAck          = isFlagSet(flags, FLAG_ACK);
        boolean isAckRequested = isFlagSet(flags, FLAG_ACK_REQUESTED);

        return FrameMessage.builder()
                .responderId(responderId)
                .messageId(messageId)
                .chunkIndex(chunkIndex)
                .totalChunks(totalChunks)
                .transport(isTransport)
                .ack(isAck)
                .ackRequested(isAckRequested)
                .payload(originalPayload)
                .build();

    }

    /**
     * Example: reassemble multiple chunks into a single payload.
     * You would collect multiple FrameMessage objects with the same messageId.
     */
    public static byte[] reassembleMessage(FrameMessage[] chunks) {
        // Sort chunks by chunkIndex, ensure contiguous from 0..totalChunks-1, etc.
        // Then concatenate all originalPayloads in order.
        // The final size will be sum of all chunk's payLength (assuming payLength is the original untransformed size per chunk).
        // For simplicity, let's assume each chunk is part of the same messageId & the same totalChunks.

        // Basic example of reassembly:
        // 1. Sort the array by chunkIndex
        java.util.Arrays.sort(chunks, (a,b) -> Integer.compare(a.getChunkIndex(), b.getChunkIndex()));

        // 2. Concatenate
        int totalLen = 0;
        for (FrameMessage fm : chunks) {
            totalLen += fm.getPayload().length;
        }
        byte[] result = new byte[totalLen];
        int offset = 0;
        for (FrameMessage fm : chunks) {
            byte[] pay = fm.getPayload();
            System.arraycopy(pay, 0, result, offset, pay.length);
            offset += pay.length;
        }
        return result;
    }

    /**
     * Factory method to create acknowledgment messages.
     *
     * @param messageId The ID of the packet being acknowledged.
     * @return A new ProtocolMessage instance representing an acknowledgment.
     */
    public static FrameMessage createAck(int messageId) {
        return createAck(messageId, null);
    }

    /**
     * Factory method to create acknowledgment messages with a payload.
     *
     * @param messageId The ID of the packet being acknowledged.
     * @param payload  Additional information included in the acknowledgment.
     * @return A new ProtocolMessage instance representing an acknowledgment with a payload.
     */
    public static FrameMessage createAck(int messageId, byte[] payload) {
        return FrameMessage.builder()
                .messageId(messageId)
                .payload(payload)
                .ackRequested(false)
                .ack(true)
                .build();
    }

    public static FrameMessage peekMessageHeader(byte[] data) {
        try {
            if (data == null || data.length < 2 + (4 * 5) + 4) {
                // Minimum size: 2 bytes (version+flags) + 20 bytes (5 ints) + 4 bytes (checksum) = 26
                return null;
            }

            // First, verify checksum
            // The last 4 bytes of 'data' are the CRC32. We compute a CRC over [0..length-5].
            ByteBuffer verifyBuf = ByteBuffer.wrap(data);
            verifyBuf.order(ByteOrder.BIG_ENDIAN);
            int dataLen = data.length;

            // The stored checksum is in the last 4 bytes
            int storedChecksum = verifyBuf.getInt(dataLen - 4);

            // Compute our own checksum over everything except those 4 bytes
            CRC32 crc32 = new CRC32();
            crc32.update(data, 0, dataLen - 4);
            long computedChecksum = crc32.getValue();

            // Now parse the frame
            ByteBuffer buffer = ByteBuffer.wrap(data);
            buffer.order(ByteOrder.BIG_ENDIAN);

            byte version = buffer.get();
            byte flags   = buffer.get();
            int messageId   = buffer.getInt();

            // Determine flags
            boolean isTransport    = isFlagSet(flags, FLAG_TRANSPORT);
            boolean isAck          = isFlagSet(flags, FLAG_ACK);
            boolean isAckRequested = isFlagSet(flags, FLAG_ACK_REQUESTED);

            return FrameMessage.builder()
                .messageId(messageId)
                .ack(isAck)
                .ackRequested(isAckRequested)
                .build();
        } catch (Exception e) {
            return null;
        }
    }
}