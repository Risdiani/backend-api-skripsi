package com.skripsi.backend_api.entity;

import java.time.LocalDateTime;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "outbox_event")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OutboxEvent {
    public enum Status { PENDING, SENT, FAILED }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "event_id", nullable = false, updatable = false, length = 36)
    private String eventId;

    @Column(name = "aggregate_type", nullable = false, updatable = false, length = 50)
    private String aggregateType;

    @Column(name = "aggregate_id", nullable = false, updatable = false, length = 50)
    private String aggregateId;

    @Column(name = "event_type", nullable = false, updatable = false, length = 50)
    private String eventType;

    @Column(nullable = false, updatable = false, length = 100)
    private String topic;

    @Column(nullable = false, updatable = false, columnDefinition = "json")
    private String payload;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    @Builder.Default
    private Status status = Status.PENDING;

    @Column(name = "retry_count", nullable = false)
    private int retryCount;

    @Column(name = "last_error", length = 500)
    private String lastError;

    // diisi default DB (CURRENT_TIMESTAMP(3))
    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "sent_at")
    private LocalDateTime sentAt;

    /** Constructor ringkas untuk KafkaOutboxService.append() */
    public OutboxEvent(String eventId, String aggregateType, String aggregateId,
                       String eventType, String topic, String payload) {
        this.eventId       = eventId;
        this.aggregateType = aggregateType;
        this.aggregateId   = aggregateId;
        this.eventType     = eventType;
        this.topic         = topic;
        this.payload       = payload;
        this.status        = Status.PENDING;
        this.retryCount    = 0;
    }

    /** Dipanggil KafkaPublisher setelah berhasil kirim ke broker. */
    public void markSent() {
        this.status  = Status.SENT;
        this.sentAt  = LocalDateTime.now();
        this.lastError = null;
    }

    /**
     * Dipanggil KafkaPublisher saat pengiriman gagal.
     * @param errorMsg pesan error dari exception
     * @param permanent true = error permanen (mis. RecordTooLargeException) → langsung FAILED,
     *                  false = error sementara (timeout, broker down) → tetap PENDING, retryCount naik
     */
    public void recordFailure(String errorMsg, boolean permanent) {
        this.lastError  = errorMsg != null && errorMsg.length() > 500
                          ? errorMsg.substring(0, 500) : errorMsg;
        this.retryCount++;
        if (permanent) {
            this.status = Status.FAILED;
        }
        // kalau transient, status tetap PENDING → akan dicoba lagi di polling berikutnya
    }
}

