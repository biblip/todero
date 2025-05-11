package com.social100.todero.protocol.transport;


import com.social100.todero.protocol.core.ProtocolFrameManager;

import java.io.IOException;
import java.net.InetSocketAddress;

public interface TransportInterface {
    void startReceiving(TransportReceiver receiver) throws IOException;
    void sendMessage(ProtocolFrameManager.FrameMessage frameMessage, InetSocketAddress destination) throws IOException;
    void close() throws IOException;
    int getPort();
}
