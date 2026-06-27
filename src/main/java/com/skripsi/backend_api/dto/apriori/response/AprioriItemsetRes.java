package com.skripsi.backend_api.dto.apriori.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AprioriItemsetRes {
    private String item; // Bisa item tunggal "Sanmol" atau kombinasi "Sanmol, Bodrex"
    private Integer jumlah; // Jumlah kemunculan di transaksi
    private Double support; // Nilai persentase support
    private String keterangan; // "Lolos" atau "Tidak Lolos"
}
