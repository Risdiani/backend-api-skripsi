package com.skripsi.backend_api.controller;

import com.skripsi.backend_api.dto.BaseResponse;
import com.skripsi.backend_api.dto.kategori.request.KategoriReq;
import com.skripsi.backend_api.dto.kategori.response.KategoriRes;
import com.skripsi.backend_api.service.kategori.KategoriService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/categories")
@RequiredArgsConstructor
public class KategoriController {
    private final KategoriService kategoriService;

    @GetMapping
    public ResponseEntity<BaseResponse<Object>> list() {
        List<KategoriRes> data = kategoriService.findAll();
        return ResponseEntity.ok(BaseResponse.ok(data));
    }

    @GetMapping("/{id}")
    public ResponseEntity<BaseResponse<Object>> detail(@PathVariable Long id) {
        KategoriRes data = kategoriService.findById(id);
        return ResponseEntity.ok(BaseResponse.ok(data));
    }

    @PostMapping
    public ResponseEntity<BaseResponse<Object>> create(@RequestBody KategoriReq req) {
        KategoriRes data = kategoriService.create(req);
        return ResponseEntity.ok(BaseResponse.ok("Kategori created", data));
    }

    @PutMapping("/{id}")
    public ResponseEntity<BaseResponse<Object>> update(@PathVariable Long id, @RequestBody KategoriReq req) {
        KategoriRes data = kategoriService.update(id, req);
        return ResponseEntity.ok(BaseResponse.ok("Kategori updated", data));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<BaseResponse<Object>> delete(@PathVariable Long id) {
        kategoriService.delete(id);
        return ResponseEntity.ok(BaseResponse.create(200, true, "Kategori deleted", null));
    }
}
