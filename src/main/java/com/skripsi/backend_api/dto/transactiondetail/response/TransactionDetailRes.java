package com.skripsi.backend_api.dto.transactiondetail.response;

import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class TransactionDetailRes {
    private Long id;
    private Long produkId;
    private String kodeProduk;
    private String namaProduk;
    private Integer jumlah;
    private BigDecimal hargaSatuan;
    private BigDecimal subtotal;
    private LocalDateTime createdAt;
}
