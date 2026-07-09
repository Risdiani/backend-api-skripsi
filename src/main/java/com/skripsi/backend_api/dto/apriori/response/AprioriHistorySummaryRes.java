package com.skripsi.backend_api.dto.apriori.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
public class AprioriHistorySummaryRes {
    private Long id;
    private String namaProses;
    private Double minSupport;
    private Double minConfidence;
    private LocalDate tglMulai;
    private LocalDate tglSelesai;
    private Integer totalTransaksi;
    private String status;
    private String executedBy;
    private LocalDateTime createdAt;
}
