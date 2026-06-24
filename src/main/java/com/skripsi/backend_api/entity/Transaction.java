package com.skripsi.backend_api.entity;

import com.skripsi.backend_api.utils.SumberData;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(
    name = "transaksi",
    uniqueConstraints = {
        @UniqueConstraint(name = "uq_transaksi_kode", columnNames = "kode_transaksi")
    },
    indexes = {
        @Index(name = "idx_transaksi_date", columnList = "transaction_date"),
        @Index(name = "idx_transaksi_imported_by", columnList = "imported_by")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Transaction {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "kode_transaksi", nullable = false, unique = true, length = 30)
    private String kodeTransaksi;

    @Column(name = "transaction_date", nullable = false)
    private LocalDate transactionDate;

    @Column(name = "total_harga", nullable = false, precision = 14, scale = 2)
    @Builder.Default
    private BigDecimal totalHarga = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(name = "sumber_data", nullable = false, length = 20)
    @Builder.Default
    private SumberData sumberData = SumberData.manual;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "imported_by")
    private User importedBy;

    @Column(columnDefinition = "TEXT")
    private String catatan;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "transaksi", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<TransactionDetail> details = new ArrayList<>();

    public void addDetail(TransactionDetail detail) {
        details.add(detail);
        detail.setTransaksi(this);
    }

    public void removeDetail(TransactionDetail detail) {
        details.remove(detail);
        detail.setTransaksi(null);
    }
}