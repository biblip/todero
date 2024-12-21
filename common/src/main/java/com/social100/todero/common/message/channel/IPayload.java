package com.social100.todero.common.message.channel;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.social100.todero.common.message.channel.impl.ControlPayload;
import com.social100.todero.common.message.channel.impl.EventPayload;
import com.social100.todero.common.message.channel.impl.HiddenDataPayload;
import com.social100.todero.common.message.channel.impl.NotificationPayload;
import com.social100.todero.common.message.channel.impl.PublicDataPayload;

@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME, // Include a type field in JSON
        property = "type"          // Use "type" to distinguish payloads
)
@JsonSubTypes({
        @JsonSubTypes.Type(value = PublicDataPayload.class, name = "PUBLIC_DATA"),
        @JsonSubTypes.Type(value = HiddenDataPayload.class, name = "HIDDEN_DATA"),
        @JsonSubTypes.Type(value = NotificationPayload.class, name = "NOTIFICATION"),
        @JsonSubTypes.Type(value = EventPayload.class, name = "EVENT"),
        @JsonSubTypes.Type(value = ControlPayload.class, name = "CONTROL")
})
public interface IPayload {
}
