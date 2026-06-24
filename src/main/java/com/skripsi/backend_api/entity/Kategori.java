package com.skripsi.backend_api.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(
    name = "kategori",
    uniqueConstraints = {
        @UniqueConstraint(name = "uq_kategori_nama", columnNames = "nama"),
        @UniqueConstraint(name = "uq_kategori_penanda", columnNames = "penanda")
    },
    indexes = {
        @Index(name = "idx_kategori_nama", columnList = "nama"),
        @Index(name = "idx_kategori_penanda", columnList = "penanda")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Kategori {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String nama;

    @Column(nullable = false, length = 50)
    private String penanda;

    @Column(length = 255)
    private String keterangan;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @OneToMany(mappedBy = "kategori", fetch = FetchType.LAZY)
    @Builder.Default
    private List<Product> produk = new ArrayList<>();
}