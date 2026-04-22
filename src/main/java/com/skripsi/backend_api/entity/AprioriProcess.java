package com.skripsi.backend_api.entity;

import com.skripsi.backend_api.utils.Status;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
 
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
 
@Entity
@Table(name = "apriori_process")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AprioriProcess {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
 
    @Column(name = "nama_proses", length = 255)
    private String namaProses;
 
    @Column(name = "min_support", nullable = false)
    private Double minSupport;
 
    @Column(name = "min_confidence", nullable = false)
    private Double minConfidence;
 
    @Column(name = "tgl_mulai")
    private LocalDate tglMulai;
 
    @Column(name = "tgl_selesai")
    private LocalDate tglSelesai;
 
    @Column(name = "total_transaksi")
    private Integer totalTransaksi;
 
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    @Builder.Default
    private Status status = Status.PENDING;
 
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
 
    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
 
    // ── Relasi ke User (siapa yang menjalankan proses) ────────
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "executed_by", referencedColumnName = "id")
    private User executedBy;
 
    // ── Relasi ke FrequentItemset ─────────────────────────────
    @OneToMany(mappedBy = "process", cascade = CascadeType.ALL,
               orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<FrequentItemset> frequentItemsets = new ArrayList<>();
 
    // ── Relasi ke AssociationRule ─────────────────────────────
    @OneToMany(mappedBy = "process", cascade = CascadeType.ALL,
               orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<AssociationRule> associationRules = new ArrayList<>();
 
    // ── Enum Status ───────────────────────────────────────────
    // public enum Status {
    //     pending, running, done, failed
    // }
}
