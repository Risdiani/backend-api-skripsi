package com.skripsi.backend_api.controller;

import com.skripsi.backend_api.dto.BaseResponse;
import com.skripsi.backend_api.dto.excelimportlog.response.ExcelImportLogRes;
import com.skripsi.backend_api.dto.transaction.request.TransactionReq;
import com.skripsi.backend_api.dto.transaction.response.TransactionRes;
import com.skripsi.backend_api.service.transaction.TransactionService;
import com.skripsi.backend_api.utils.PageResponse;

import lombok.RequiredArgsConstructor;

import org.springframework.data.web.PageableDefault;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/transactions")
@RequiredArgsConstructor
public class TransactionController {

    private final TransactionService transactionService;

    @GetMapping
    public ResponseEntity<BaseResponse<Object>> getAllTransactions(
            @RequestParam(required = false) LocalDate tglAwal,
            @RequestParam(required = false) LocalDate tglAkhir,
            @RequestParam(defaultValue = "0") Integer page,
            @RequestParam(defaultValue = "10") Integer size) {

        var data = transactionService.findAllPage(tglAwal, tglAkhir, page, size);
        return ResponseEntity.ok(BaseResponse.page("Berhasil mengambil daftar transaksi", data));
    }

    @GetMapping("/{kodeTransaksi}")
    public ResponseEntity<BaseResponse<Object>> getTransactionByKodeTransaksi(@PathVariable String kodeTransaksi) {
        TransactionRes data = transactionService.findByKodeTransaksi(kodeTransaksi);
        return ResponseEntity.ok(BaseResponse.ok("Berhasil mengambil detail transaksi", data));
    }

    @PostMapping(value = "/import-excel", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<BaseResponse<ExcelImportLogRes>> importExcel(
            @RequestParam("file") MultipartFile file) {
        ExcelImportLogRes data = transactionService.importFromExcel(file);
        return ResponseEntity.ok(BaseResponse.ok("Excel transaksi berhasil diproses", data));
    }

    @PostMapping("/create")
    public ResponseEntity<BaseResponse<Object>> createTransaction(@RequestBody TransactionReq req) {
        TransactionRes data = transactionService.createTransaction(req);
        return ResponseEntity.ok(BaseResponse.ok("Berhasil membuat transaksi baru", data));
    }

    @PostMapping("/preview")
    public ResponseEntity<BaseResponse<Object>> previewTransaction(@RequestBody TransactionReq req) {
        return ResponseEntity.ok(transactionService.previewTransaction(req));
    }

    @PutMapping("/update/{id}")
    public ResponseEntity<BaseResponse<Object>> updateTransaction(@PathVariable Long id,
            @RequestBody TransactionReq req) {
        Object data = transactionService.updateTransaction(id, req);
        return ResponseEntity.ok(BaseResponse.ok("Berhasil mengupdate transaksi", data));
    }

    @DeleteMapping("/delete/{id}")
    public ResponseEntity<BaseResponse<Object>> deleteTransaction(@PathVariable Long id) {
        transactionService.deleteTransaction(id);
        return ResponseEntity.ok(BaseResponse.ok("Berhasil menghapus transaksi"));
    }
}
