package com.social100.todero.protocol.transport;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;

public class UdpTransport implements TransportInterface {
    private final DatagramChannel channel;
    private final int port;

    public UdpTransport(int port) throws IOException {
        this.channel = DatagramChannel.open();
        this.channel.bind(new InetSocketAddress(port));
        this.channel.configureBlocking(false); // Non-blocking mode

        SocketAddress localAddress = channel.getLocalAddress();
        if (localAddress instanceof InetSocketAddress inetSocketAddress) {
            this.port = inetSocketAddress.getPort();
            System.out.println("The channel is bound to port: " + this.port);
        } else {
            this.port = port;
        }
    }

    @Override
    public void send(InetSocketAddress destination, byte[] message) throws IOException {
        ByteBuffer buffer = ByteBuffer.wrap(message);
        channel.send(buffer, destination);
    }

    @Override
    public DatagramChannel getChannel() {
        return this.channel;
    }

    @Override
    public int getPort() {
        return this.port;
    }
}
