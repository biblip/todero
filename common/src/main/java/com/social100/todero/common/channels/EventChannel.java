package com.social100.todero.common.channels;

import com.social100.todero.common.message.MessageContainer;
import lombok.Getter;

import java.util.Map;

public interface EventChannel {

    // Shared reserved events across all implementations
    @Getter
    enum ReservedEvent {
        START("start"),
        STOP("stop"),
        RESTART("restart"),
        RESPONSE("response");

        private final String eventName;

        ReservedEvent(String eventName) {
            this.eventName = eventName;
        }

        public static boolean isReserved(String eventName) {
            for (ReservedEvent reservedEvent : ReservedEvent.values()) {
                if (reservedEvent.eventName.equals(eventName)) {
                    return true;
                }
            }
            return false;
        }
    }

    /**
     * Registers a new dynamic event with a description.
     *
     * @param eventName    the name of the event to register.
     * @param description  the description of the event.
     */
    void registerEvent(String eventName, String description);

    /**
     * Checks if an event is registered.
     *
     * @param eventName  the name of the event to check.
     * @return true if the event is registered or reserved, false otherwise.
     */
    boolean isEventRegistered(String eventName);

    /**
     * Subscribes a listener to a specific event.
     *
     * @param eventName  the name of the event to subscribe to.
     * @param listener   the listener to notify when the event is triggered.
     */
    void subscribeToEvent(String eventName, EventListener listener);

    /**
     * Triggers an event, notifying all subscribed listeners.
     *
     * @param eventName  the name of the event to trigger.
     * @param message    the message to send to listeners.
     */
    void triggerEvent(String eventName, String message);

    /**
     * Triggers an event, notifying all subscribed listeners.
     *
     * @param message    the message to send as response to listeners.
     */
    /*void respond(String message);*/

    /**
     * Retrieves all registered events and their descriptions, excluding reserved events.
     *
     * @return a map of event names to descriptions.
     */
    Map<String, String> getAvailableEvents();

    /**
     * Interface for event listeners.
     */
    interface EventListener {
        void onEvent(String eventName, MessageContainer message);
    }
}
