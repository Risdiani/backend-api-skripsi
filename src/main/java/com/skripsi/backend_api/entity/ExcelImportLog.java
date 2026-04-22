package com.skripsi.backend_api.entity;

import com.skripsi.backend_api.utils.Status;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
 
import java.time.LocalDateTime;
 
@Entity
@Table(name = "excel_import_log")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExcelImportLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
 
    @Column(name = "file_name", nullable = false, length = 255)
    private String fileName;
 
    // ukuran file dalam bytes
    @Column(name = "file_size")
    private Long fileSize;
 
    // total baris data di Excel (tidak termasuk header)
    @Column(name = "total_rows")
    private Integer totalRows;
 
    @Column(name = "rows_success")
    @Builder.Default
    private Integer rowsSuccess = 0;
 
    @Column(name = "rows_failed")
    @Builder.Default
    private Integer rowsFailed = 0;
 
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 15)
    @Builder.Default
    private Status status = Status.PROCESSING;
 
    // detail error jika ada baris yang gagal
    @Column(name = "error_detail", columnDefinition = "TEXT")
    private String errorDetail;
 
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
 
    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
 
    // ── Relasi ke User ────────────────────────────────────────
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "imported_by", referencedColumnName = "id")
    private User importedBy;
 
    // ── Enum Status ───────────────────────────────────────────
    // public enum Status {
    //     processing, success, partial, failed
    // }
}
