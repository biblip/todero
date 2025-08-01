package com.social100.todero.common.message.channel.impl;

import com.social100.todero.common.message.channel.IPayload;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class NotificationPayload implements IPayload {
    /**
     * The notification message text/content.
     */
    private final String message;

    public NotificationPayload() {
        message = null;
    }
}
