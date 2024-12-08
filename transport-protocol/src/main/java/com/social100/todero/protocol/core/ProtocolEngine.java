package com.social100.todero.protocol.core;

import com.social100.todero.protocol.pipeline.Pipeline;
import com.social100.todero.protocol.transport.TransportInterface;
import lombok.Getter;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

public class ProtocolEngine {
    private static final String CHANNEL_DATA = "DATA";
    private static final String CHANNEL_TRANSPORT = "TRANSPORT";
    private static final long QUEUE_WAIT_TIMEOUT = 1000;

    private final TransportInterface dataTraffic;
    private final Selector selector;
    private final AtomicInteger packetIdGenerator = new AtomicInteger(1);
    private final ConcurrentHashMap<Integer, Object> ackWaitLocks = new ConcurrentHashMap<>();
    private final BlockingQueue<MessageQueueEntry> sendQueue = new LinkedBlockingQueue<>();
    // Create a priority queue sorted by `getTime`
    private final PriorityBlockingQueue<MessageQueueEntry> priorityQueue =
            new PriorityBlockingQueue<>(100, Comparator.comparingLong(MessageQueueEntry::getTime));
    ExecutorService executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());

    private final Pipeline pipeline;
    private List<Thread> threadList = new ArrayList<>();

    public ProtocolEngine(TransportInterface dataTraffic) throws IOException {
        this.dataTraffic = dataTraffic;
        this.selector = Selector.open();
        this.pipeline = null;

        // Register channels with the selector
        dataTraffic.getChannel().register(selector, SelectionKey.OP_READ, CHANNEL_DATA);

        // Start the sender thread
        startSenderThread();
        startSendMessageRetryThread();
    }

    public ProtocolEngine(TransportInterface dataTraffic, Pipeline pipeline) throws IOException {
        this.dataTraffic = dataTraffic;
        this.selector = Selector.open();
        this.pipeline = pipeline;

        // Register channels with the selector
        dataTraffic.getChannel().register(selector, SelectionKey.OP_READ, CHANNEL_DATA);

        // Start the sender thread
        startSenderThread();
        startSendMessageRetryThread();
    }

    private void startSendMessageRetryThread() {
        Thread thread = new Thread(() -> {
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    List<MessageQueueEntry> expiredEntries = new ArrayList<>();
                    // Poll all entries from the priority queue that have expired
                    synchronized (priorityQueue) {
                        if (priorityQueue.isEmpty()) {
                            priorityQueue.wait();
                        } else {
                            priorityQueue.wait(QUEUE_WAIT_TIMEOUT);
                        }
                        // Bulk removal of expired entries
                        Iterator<MessageQueueEntry> iterator = priorityQueue.iterator();
                        while (iterator.hasNext()) {
                            MessageQueueEntry entry = iterator.next();
                            if ((System.currentTimeMillis() - entry.getTime()) > 1000) {
                                expiredEntries.add(entry); // Collect expired entries
                                iterator.remove(); // Remove directly from PriorityQueue
                            } else {
                                break; // Stop iterating as PriorityQueue is ordered
                            }
                        }
                    }

                    // Add expired entries to the send queue
                    sendQueue.addAll(expiredEntries);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }, "ProtocolEngine-SenderMessageRetry-Thread");
        threadList.add(thread);
        thread.start();
    }

    private void startSenderThread() {
        Thread thread = new Thread(() -> {
            try {
                while (!Thread.currentThread().isInterrupted()) {
                    // Take a message from the queue
                    MessageQueueEntry entry = sendQueue.take();

                    boolean isInAckWaitLocks;
                    synchronized (ackWaitLocks) {
                        isInAckWaitLocks = ackWaitLocks.containsKey(entry.getPacketId());
                    }
                    if (isInAckWaitLocks) {
                        dataTraffic.send(entry.getDestination(), entry.serializedMessage);
                        synchronized (priorityQueue) {
                            if (entry.retry()) {
                                priorityQueue.offer(entry);
                                priorityQueue.notifyAll();
                            }
                        }
                    }
                }
            } catch (InterruptedException | IOException e) {
                Thread.currentThread().interrupt();
            }
        }, "ProtocolEngine-Sender-Thread");
        threadList.add(thread);
        thread.start();
    }

    public void startClient(ReceiveMessageCallback messageCallback, Consumer<Integer> ackSendMessageCallback) throws IOException {
        ReceiveMessageCallback receiveMessageCallback = new ReceiveMessageCallback((protocolMessage, responder) -> {
            messageCallback.consume(protocolMessage);
        });
        startServer( receiveMessageCallback, ackSendMessageCallback);
    }

    public void startServer(ReceiveMessageCallback messageCallback, Consumer<Integer> ackSendMessageCallback) throws IOException {
        // Start the selector loop in a new thread
        Thread thread = new Thread(() -> {
            try {
                while (!Thread.currentThread().isInterrupted()) {
                    selector.select(); // Blocking call, wakes up when an event occurs

                    Iterator<SelectionKey> keys = selector.selectedKeys().iterator();
                    while (keys.hasNext()) {
                        SelectionKey key = keys.next();
                        keys.remove();

                        if (key.isReadable()) {
                            DatagramChannel channel = (DatagramChannel) key.channel();
                            String channelType = (String) key.attachment();
                            ByteBuffer buffer = ByteBuffer.allocate(8192 * 1024);
                            InetSocketAddress sourceAddress = (InetSocketAddress) channel.receive(buffer);
                            buffer.flip();
                            String receivedData = new String(buffer.array(), 0, buffer.limit());

                            executor.submit(() -> {
                                try {
                                    ProtocolMessage protocolMessage = ProtocolUtils.deserialize(receivedData, this::processReceivedMessage);

                                    if (protocolMessage != null) {
                                        if (!protocolMessage.isAck()) {
                                            if (protocolMessage.isAckRequested()) {
                                                // Send ACK back to the sender
                                                sendAck(sourceAddress, protocolMessage);
                                            }
                                            handleDataMessage(protocolMessage, sourceAddress, messageCallback);
                                        } else {
                                            handleTransportMessage(protocolMessage, ackSendMessageCallback);
                                        }
                                    }
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                            });
                        }
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }, "ProtocolEngine-Selector-Thread");
        threadList.add(thread);
        thread.start();
    }

    public Integer sendMessage(InetSocketAddress destination, String message, boolean ackRequested) throws Exception {
        int packetId = packetIdGenerator.getAndIncrement();
        ProtocolMessage protocolMessage = new ProtocolMessage(packetId, message, ackRequested);
        //protocolMessage.setTransportPort(dataTraffic.getPort());
        String serializedMessage = ProtocolUtils.serialize(protocolMessage, this::prepareMessageForSending);

        // Add the message to the send queue
        MessageQueueEntry messageQueueEntry = new MessageQueueEntry(destination, packetId, serializedMessage, ackRequested);

        addMessageToSendQueue(packetId, messageQueueEntry);

        return packetId;
    }

    private void addMessageToSendQueue(int packetId, MessageQueueEntry messageQueueEntry) throws InterruptedException {
        Object object = new Object();
        ackWaitLocks.put(packetId, object);
        sendQueue.put(messageQueueEntry);
    }

    private void handleDataMessage(ProtocolMessage protocolMessage, InetSocketAddress sourceAddress, ReceiveMessageCallback messageCallback) throws IOException {
        // Pass the message and responder to the callback
        if (messageCallback != null) {
            Responder responder = new Responder(sourceAddress, this);
            messageCallback.consume(protocolMessage, responder);
        }
    }

    private void sendAck(InetSocketAddress sourceAddress, ProtocolMessage protocolMessage) throws IOException {
        int packetId = protocolMessage.getPacketId();
        ProtocolMessage ackMessage = ProtocolMessage.createAck(packetId);
        String serializedAck = ProtocolUtils.serialize(ackMessage);
        //InetSocketAddress ackDestination = new InetSocketAddress(sourceAddress.getAddress(), protocolMessage.getTransportPort());
        dataTraffic.send(sourceAddress, serializedAck);
    }

    private void handleTransportMessage(ProtocolMessage protocolMessage, Consumer<Integer> ackSendMessageCallback) {
        if (protocolMessage.isAck()) {
            int packetId = protocolMessage.getPacketId();
            Object ackLock = ackWaitLocks.remove(packetId);
            if (ackLock != null) {
                synchronized (ackLock) {
                    ackLock.notifyAll();
                }
            }
            ackSendMessageCallback.accept(packetId);
        }
    }

    public String prepareMessageForSending(String message) {
        return pipeline != null ? pipeline.processToSend(message) : message;
    }

    public String processReceivedMessage(String message) {
        return pipeline != null ? pipeline.processToReceive(message) : message;
    }

    public void close() {
        threadList.forEach(Thread::interrupt);
    }

    @Getter
    private static class MessageQueueEntry {
        private final int packetId;
        private final InetSocketAddress destination;
        private final String serializedMessage;
        private final boolean ackRequested;
        private long time;
        private byte retry;

        public MessageQueueEntry(InetSocketAddress destination, int packetId, String serializedMessage, boolean ackRequested) {
            this.packetId = packetId;
            this.destination = destination;
            this.serializedMessage = serializedMessage;
            this.ackRequested = ackRequested;
            this.time = System.currentTimeMillis();
            this.retry = 0;
        }

        public boolean retry() {
            if (this.retry > 5) {
                return false;
            }
            this.time = System.currentTimeMillis();
            this.retry += 1;
            return true;
        }
    }
}
