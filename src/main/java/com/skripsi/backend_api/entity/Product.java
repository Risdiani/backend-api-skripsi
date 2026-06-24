package com.skripsi.backend_api.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(
    name = "produk",
    uniqueConstraints = {
        @UniqueConstraint(name = "uq_produk_kode", columnNames = "kode_produk")
    },
    indexes = {
        @Index(name = "idx_produk_nama", columnList = "nama_produk"),
        @Index(name = "idx_produk_kategori_id", columnList = "kategori_id")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Product {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "kode_produk", nullable = false, unique = true, length = 50)
    private String kodeProduk;

    @Column(name = "nama_produk", nullable = false, length = 255)
    private String namaProduk;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "kategori_id")
    private Kategori kategori;

    @Column(length = 50)
    private String satuan;

    @Column(precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal harga = BigDecimal.ZERO;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    @Column(name = "stok_tersedia", nullable = false)
    @Builder.Default
    private Integer stokTersedia = 0;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "produk", fetch = FetchType.LAZY)
    @Builder.Default
    private List<TransactionDetail> transaksiDetails = new ArrayList<>();
}