package com.skripsi.backend_api.dto.apriori.request;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import java.time.LocalDate;

@Data
public class AprioriReq {
    private Double minSupport; // Contoh: 20.0 untuk 20%
    private Double minConfidence; // Contoh: 50.0 untuk 50%

    @JsonFormat(pattern = "dd/MM/yyyy")
    private LocalDate startDate;

    @JsonFormat(pattern = "dd/MM/yyyy")
    private LocalDate endDate;
}
