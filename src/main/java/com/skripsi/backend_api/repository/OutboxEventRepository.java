package com.skripsi.backend_api.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.skripsi.backend_api.entity.OutboxEvent;

public interface OutboxEventRepository extends JpaRepository<OutboxEvent, Long> {
    
    /**
     * Ambil batch event PENDING berurutan dan kunci barisnya. SKIP LOCKED (MySQL 8+) membuat
     * instance lain yang polling bersamaan melewati baris yang sedang diproses, bukan menunggu.
     * Harus dipanggil di dalam transaksi (dengan isolasi READ COMMITTED, lihat OutboxPublisher).
     */
    @Query(value = """
            SELECT * FROM outbox_event
            WHERE status = 'PENDING'
            ORDER BY id
            LIMIT :limit
            FOR UPDATE SKIP LOCKED
            """, nativeQuery = true)
    List<OutboxEvent> lockNextPending(@Param("limit") int limit);

    @Modifying
    @Query(value = "DELETE FROM outbox_event WHERE status = 'SENT' AND sent_at < :before LIMIT :limit",
            nativeQuery = true)
    int deleteSentBefore(@Param("before") LocalDateTime before, @Param("limit") int limit);
}
