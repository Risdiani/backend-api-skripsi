package com.skripsi.backend_api.dto.apriori.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AprioriStepRes<T> {
    private Long processId;
    private String namaProses;
    private String status;
    private Integer totalTransaksi;
    private T data;
}

