package com.skripsi.backend_api.dto.kategori.response;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class KategoriRes {
    private Long id;
    private String nama;
    private String penanda;
    private String keterangan;
    private LocalDateTime createdAt;
}