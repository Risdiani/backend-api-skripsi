package com.skripsi.backend_api.dto.kafka;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TransactionCreatedDto {
    private Long transaksiId;
    private String kodeTransaksi;
    private LocalDate tanggal;
    private BigDecimal totalHarga;
    private String sumberData;
    private Long importedBy;
    private OffsetDateTime createdAt;
    private List<TransactionItemDto> items;
}
