package com.skripsi.backend_api.config;

import java.time.Duration;
import org.apache.kafka.common.config.TopicConfig;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;
import org.apache.kafka.clients.admin.NewTopic;

/**
 * Topic dibuat otomatis oleh KafkaAdmin saat aplikasi start (idempotent kalau topic sudah ada).
 * Replicas = 1 karena broker lokal tunggal; di cluster produksi gunakan 3 + min.insync.replicas=2.
 */
@Configuration
public class KafkaTopicConfig {
    // Event transaksi: key = transaksiId
    public static final String TOPIC_TRANSAKSI   = "larasati.transaksi.v1";
    // Event mutasi stok: key = produkId (urutan stok per produk harus terjaga)
    public static final String TOPIC_STOK_MUTASI = "larasati.stok-mutasi.v1";
    // Data master: compacted, key = id entitas -> Kafka hanya menyimpan nilai terakhir per key
    public static final String TOPIC_PRODUK      = "larasati.produk.v1";
    public static final String TOPIC_KATEGORI    = "larasati.kategori.v1";
    public static final String TOPIC_USER        = "larasati.user.v1";

    @Bean
    NewTopic transaksiTopic() {
        return TopicBuilder.name(TOPIC_TRANSAKSI)
                .partitions(3).replicas(1)
                // retensi lebih panjang dari default 7 hari supaya analytics yang mati lama masih bisa mengejar
                .config(TopicConfig.RETENTION_MS_CONFIG, String.valueOf(Duration.ofDays(30).toMillis()))
                .build();
    }

    @Bean
    NewTopic stokMutasiTopic() {
        return TopicBuilder.name(TOPIC_STOK_MUTASI)
                .partitions(3).replicas(1)
                .config(TopicConfig.RETENTION_MS_CONFIG, String.valueOf(Duration.ofDays(30).toMillis()))
                .build();
    }

    @Bean
    NewTopic produkTopic() {
        return TopicBuilder.name(TOPIC_PRODUK).partitions(1).replicas(1).compact().build();
    }

    @Bean
    NewTopic kategoriTopic() {
        return TopicBuilder.name(TOPIC_KATEGORI).partitions(1).replicas(1).compact().build();
    }

    @Bean
    NewTopic userTopic() {
        return TopicBuilder.name(TOPIC_USER).partitions(1).replicas(1).compact().build();
    }
}
