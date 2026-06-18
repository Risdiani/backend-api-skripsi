package com.skripsi.backend_api.dto.product.response;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductRes {
    private Long id;
    private String kodeProduk;
    private String namaProduk;
    private String kategori;
    private String satuan;
    private BigDecimal harga;
    private Boolean isActive;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
