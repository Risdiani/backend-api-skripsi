package com.skripsi.backend_api.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
 
import java.time.LocalDateTime;
 
@Entity
@Table(name = "frequent_itemset")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FrequentItemset {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
 
    // level 1 = 1-itemset, level 2 = 2-itemset, dst.
    @Column(nullable = false)
    private Integer level;
 
    // nama item dipisah koma, urut abjad. contoh: "Decolgen,Sanmol"
    @Column(nullable = false, length = 500)
    private String items;
 
    // σ(X): jumlah transaksi yang mengandung itemset ini
    @Column(name = "support_count", nullable = false)
    private Integer supportCount;
 
    // support = supportCount / totalTransaksi
    @Column(nullable = false)
    private Double support;
 
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
 
    // ── Relasi ke AprioriProcess ──────────────────────────────
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "process_id", nullable = false)
    private AprioriProcess process;
}
