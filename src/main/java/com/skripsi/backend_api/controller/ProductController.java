package com.skripsi.backend_api.controller;

import com.skripsi.backend_api.dto.BaseResponse;
import com.skripsi.backend_api.dto.excelimportlog.response.ExcelImportLogRes;
import com.skripsi.backend_api.dto.product.request.ProductReq;
import com.skripsi.backend_api.dto.product.response.ProductRes;
import com.skripsi.backend_api.service.product.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.http.MediaType;

import java.util.List;

@RestController
@RequestMapping("/products")
@RequiredArgsConstructor
public class ProductController {
    
    private final ProductService productService;

    @GetMapping
    public ResponseEntity<BaseResponse<Object>> list() {
        List<ProductRes> data = productService.findAll();
        return ResponseEntity.ok(BaseResponse.ok(data));
    }

    @GetMapping("/{id}")
    public ResponseEntity<BaseResponse<Object>> detail(@PathVariable Long id) {
        ProductRes data = productService.findById(id);
        return ResponseEntity.ok(BaseResponse.ok(data));
    }

    @PostMapping
    public ResponseEntity<BaseResponse<Object>> create(@RequestBody ProductReq req) {
        ProductRes data = productService.create(req);
        return ResponseEntity.ok(BaseResponse.ok("Product created", data));
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
