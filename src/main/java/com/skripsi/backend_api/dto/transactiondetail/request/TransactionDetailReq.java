package com.skripsi.backend_api.dto.transactiondetail.request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TransactionDetailReq {
    private Long id;
    private String kodeProduk;
    private Integer jumlah;
    private BigDecimal hargaSatuan;
}
