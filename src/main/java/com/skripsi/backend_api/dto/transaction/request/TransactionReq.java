package com.skripsi.backend_api.dto.transaction.request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import com.skripsi.backend_api.dto.transactiondetail.request.TransactionDetailReq;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * DTO for creating or updating a Transaction.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TransactionReq {
    private String kodeTransaksi;
    private LocalDate transactionDate;
    private String catatan;
    private List<TransactionDetailReq> details;
}
