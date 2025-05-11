package com.social100.todero.protocol.transport;

import java.net.InetSocketAddress;

@FunctionalInterface
public interface TransportReceiver {
    void onMessageReceived(InetSocketAddress sourceAddress, byte[] message);
}
