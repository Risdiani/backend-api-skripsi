package com.skripsi.backend_api.utils;

import java.time.OffsetDateTime;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class KafkaEnvelope<T> {
    private String eventId;
    private String eventType;
    private int eventVersion;
    private OffsetDateTime occurredAt;
    private String source;
    private T data;
}
