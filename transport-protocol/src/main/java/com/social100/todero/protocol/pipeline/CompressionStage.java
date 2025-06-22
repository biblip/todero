package com.social100.todero.protocol.pipeline;

import java.io.ByteArrayOutputStream;
import java.util.zip.DataFormatException;
import java.util.zip.Deflater;
import java.util.zip.Inflater;

public class CompressionStage implements PipelineStage {

    @Override
    public byte[] processToSend(byte[] message, String destinationId) {
        if (message == null) {
            message = new byte[0];
        }
        return compress(message);
    }

    @Override
    public byte[] processToReceive(byte[] message, String sourceId) {
        if (message == null) {
            message = new byte[0];
        }
        return decompress(message);
    }

    private byte[] compress(byte[] data) {
        Deflater deflater = new Deflater();
        deflater.setInput(data);
        deflater.finish();

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        byte[] buffer = new byte[4096];  // buffer of 4 KB

        try {
            while (!deflater.finished()) {
                int bytesCompressed = deflater.deflate(buffer);
                baos.write(buffer, 0, bytesCompressed);
            }
        } finally {
            deflater.end();
        }

        return baos.toByteArray();
    }

    private byte[] decompress(byte[] data) {
        Inflater inflater = new Inflater();
        inflater.setInput(data);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        byte[] buffer = new byte[4096];  // buffer of 4 KB

        try {
            while (!inflater.finished()) {
                int bytesDecompressed = inflater.inflate(buffer);
                baos.write(buffer, 0, bytesDecompressed);
            }
        } catch (DataFormatException e) {
            throw new RuntimeException("Decompression failed", e);
        } finally {
            inflater.end();
        }

        return baos.toByteArray();
    }
}