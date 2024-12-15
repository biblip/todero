package com.social100.todero.common.message.channel.impl;

import com.social100.todero.common.message.channel.IPayload;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class HiddenDataPayload implements IPayload {
    /**
     * The hidden message text/content.
     */
    private final String message;
}
