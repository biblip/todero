package com.social100.todero.common.channels;

import com.social100.todero.common.message.MessageContainer;
import lombok.Getter;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

public class DynamicEventChannel implements EventChannel {

    private final Map<String, EventDetails> dynamicEvents;

    @Getter
    private static class EventDetails {
        private final String description;
        private final Set<EventListener> listeners;

        public EventDetails(String description) {
            if (description == null || description.isEmpty()) {
                throw new IllegalArgumentException("Event description cannot be null or empty.");
            }
            this.description = description;
            this.listeners = new CopyOnWriteArraySet<>();
        }

    }

    public DynamicEventChannel() {
        this.dynamicEvents = new ConcurrentHashMap<>();
    }

    @Override
    public synchronized void registerEvent(String eventName, String description) {
        if (eventName == null || eventName.isEmpty()) {
            throw new IllegalArgumentException("Event name cannot be null or empty.");
        }
        if (EventChannel.ReservedEvent.isReserved(eventName)) {
            throw new IllegalArgumentException("Event '" + eventName + "' is reserved and cannot be registered.");
        }
        if (dynamicEvents.containsKey(eventName)) {
            throw new IllegalArgumentException("Event '" + eventName + "' is already registered.");
        }
        dynamicEvents.put(eventName, new EventDetails(description));
    }

    @Override
    public boolean isEventRegistered(String eventName) {
        return dynamicEvents.containsKey(eventName) || EventChannel.ReservedEvent.isReserved(eventName);
    }

    @Override
    public synchronized void subscribeToEvent(String eventName, EventListener listener) {
        if (EventChannel.ReservedEvent.isReserved(eventName)) {
            EventChannel.ReservedEvent reservedEvent = Arrays.stream(EventChannel.ReservedEvent.values())
                    .filter(e -> e.getEventName().equals(eventName))
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("Invalid reserved event name: " + eventName));
            ReservedEventRegistry.subscribe(reservedEvent, listener);
        } else if (dynamicEvents.containsKey(eventName)) {
            dynamicEvents.get(eventName).getListeners().add(listener);
        } else {
            throw new IllegalArgumentException("Event '" + eventName + "' is not registered.");
        }
    }

    @Override
    public void triggerEvent(String eventName, MessageContainer message) {
        if (EventChannel.ReservedEvent.isReserved(eventName)) {
            EventChannel.ReservedEvent reservedEvent = Arrays.stream(EventChannel.ReservedEvent.values())
                    .filter(e -> e.getEventName().equals(eventName))
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("Invalid reserved event name: " + eventName));
            ReservedEventRegistry.trigger(reservedEvent, message);
        } else if (dynamicEvents.containsKey(eventName)) {
            EventDetails details = dynamicEvents.get(eventName);

            synchronized (details) {
                for (EventListener listener : details.getListeners()) {
                    try {
                        listener.onEvent(eventName, message); // Notify listener
                    } catch (Exception e) {
                        // Log the error and proceed with the next listener
                        System.err.println("Error in listener : " + e.getMessage());
                        e.printStackTrace();
                    }
                }
            }
        } else {
            System.err.println("Event '" + eventName + "' is not registered.");
        }
    }

    @Override
    public Map<String, String> getAvailableEvents() {
        Map<String, String> eventDescriptions = new HashMap<>();
        for (Map.Entry<String, EventDetails> entry : dynamicEvents.entrySet()) {
            eventDescriptions.put(entry.getKey(), entry.getValue().getDescription());
        }
        return eventDescriptions;
    }
}
