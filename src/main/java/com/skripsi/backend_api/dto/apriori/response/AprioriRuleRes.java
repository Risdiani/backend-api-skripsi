package com.skripsi.backend_api.dto.apriori.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AprioriRuleRes {
    private String antecedent; // Item 1 (Jika beli ini...)
    private String consequent; // Item 2 (...maka beli ini)
    private Double confidence; // Nilai persentase confidence
    private String keterangan; // "Lolos" atau "Tidak Lolos"
}
