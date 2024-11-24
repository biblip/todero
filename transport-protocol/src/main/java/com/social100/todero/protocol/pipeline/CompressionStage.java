package com.social100.todero.protocol.pipeline;

import java.util.zip.Deflater;
import java.util.zip.Inflater;

public class CompressionStage implements PipelineStage {
    @Override
    public String processToSend(String message) {
        return compress(message);
    }

    @Override
    public String processToReceive(String message) {
        return decompress(message);
    }

    private String compress(String data) {
        try {
            byte[] input = data.getBytes("UTF-8");
            Deflater deflater = new Deflater();
            deflater.setInput(input);
            deflater.finish();

            byte[] buffer = new byte[1024];
            int compressedDataLength = deflater.deflate(buffer);
            deflater.end();

            return new String(buffer, 0, compressedDataLength, "ISO-8859-1");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private String decompress(String data) {
        try {
            byte[] input = data.getBytes("ISO-8859-1");
            Inflater inflater = new Inflater();
            inflater.setInput(input);

            byte[] buffer = new byte[1024];
            int decompressedDataLength = inflater.inflate(buffer);
            inflater.end();

            return new String(buffer, 0, decompressedDataLength, "UTF-8");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}

