package com.skripsi.backend_api.controller;

import com.skripsi.backend_api.dto.BaseResponse;
import com.skripsi.backend_api.dto.stokmutasi.response.StokMutasiRes;
import com.skripsi.backend_api.service.stokmutasi.StokMutasiService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/stok-mutasi")
@RequiredArgsConstructor
public class StokMutasiController {

    private final StokMutasiService stokMutasiService;

    /**
     * Endpoint GET /stok-mutasi
     * Paginated list semua mutasi stok dengan filter opsional tipe (LIKE) &
     * namaProduk (LIKE)
     */
    @GetMapping
    public ResponseEntity<BaseResponse<Object>> getAllPage(
            @RequestParam(required = false) String tipe,
            @RequestParam(required = false) String namaProduk,
            @RequestParam(defaultValue = "0") Integer page,
            @RequestParam(defaultValue = "10") Integer size) {
        Page<StokMutasiRes> data = stokMutasiService.findAllPage(tipe, namaProduk, page, size);
        return ResponseEntity.ok(BaseResponse.page("Berhasil mengambil data mutasi stok", data));
    }

    /**
     * Endpoint GET /stok-mutasi/detail
     * Paginated list mutasi stok berdasarkan kodeProduk (REQUIRED) dan tipe
     * (OPTIONAL LIKE)
     */
    @GetMapping("/detail")
    public ResponseEntity<BaseResponse<Object>> getDetailPage(
            @RequestParam String kodeProduk,
            @RequestParam(required = false) String tipe,
            @RequestParam(defaultValue = "0") Integer page,
            @RequestParam(defaultValue = "10") Integer size) {
        Page<StokMutasiRes> data = stokMutasiService.findDetailPage(kodeProduk, tipe, page, size);
        return ResponseEntity.ok(BaseResponse.page("Berhasil mengambil detail mutasi stok produk", data));
    }
}
