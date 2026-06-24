package com.skripsi.backend_api.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(
    name = "stok_mutasi",
    indexes = {
        @Index(name = "idx_sm_produk", columnList = "produk_id"),
        @Index(name = "idx_sm_tipe", columnList = "tipe"),
        @Index(name = "idx_sm_created_at", columnList = "created_at"),
        @Index(name = "idx_sm_transaksi_detail", columnList = "transaksi_detail_id")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StokMutasi {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "produk_id", nullable = false)
    private Product produk;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "transaksi_detail_id")
    private TransactionDetail transaksiDetail;

    @Column(nullable = false, length = 50)
    private String tipe;

    @Column(nullable = false)
    private Integer qty;

    @Column(name = "stok_sebelum", nullable = false)
    private Integer stokSebelum;

    @Column(name = "stok_sesudah", nullable = false)
    private Integer stokSesudah;

    @Column(length = 255)
    private String keterangan;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}