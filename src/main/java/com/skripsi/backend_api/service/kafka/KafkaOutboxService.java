package com.skripsi.backend_api.service.kafka;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.skripsi.backend_api.entity.OutboxEvent;
import com.skripsi.backend_api.repository.OutboxEventRepository;
import com.skripsi.backend_api.utils.KafkaEnvelope;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j 
@RequiredArgsConstructor 
public class KafkaOutboxService {
    private static final ZoneId ZONE = ZoneId.of("Asia/Jakarta");
    private static final String SOURCE = "main-app";

    private final OutboxEventRepository repository;
    private final ObjectMapper objectMapper;

    /**
     * MANDATORY: method ini sengaja menolak dipanggil di luar transaksi. Event outbox harus
     * di-commit atau di-rollback bersama perubahan datanya; kalau tidak, jaminan outbox hilang.
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public void append(String topic, String aggregateType, Object aggregateId,
                       String eventType, int eventVersion, Object data) {
        String eventId = UUID.randomUUID().toString();
        KafkaEnvelope<Object> envelope = new KafkaEnvelope<>(
                eventId, eventType, eventVersion, OffsetDateTime.now(ZONE), SOURCE, data);
        try {
            String json = objectMapper.writeValueAsString(envelope);
            repository.save(new OutboxEvent(eventId, aggregateType, String.valueOf(aggregateId),
                    eventType, topic, json));
        } catch (JsonProcessingException e) {
            // RuntimeException => transaksi bisnis ikut rollback (lebih baik gagal keras daripada event hilang diam-diam)
            throw new IllegalStateException("Gagal serialisasi event " + eventType, e);
        }
    }
}
