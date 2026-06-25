package com.skripsi.backend_api.dto.transaction.response;

import com.skripsi.backend_api.dto.transactiondetail.response.TransactionDetailRes;
import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class TransactionRes {
    private Long id;
    private String kodeTransaksi;
    private LocalDate transactionDate;
    private BigDecimal totalHarga;
    private String sumberData;
    private String importedBy;
    private String catatan;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<TransactionDetailRes> details;
}
