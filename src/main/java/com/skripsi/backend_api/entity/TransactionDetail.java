package com.skripsi.backend_api.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
 
import java.math.BigDecimal;
import java.time.LocalDateTime;
 
@Entity
@Table(name = "transaksi_detail")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TransactionDetail {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
 
    @Column(nullable = false)
    @Builder.Default
    private Integer jumlah = 1;
 
    @Column(name = "harga_satuan", nullable = false, precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal hargaSatuan = BigDecimal.ZERO;
 
    @Column(nullable = false, precision = 14, scale = 2)
    @Builder.Default
    private BigDecimal subtotal = BigDecimal.ZERO;
 
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
 
    // ── Relasi ke Transaksi (Many-to-One) ────────────────────
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "transaksi_id", nullable = false)
    private Transaction transaksi;
 
    // ── Relasi ke Produk (Many-to-One) ───────────────────────
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "produk_id", nullable = false)
    private Product produk;
 
    // ── Helper: hitung subtotal otomatis ─────────────────────
    // public void hitungSubtotal() {
    //     if (hargaSatuan != null && jumlah != null) {
    //         this.subtotal = hargaSatuan.multiply(BigDecimal.valueOf(jumlah));
    //     }
    // }
}
