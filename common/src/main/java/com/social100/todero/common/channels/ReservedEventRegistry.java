package com.social100.todero.common.channels;

import com.social100.todero.common.message.MessageContainer;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class ReservedEventRegistry {

    // Centralized registry for reserved events and listeners
    private static final Map<EventChannel.ReservedEvent, Set<EventChannel.EventListener>> reservedListeners = new HashMap<>();

    static {
        // Initialize reserved events
        for (EventChannel.ReservedEvent reservedEvent : EventChannel.ReservedEvent.values()) {
            reservedListeners.put(reservedEvent, new HashSet<>());
        }
    }

    // Subscribe a listener to a reserved event
    public static void subscribe(EventChannel.ReservedEvent reservedEvent, EventChannel.EventListener listener) {
        reservedListeners.get(reservedEvent).add(listener);
    }

    // Trigger a reserved event
    public static void trigger(EventChannel.ReservedEvent reservedEvent, MessageContainer message) {
        for (EventChannel.EventListener listener : reservedListeners.get(reservedEvent)) {
            listener.onEvent(reservedEvent.getEventName(), message);
        }
    }
}
