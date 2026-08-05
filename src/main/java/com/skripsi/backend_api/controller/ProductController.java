package com.skripsi.backend_api.controller;

import com.skripsi.backend_api.dto.BaseResponse;
import com.skripsi.backend_api.dto.excelimportlog.response.ExcelImportLogRes;
import com.skripsi.backend_api.dto.product.request.AddStokReq;
import com.skripsi.backend_api.dto.product.request.ProductReq;
import com.skripsi.backend_api.dto.product.response.ProductRes;
import com.skripsi.backend_api.service.product.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.http.MediaType;
import org.springframework.data.domain.Page;

import java.util.List;

@RestController
@RequestMapping("/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    // Endpoint Utama GET /products (Paginated, filter: nama & kategori)
    @GetMapping
    public ResponseEntity<BaseResponse<Object>> getAll(
            @RequestParam(required = false) String nama,
            @RequestParam(required = false) String kategori,
            @RequestParam(defaultValue = "0") Integer page,
            @RequestParam(defaultValue = "10") Integer size) {
        Page<ProductRes> data = productService.findAllPage(nama, kategori, page, size);
        return ResponseEntity.ok(BaseResponse.page("Berhasil mengambil daftar produk", data));
    }

    // Endpoint GET /products/list (Non-paginated, filter: nama opsional, digunakan
    // untuk form dropdown/pemilihan transaksi)
    @GetMapping("/list")
    public ResponseEntity<BaseResponse<Object>> getList(
            @RequestParam(required = false) String nama) {
        List<ProductRes> data = productService.getList(nama);
        return ResponseEntity.ok(BaseResponse.ok(data));
    }

    @GetMapping("/{kodeProduk}")
    public ResponseEntity<BaseResponse<Object>> detail(@PathVariable String kodeProduk) {
        ProductRes data = productService.findByKodeProduk(kodeProduk);
        return ResponseEntity.ok(BaseResponse.ok(data));
    }

    @PostMapping
    public ResponseEntity<BaseResponse<Object>> create(@RequestBody ProductReq req) {
        ProductRes data = productService.create(req);
        return ResponseEntity.ok(BaseResponse.ok("Product created", data));
    }

    // Endpoint Tambah Stok Obat (via Request Body)
    @PostMapping("/add-stock")
    public ResponseEntity<BaseResponse<Object>> addStock(@RequestBody AddStokReq req) {
        ProductRes data = productService.addStock(req);
        return ResponseEntity.ok(BaseResponse.ok("Stok obat berhasil ditambahkan", data));
    }

    @PutMapping("/{id}")
    public ResponseEntity<BaseResponse<Object>> update(@PathVariable Long id, @RequestBody ProductReq req) {
        ProductRes data = productService.update(id, req);
        return ResponseEntity.ok(BaseResponse.ok("Product updated", data));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<BaseResponse<Object>> delete(@PathVariable Long id) {
        productService.delete(id);
        return ResponseEntity.ok(BaseResponse.create(200, true, "Product deleted", null));
    }

    @PostMapping(value = "/import-excel", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<BaseResponse<ExcelImportLogRes>> importExcel(@RequestParam("file") MultipartFile file) {
        ExcelImportLogRes data = productService.importFromExcel(file);
        return ResponseEntity.ok(BaseResponse.ok("Excel import processed", data));
    }
}
