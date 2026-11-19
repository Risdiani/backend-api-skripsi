package com.skripsi.backend_api.dto.kafka;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TransactionCancelledDto {
    private Long transaksiId;
    private String kodeTransaksi;
    private LocalDate tanggal;
    private String alasan;
}
