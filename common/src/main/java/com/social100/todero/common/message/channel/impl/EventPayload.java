package com.social100.todero.common.message.channel.impl;

import com.social100.todero.common.message.channel.IPayload;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class EventPayload implements IPayload {
    /**
     * The event name, e.g., "UserLoggedInEvent".
     */
    private final String name;

    /**
     * The message describing the event details.
     */
    private final String message;

    public EventPayload() {
        name = null;
        message = null;
    }
}
