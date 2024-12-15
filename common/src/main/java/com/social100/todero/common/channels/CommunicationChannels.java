package com.social100.todero.common.channels;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class CommunicationChannels {

    // Fixed channel types
    public static final String CONTROL_CHANNEL = "control";
    public static final String PUBLIC_CHANNEL = "public";

    // Reserved events for CONTROL_CHANNEL
    private static final Set<String> RESERVED_CONTROL_EVENTS = Set.of("start", "stop", "restart");

    // Dynamic events and subscriptions
    private final Map<String, EventDetails> dynamicEvents;

    // Event details class
    private static class EventDetails {
        private final String description;
        private final Set<EventListener> listeners;

        public EventDetails(String description) {
            if (description == null || description.isEmpty()) {
                throw new IllegalArgumentException("Event description cannot be null or empty.");
            }
            this.description = description;
            this.listeners = new HashSet<>();
        }

        public String getDescription() {
            return description;
        }

        public Set<EventListener> getListeners() {
            return listeners;
        }
    }

    // Interface for event listeners
    public interface EventListener {
        void onEvent(String eventName, String message);
    }

    // Constructor
    public CommunicationChannels() {
        this.dynamicEvents = new HashMap<>();
    }

    // Register a new dynamic event with a description
    public void registerEvent(String eventName, String description) {
        if (eventName == null || eventName.isEmpty()) {
            throw new IllegalArgumentException("Event name cannot be null or empty.");
        }
        if (RESERVED_CONTROL_EVENTS.contains(eventName)) {
            throw new IllegalArgumentException("Event '" + eventName + "' is reserved for the CONTROL channel.");
        }
        if (dynamicEvents.containsKey(eventName)) {
            throw new IllegalArgumentException("Event '" + eventName + "' is already registered.");
        }
        dynamicEvents.put(eventName, new EventDetails(description));
    }

    // Check if an event is registered
    public boolean isEventRegistered(String eventName) {
        return dynamicEvents.containsKey(eventName);
    }

    // Subscribe a client to a dynamic event
    public void subscribeToEvent(String eventName, EventListener listener) {
        if (!isEventRegistered(eventName)) {
            throw new IllegalArgumentException("Event '" + eventName + "' is not registered.");
        }
        dynamicEvents.get(eventName).getListeners().add(listener);
    }

    // Notify clients of a triggered event
    public void triggerEvent(String eventName, String message) {
        if (!isEventRegistered(eventName)) {
            throw new IllegalArgumentException("Event '" + eventName + "' is not registered.");
        }
        EventDetails details = dynamicEvents.get(eventName);
        for (EventListener listener : details.getListeners()) {
            listener.onEvent(eventName, message);
        }
    }

    // Retrieve all registered events with their descriptions
    public Map<String, String> getAvailableEvents() {
        Map<String, String> eventDescriptions = new HashMap<>();
        for (Map.Entry<String, EventDetails> entry : dynamicEvents.entrySet()) {
            eventDescriptions.put(entry.getKey(), entry.getValue().getDescription());
        }
        return eventDescriptions;
    }

}
