package com.social100.todero.common.message.channel;

import com.social100.todero.common.message.channel.impl.ControlPayload;
import com.social100.todero.common.message.channel.impl.EventPayload;
import com.social100.todero.common.message.channel.impl.HiddenDataPayload;
import com.social100.todero.common.message.channel.impl.NotificationPayload;
import com.social100.todero.common.message.channel.impl.PublicDataPayload;
import lombok.Getter;

@Getter
public enum ChannelType {
    PUBLIC_DATA(PublicDataPayload.class),
    HIDDEN_DATA(HiddenDataPayload.class),
    NOTIFICATION(NotificationPayload.class),
    EVENT(EventPayload.class),
    CONTROL(ControlPayload.class);

    private final Class<? extends IPayload> payloadType;

    ChannelType(Class<? extends IPayload> payloadType) {
        this.payloadType = payloadType;
    }

}