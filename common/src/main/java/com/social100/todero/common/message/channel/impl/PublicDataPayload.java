package com.social100.todero.common.message.channel.impl;

import com.social100.todero.common.message.channel.IPayload;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class PublicDataPayload implements IPayload {
    /**
     * The public message for the public_data channel, e.g., commands or responses.
     */
    private final String message;

    public PublicDataPayload() {
        message = null;
    }
}
