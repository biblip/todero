package com.social100.todero.protocol.transport;


import com.social100.todero.protocol.core.ProtocolFrameManager;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;

public interface TransportInterface {
    void sendMessageRaw(byte[] data, SocketAddress destination) throws IOException;

    void startReceiving(TransportReceiver receiver) throws IOException;

    void sendMessage(ProtocolFrameManager.FrameMessage frameMessage, InetSocketAddress destination) throws IOException;

    void close() throws IOException;

    int getPort();
}
