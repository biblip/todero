package com.social100.todero.common.channels.process;

import com.social100.todero.common.message.channel.IPayload;

public interface ChannelHandler<T extends IPayload> {
    void process(T payload);
    Class<T> getPayloadType();
}
