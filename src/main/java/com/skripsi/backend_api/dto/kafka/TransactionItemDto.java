package com.skripsi.backend_api.dto.kafka;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TransactionItemDto {
    
    private Long detailId;
    private Long produkId;
    private String kodeProduk;
    private String namaProduk;
    private Long kategoriId;
    private Integer jumlah;
    private BigDecimal hargaSatuan;
    private BigDecimal subTotal;
}
