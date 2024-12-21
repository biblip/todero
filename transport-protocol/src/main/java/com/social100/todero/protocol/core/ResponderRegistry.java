package com.social100.todero.protocol.core;

import lombok.Getter;

import java.net.InetSocketAddress;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;

public class ResponderRegistry {

    private final Map<String, Responder> responderMap;
    private final Map<String, Responder> addressLabelToResponderMap;
    private final Map<String, Long> lastUsedMap;
    private final ScheduledExecutorService cleanerService;
    private static final long TIMEOUT_MS = 30000; // 30 seconds timeout for demonstration purposes
    private final ReentrantLock lock = new ReentrantLock();

    public ResponderRegistry() {
        this.responderMap = new ConcurrentHashMap<>();
        this.addressLabelToResponderMap = new ConcurrentHashMap<>();
        this.lastUsedMap = new ConcurrentHashMap<>();
        this.cleanerService = Executors.newSingleThreadScheduledExecutor();
        startCleaner();
    }

    private void startCleaner() {
        cleanerService.scheduleAtFixedRate(() -> {
            lock.lock();
            try {
                long currentTime = System.currentTimeMillis();
                for (String id : lastUsedMap.keySet()) {
                    if (currentTime - lastUsedMap.get(id) > TIMEOUT_MS) {
                        removeResponder(id);
                    }
                }
            } finally {
                lock.unlock();
            }
        }, TIMEOUT_MS, TIMEOUT_MS, TimeUnit.MILLISECONDS);
    }

    /**
     * Adds or reuses a Responder in the registry.
     *
     * @param destination The destination address of the responder.
     * @param engine      The protocol engine associated with the responder.
     * @return The Responder.
     */
    public Responder useResponder(InetSocketAddress destination, ProtocolEngine engine) {
        if (destination == null || engine == null) {
            throw new IllegalArgumentException("Destination and engine cannot be null.");
        }

        String addressLabel = Responder.getAddressLabel(destination);

        lock.lock();
        try {
            Responder responder;

            if (addressLabelToResponderMap.containsKey(addressLabel)) {
                responder = addressLabelToResponderMap.get(addressLabel);
            } else {
                String id = UUID.randomUUID().toString();
                responder = new Responder(destination, id, engine, this::updateLastUsed);
                responderMap.put(id, responder);
                addressLabelToResponderMap.put(addressLabel, responder);
            }

            lastUsedMap.put(responder.getId(), System.currentTimeMillis());
            return responder;
        } finally {
            lock.unlock();
        }
    }

    public void updateLastUsed(String id) {
        lock.lock();
        try {
            lastUsedMap.put(id, System.currentTimeMillis());
        } finally {
            lock.unlock();
        }
    }

    /**
     * Retrieves a Responder by its identifier.
     *
     * @param id The identifier.
     * @return The corresponding Responder, or null if not found.
     */
    public Responder getResponder(String id) {
        lock.lock();
        try {
            Responder responder = responderMap.get(id);
            if (responder != null) {
                lastUsedMap.put(id, System.currentTimeMillis());
            }
            return responder;
        } finally {
            lock.unlock();
        }
    }

    /**
     * Retrieves a Responder by its address label.
     *
     * @param addressLabel The label for an IP address value e.g., "ip:port".
     * @return The corresponding Responder, or null if not found.
     */
    public Responder getResponderByAddressLabel(String addressLabel) {
        lock.lock();
        try {
            Responder responder = addressLabelToResponderMap.get(addressLabel);
            if (responder != null) {
                lastUsedMap.put(responder.getId(), System.currentTimeMillis());
            }
            return responder;
        } finally {
            lock.unlock();
        }
    }

    /**
     * Removes a Responder by its identifier.
     *
     * @param id The identifier.
     * @return The removed Responder, or null if not found.
     */
    public Responder removeResponder(String id) {
        lock.lock();
        try {
            Responder removed = responderMap.remove(id);
            if (removed != null) {
                addressLabelToResponderMap.remove(removed.getAddressLabel());
                lastUsedMap.remove(id);
            }
            return removed;
        } finally {
            lock.unlock();
        }
    }

    /**
     * Checks if a Responder exists for a given identifier.
     *
     * @param id The identifier.
     * @return True if the Responder exists, false otherwise.
     */
    public boolean containsResponder(String id) {
        lock.lock();
        try {
            boolean exists = responderMap.containsKey(id);
            if (exists) {
                lastUsedMap.put(id, System.currentTimeMillis());
            }
            return exists;
        } finally {
            lock.unlock();
        }
    }

    public static class Responder {
        private final InetSocketAddress destination;
        @Getter
        private final String id;
        private final ProtocolEngine engine;
        private final AtomicInteger packetIdGenerator = new AtomicInteger(1);
        private final UsageTracker usageTracker;

        public Responder(InetSocketAddress destination, String id, ProtocolEngine engine, UsageTracker usageTracker) {
            this.destination = destination;
            this.id = id;
            this.engine = engine;
            this.usageTracker = usageTracker;
        }

        public String getAddressLabel() {
            return destination.getAddress().getHostAddress() + ":" + destination.getPort();
        }

        public static String getAddressLabel(InetSocketAddress destination) {
            return destination.getAddress().getHostAddress() + ":" + destination.getPort();
        }

        public Integer sendMessage(byte[] message, boolean ackRequested) throws Exception {
            usageTracker.updateLastUsed(id);
            return engine.sendMessage(destination, message, ackRequested);
        }
    }

    // Interface for tracking usage
    public interface UsageTracker {
        void updateLastUsed(String id);
    }

    public void shutdown() {
        cleanerService.shutdown();
        try {
            if (!cleanerService.awaitTermination(5, TimeUnit.SECONDS)) {
                cleanerService.shutdownNow();
            }
        } catch (InterruptedException e) {
            cleanerService.shutdownNow();
        }
    }
}