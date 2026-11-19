package com.skripsi.backend_api.dto.kafka;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;

@Data 
@RequiredArgsConstructor 
@AllArgsConstructor 
public class KategoriDto {
    private Long kategoriId;
    private String nama;
    private String penanda;
}
