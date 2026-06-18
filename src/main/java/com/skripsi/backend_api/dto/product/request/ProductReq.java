package com.skripsi.backend_api.dto.product.request;

import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductReq {
    private String kodeProduk;
    private String namaProduk;
    private String kategori;
    private String satuan;
    private BigDecimal harga;
    private Boolean isActive;
}