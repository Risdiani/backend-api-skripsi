package com.skripsi.backend_api.dto.kategori.request;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class KategoriReq {
    private String nama;
    private String penanda;
    private String keterangan;
}
