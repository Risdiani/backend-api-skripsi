package com.skripsi.backend_api.dto.stokmutasi.response;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StokMutasiRes {
    private Long id;
    private String kodeProduk;
    private String namaProduk;
    private BigDecimal harga;
    private String satuan;
    private String namaKategori;
    private String kodeTransaksi;
    private LocalDate tanggalTransaksi;
    private String tipe;
    private Integer qty;
    private Integer stokSebelum;
    private Integer stokSesudah;
    private String keterangan;
    private LocalDateTime createdAt;
}
