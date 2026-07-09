package com.skripsi.backend_api.dto.apriori.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class AprioriHistoryDetailRes {
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
    private List<AprioriHistoryItemsetRes> frequentItemsets;
    private List<AprioriHistoryRuleRes> associationRules;
}
