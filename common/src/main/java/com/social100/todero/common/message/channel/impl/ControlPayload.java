package com.social100.todero.common.message.channel.impl;

import com.social100.todero.common.message.channel.IPayload;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class ControlPayload implements IPayload {
    /**
     * The control message text/content.
     */
    private final String message;

    public ControlPayload() {
        message = null;
    }
}
