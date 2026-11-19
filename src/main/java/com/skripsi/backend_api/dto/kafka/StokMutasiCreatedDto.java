package com.skripsi.backend_api.dto.kafka;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.OffsetDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class StokMutasiCreatedDto {
    private Long mutasiId;
    private Long produkId;
    private Long transaksiDetailId;
    private String tipe;
    private Integer qty;
    private Integer stokSebelum;
    private Integer stokSesudah;
    private String keterangan;
    private OffsetDateTime createdAt;
}
