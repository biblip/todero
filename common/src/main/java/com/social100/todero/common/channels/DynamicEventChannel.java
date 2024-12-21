package com.social100.todero.common.channels;

import com.social100.todero.common.message.MessageContainer;
import com.social100.todero.common.message.channel.ChannelMessageFactory;
import com.social100.todero.common.message.channel.ChannelType;
import com.social100.todero.common.message.channel.impl.PublicDataPayload;
import lombok.Getter;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

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
            this.listeners = new HashSet<>();
        }

    }

    public DynamicEventChannel() {
        this.dynamicEvents = new HashMap<>();
    }

    @Override
    public void registerEvent(String eventName, String description) {
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
    public void subscribeToEvent(String eventName, EventListener listener) {
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
    public void triggerEvent(String eventName, String message) {
        if (EventChannel.ReservedEvent.isReserved(eventName)) {
            EventChannel.ReservedEvent reservedEvent = Arrays.stream(EventChannel.ReservedEvent.values())
                    .filter(e -> e.getEventName().equals(eventName))
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("Invalid reserved event name: " + eventName));
            ReservedEventRegistry.trigger(reservedEvent, MessageContainer.builder()
                    .addChannelMessage(ChannelMessageFactory.createChannelMessage(ChannelType.PUBLIC_DATA,
                            PublicDataPayload.builder()
                                    .message(message)
                                    .build()))
                    .build());
        } else if (dynamicEvents.containsKey(eventName)) {
            EventDetails details = dynamicEvents.get(eventName);
            for (EventListener listener : details.getListeners()) {
                listener.onEvent(eventName, MessageContainer.builder()
                        .addChannelMessage(ChannelMessageFactory.createChannelMessage(ChannelType.PUBLIC_DATA,
                                PublicDataPayload.builder()
                                        .message(message)
                                        .build()))
                        .build());
            }
        } else {
            throw new IllegalArgumentException("Event '" + eventName + "' is not registered.");
        }
    }

    /*@Override
    public void respond(String message) {
        EventChannel.ReservedEvent reservedEvent = EventChannel.ReservedEvent.RESPONSE;
        MessageContainer messageContainer = MessageContainer.builder()
                .version(MessageContainer.VERSION)
                .addChannelMessage(ChannelMessageFactory.createChannelMessage(ChannelType.PUBLIC_DATA, PublicDataPayload.builder()
                        .message(message)
                        .build()))
                .build();
        ReservedEventRegistry.trigger(reservedEvent, MessageContainerUtils.serialize(messageContainer));
    }*/

    @Override
    public Map<String, String> getAvailableEvents() {
        Map<String, String> eventDescriptions = new HashMap<>();
        for (Map.Entry<String, EventDetails> entry : dynamicEvents.entrySet()) {
            eventDescriptions.put(entry.getKey(), entry.getValue().getDescription());
        }
        return eventDescriptions;
    }
}
