package com.skripsi.backend_api.dto.kafka;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Data 
@RequiredArgsConstructor 
@AllArgsConstructor 
public class ProdukDto {
    private Long produkId;
    private String kodeProduk;
    private String namaProduk;
    private Long kategoriId;
    private String satuan;
    private BigDecimal harga;
    private Boolean isActive;
    private OffsetDateTime updatedAt;
}
