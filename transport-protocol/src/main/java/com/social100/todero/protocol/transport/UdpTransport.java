package com.social100.todero.protocol.transport;

import com.social100.todero.protocol.core.ProtocolFrameManager;
import com.social100.todero.protocol.core.ProtocolFrameManager.FrameMessage;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.PriorityBlockingQueue;

public class UdpTransport implements TransportInterface {

    /* ------------- unchanged fields ------------- */
    private final DatagramChannel channel;
    private final int port;
    private final ExecutorService receiverExecutor =
            Executors.newSingleThreadExecutor(r -> new Thread(r, "UdpTransport-Receiver"));
    private final BlockingQueue<MessageQueueEntry> sendQueue = new LinkedBlockingQueue<>();
    private final PriorityBlockingQueue<MessageQueueEntry> retryQueue =
            new PriorityBlockingQueue<>(100, Comparator.comparingLong(MessageQueueEntry::getTime));
    private final ConcurrentHashMap<Integer, MessageQueueEntry> ackTracking = new ConcurrentHashMap<>();
    private volatile TransportReceiver receiver;
    private volatile boolean running = true;

    /* ------------- ctor ------------- */
    public UdpTransport(int port) throws IOException {
        channel = DatagramChannel.open();
        channel.bind(new InetSocketAddress(InetAddress.getByName("0.0.0.0"), port));
        channel.configureBlocking(false);

        SocketAddress local = channel.getLocalAddress();
        this.port = (local instanceof InetSocketAddress isa) ? isa.getPort() : port;

        startReceiver();
        startSender();
        startRetryLoop();
    }

    @Override
    public void sendMessageRaw(byte[] data, SocketAddress destination) throws IOException {
        channel.send(ByteBuffer.wrap(data), destination);
    }

    /* ------------- API ------------- */
    @Override
    public void startReceiving(TransportReceiver r) {
        receiver = r;
    }

    @Override
    public void sendMessage(FrameMessage frame, InetSocketAddress dest) throws IOException {
        byte[] bytes = ProtocolFrameManager.serialize(frame, null);
        MessageQueueEntry entry = new MessageQueueEntry(dest, frame.getMessageId(), bytes, frame.isAckRequested());

        if (frame.isAckRequested())      // track for ACK
            ackTracking.put(entry.getPacketId(), entry);

        sendQueue.add(entry);
    }

    @Override public int  getPort() {
        return port;
    }

    @Override public void close() throws IOException {
        running = false;
        receiverExecutor.shutdownNow();
        channel.close();
    }

    /* ========== internal threads ========== */

    /* -- Receiver ---------------------------------------------------------- */
    private void startReceiver() throws IOException {
        Selector sel = Selector.open();
        channel.register(sel, SelectionKey.OP_READ);

        receiverExecutor.submit(() -> {
            try {
                while (running) {
                    sel.select();

                    for (Iterator<SelectionKey> it = sel.selectedKeys().iterator(); it.hasNext();) {
                        SelectionKey key = it.next(); it.remove();
                        if (!key.isReadable()) continue;

                        ByteBuffer buf = ByteBuffer.allocate(8192);
                        InetSocketAddress src = (InetSocketAddress) channel.receive(buf);
                        if (src == null) continue;

                        buf.flip();
                        byte[] data = Arrays.copyOf(buf.array(), buf.limit());

                        /* ---------- Optional quick ACK handling ---------- */
                        try {
                            // Try deserializing only enough to check for ACK
                            FrameMessage peek = ProtocolFrameManager.peekMessageHeader(data);
                            if (peek != null && peek.isAck()) {
                                ackTracking.remove(peek.getMessageId());
                                continue; // don't pass pure ACKs to engine
                            }

                            if (peek != null && peek.isAckRequested()) {
                                sendAck(src, peek.getMessageId());
                            }
                        } catch (Exception ignored) {
                            // Not a frame — could be handshake or malformed — still forward
                        }

                        /* ---------- Always forward raw message ---------- */
                        if (receiver != null) {
                            receiver.onMessageReceived(src, data);
                        }
                    }
                }
            } catch (IOException e) {
                if (running) e.printStackTrace();
            }
        });
    }

    /* -- Sender ------------------------------------------------------------ */
    private void startSender() {
        new Thread(() -> {
            try {
                while (running) {
                    MessageQueueEntry e = sendQueue.take();

                    /* NEW: drop if already ACKed */
                    if (e.isAckRequested() && !ackTracking.containsKey(e.getPacketId()))
                        continue;

                    channel.send(ByteBuffer.wrap(e.getSerializedMessage()), e.getDestination());

                    if (e.isAckRequested() && ackTracking.containsKey(e.getPacketId()) && e.retry()) {
                        synchronized (retryQueue) {
                            retryQueue.offer(e);
                            retryQueue.notifyAll();
                        }
                    }
                }
            } catch (InterruptedException | IOException ex) {
                Thread.currentThread().interrupt();
            }
        }, "UdpTransport-Sender").start();
    }

    /* -- Retry loop -------------------------------------------------------- */
    private void startRetryLoop() {
        new Thread(() -> {
            while (running) {
                try {
                    synchronized (retryQueue) {
                        if (retryQueue.isEmpty())
                            retryQueue.wait();
                        else
                            retryQueue.wait(1_000);

                        List<MessageQueueEntry> resend = new ArrayList<>();
                        for (Iterator<MessageQueueEntry> it = retryQueue.iterator(); it.hasNext();) {
                            MessageQueueEntry e = it.next();
                            if (System.currentTimeMillis() - e.getTime() > 1_000) {
                                if (ackTracking.containsKey(e.getPacketId()))  // NEW guard
                                    resend.add(e);
                                it.remove();
                            } else break;     // queue is time‑ordered
                        }
                        sendQueue.addAll(resend);
                    }
                } catch (InterruptedException ignore) { Thread.currentThread().interrupt(); }
            }
        }, "UdpTransport-Retry").start();
    }

    /* -- helper ------------------------------------------------------------ */
    private void sendAck(InetSocketAddress dest, int id) throws IOException {
        FrameMessage ack = ProtocolFrameManager.createAck(id);
        byte[] bytes    = ProtocolFrameManager.serialize(ack, null);
        channel.send(ByteBuffer.wrap(bytes), dest);        // one‑shot, no retry
    }
}
