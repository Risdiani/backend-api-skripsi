package com.skripsi.backend_api.dto.apriori.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AprioriHistoryRuleRes {
    private Integer itemsetLevel;
    private String antecedent;
    private String consequent;
    private Integer supportCountXy;
    private Integer supportCountX;
    private Integer supportCountY;
    private Double support;
    private Double confidence;
    private Double lift;
    private Boolean lolosFilter;
    private String korelasi;
    private String keterangan;
}
