package com.skripsi.backend_api.entity;

import com.skripsi.backend_api.utils.Korelasi;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
 
import java.time.LocalDateTime;
 
@Entity
@Table(name = "association_rule")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AssociationRule {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
 
    // dari level 2-itemset atau 3-itemset
    @Column(name = "itemset_level", nullable = false)
    private Integer itemsetLevel;
 
    // X: sisi kiri aturan, contoh: "Sanmol"
    @Column(nullable = false, length = 500)
    private String antecedent;
 
    // Y: sisi kanan aturan, contoh: "Decolgen"
    @Column(nullable = false, length = 500)
    private String consequent;
 
    // ── Nilai perhitungan ─────────────────────────────────────
 
    // σ(X ∪ Y)
    @Column(name = "support_count_xy", nullable = false)
    private Integer supportCountXy;
 
    // σ(X)
    @Column(name = "support_count_x", nullable = false)
    private Integer supportCountX;
 
    // σ(Y)
    @Column(name = "support_count_y", nullable = false)
    private Integer supportCountY;
 
    // s(X→Y) = σ(X∪Y) / N
    @Column(nullable = false)
    private Double support;
 
    // c(X→Y) = σ(X∪Y) / σ(X)
    @Column(nullable = false)
    private Double confidence;
 
    // lift = confidence / support(Y)
    @Column(nullable = false)
    private Double lift;
 
    // ── Hasil evaluasi ────────────────────────────────────────
 
    // true jika lolos min_support & min_confidence
    @Column(name = "lolos_filter", nullable = false)
    @Builder.Default
    private Boolean lolosFilter = false;
 
    @Enumerated(EnumType.STRING)
    @Column(length = 15)
    private Korelasi korelasi;
 
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
 
    // ── Relasi ke AprioriProcess ──────────────────────────────
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "process_id", nullable = false)
    private AprioriProcess process;
 
    // // ── Enum Korelasi ─────────────────────────────────────────
    // // positif  : lift > 1  → A dan B saling mendorong
    // // negatif  : lift < 1  → A dan B saling menghambat
    // // independen: lift = 1 → tidak ada hubungan
    // public enum Korelasi {
    //     positif, negatif, independen
    // }
}
