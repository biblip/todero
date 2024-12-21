package com.social100.todero.protocol.transport;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.DatagramChannel;

public interface TransportInterface {
    void send(InetSocketAddress destination, byte[] message) throws IOException;
    DatagramChannel getChannel();
    int getPort();
}
