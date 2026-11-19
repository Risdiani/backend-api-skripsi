package com.skripsi.backend_api.utils;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.errors.InvalidTopicException;
import org.apache.kafka.common.errors.RecordTooLargeException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import com.skripsi.backend_api.entity.OutboxEvent;
import com.skripsi.backend_api.repository.OutboxEventRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j 
@Component 
@RequiredArgsConstructor 
public class KafkaPublisher {
    private static final int CLEANUP_CHUNK = 1000;
    private final OutboxEventRepository repository;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final TransactionTemplate trx;

    @Value("${outbox.batch-size:100}")
    private int batchSize;

    @Value("${outbox.send-timeout-ms:20000}")
    private long sendTimeoutMs;

    @Value("${outbox.retention-days:7}")
    private int retentionDays;

    // fixedDelay: eksekusi berikutnya baru mulai setelah yang sebelumnya selesai (tidak tumpang tindih)
    @Scheduled(fixedDelayString = "${outbox.poll-interval-ms:1000}")
    public void poll() {
        int processed;
        do {
            Integer n = trx.execute(status -> publishBatch());
            processed = n == null ? 0 : n;
        } while (processed == batchSize);   // batch penuh: kemungkinan masih ada backlog, langsung lanjut
    }

    private int publishBatch() {
        List<OutboxEvent> batch = repository.lockNextPending(batchSize);
        int sent = 0;
        for (OutboxEvent event : batch) {
            try {
                ProducerRecord<String, String> record =
                        new ProducerRecord<>(event.getTopic(), event.getAggregateId(), event.getPayload());
                record.headers().add("event-id", event.getEventId().getBytes(StandardCharsets.UTF_8));
                record.headers().add("event-type", event.getEventType().getBytes(StandardCharsets.UTF_8));

                // Sinkron per event: menunggu ack broker sebelum menandai SENT. Sederhana dan menjaga urutan.
                kafkaTemplate.send(record).get(sendTimeoutMs, TimeUnit.MILLISECONDS);
                event.markSent();
                sent++;
            } catch (Exception ex) {
                if (ex instanceof InterruptedException) {
                    Thread.currentThread().interrupt();
                }
                boolean permanent = isPermanent(ex);
                event.recordFailure(ex.getMessage(), permanent);
                log.warn("Gagal mengirim outbox id={} type={} permanent={}: {}",
                        event.getId(), event.getEventType(), permanent, ex.toString());
                if (!permanent) {
                    break;   // gangguan sementara (broker down, timeout): berhenti, ulangi di polling berikutnya
                }
                // permanen: tandai FAILED dan lanjut supaya satu pesan rusak tidak memblokir seluruh antrean
            }
        }
        return sent;
    }

    private static boolean isPermanent(Exception ex) {
        Throwable root = (ex instanceof ExecutionException && ex.getCause() != null) ? ex.getCause() : ex;
        return root instanceof RecordTooLargeException || root instanceof InvalidTopicException;
    }

    @Scheduled(cron = "${outbox.cleanup-cron:0 0 3 * * *}", zone = "Asia/Jakarta")
    public void cleanup() {
        LocalDateTime before = LocalDateTime.now().minusDays(retentionDays);
        int deleted;
        do {
            Integer n = trx.execute(status -> repository.deleteSentBefore(before, CLEANUP_CHUNK));
            deleted = n == null ? 0 : n;
        } while (deleted == CLEANUP_CHUNK);
    }
}
