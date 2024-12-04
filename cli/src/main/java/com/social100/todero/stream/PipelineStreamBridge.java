package com.social100.todero.stream;

import java.io.IOException;

public class PipelineStreamBridge {
    private final PipelineStream sendStream;   // Stream to send data to the other end
    private final PipelineStream receiveStream; // Stream to receive data from the other end

    private PipelineStreamBridge(PipelineStream sendStream, PipelineStream receiveStream) {
        this.sendStream = sendStream;
        this.receiveStream = receiveStream;
    }

    // Return the object for the other end of the communication
    public PipelineStreamBridgeShadow getBridge() {
        return new PipelineStreamBridgeShadow(receiveStream, sendStream);
    }

    public void writeAsync(byte[] data) {
        sendStream.writeAsync(data);
    }

    public void close() {
        sendStream.close();
        receiveStream.close();
    }

    // Builder class
    public static class Builder {
        private DataHandler receiveHandler; // Handler for incoming data
        private boolean listeningStarted = false;

        public Builder onReceive(DataHandler handler) {
            this.receiveHandler = handler;
            return this;
        }

        public PipelineStreamBridge build() throws IOException {
            if (receiveHandler == null) {
                throw new IllegalStateException("Receive handler must be provided");
            }

            PipelineStream sendStream = new PipelineStream();
            PipelineStream receiveStream = new PipelineStream();

            // Start listening for incoming data
            if (!listeningStarted) {
                receiveStream.readAsync(receiveHandler::handle);
                listeningStarted = true;
            }

            return new PipelineStreamBridge(sendStream, receiveStream);
        }
    }

    public static class PipelineStreamBridgeShadow {
        private final PipelineStream sendStream;   // Stream to send data to the other end
        private final PipelineStream receiveStream; // Stream to receive data from the other end

        private PipelineStreamBridgeShadow(PipelineStream sendStream, PipelineStream receiveStream) {
            this.sendStream = sendStream;
            this.receiveStream = receiveStream;
        }

        public void writeAsync(byte[] data) {
            sendStream.writeAsync(data);
        }

        public void readAsync(PipelineStream.ByteDataHandler handler) {
            receiveStream.readAsync(handler);
        }

        public void close() {
            sendStream.close();
            receiveStream.close();
        }
    }

    // Functional interface for handling received data
    @FunctionalInterface
    public interface DataHandler {
        void handle(byte[] data);
    }
}