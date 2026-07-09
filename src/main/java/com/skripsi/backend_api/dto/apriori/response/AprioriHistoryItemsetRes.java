package com.skripsi.backend_api.dto.apriori.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AprioriHistoryItemsetRes {
    private Integer level;
    private String item;
    private Integer supportCount;
    private Double support;
    private String keterangan;
}
