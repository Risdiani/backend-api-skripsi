package com.skripsi.backend_api.controller;

import com.skripsi.backend_api.dto.BaseResponse;
import com.skripsi.backend_api.dto.excelimportlog.response.ExcelImportLogRes;
import com.skripsi.backend_api.dto.transaction.request.TransactionReq;
import com.skripsi.backend_api.dto.transaction.response.TransactionRes;
import com.skripsi.backend_api.service.transaction.TransactionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/transactions")
@RequiredArgsConstructor
public class TransactionController {

    private final TransactionService transactionService;

    @GetMapping
    public ResponseEntity<BaseResponse<Object>> getAllTransactions() {
        List<TransactionRes> data = transactionService.findAll();
        return ResponseEntity.ok(BaseResponse.ok("Berhasil mengambil daftar transaksi", data));
    }

    @GetMapping("/{id}")
    public ResponseEntity<BaseResponse<Object>> getTransactionById(@PathVariable Long id) {
        TransactionRes data = transactionService.findById(id);
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
