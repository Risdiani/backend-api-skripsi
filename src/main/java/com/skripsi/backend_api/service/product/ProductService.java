package com.skripsi.backend_api.service.product;

import com.skripsi.backend_api.dto.product.request.ProductReq;
import com.skripsi.backend_api.dto.product.response.ProductRes;
import com.skripsi.backend_api.entity.Product;
import com.skripsi.backend_api.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;

    private static String normalizeCode(String value) {
        if (value == null) return null;
        return value.trim().toUpperCase();
    }

    private static String normalizeText(String value) {
        if (value == null) return null;
        return value.trim();
    }

    private ProductRes toResponse(Product p) {
        return ProductRes.builder()
                .id(p.getId())
                .kodeProduk(p.getKodeProduk())
                .namaProduk(p.getNamaProduk())
                .kategori(p.getKategori())
                .satuan(p.getSatuan())
                .harga(p.getHarga())
                .isActive(p.getIsActive())
                .createdAt(p.getCreatedAt())
                .updatedAt(p.getUpdatedAt())
                .build();
    }

    public List<ProductRes> findAll() {
        log.info("ProductService.findAll - fetching all products");
        List<ProductRes> result = productRepository.findAll().stream().map(this::toResponse).toList();
        log.info("ProductService.findAll - total={}", result.size());
        return result;
    }

    public ProductRes findById(Long id) {
        log.info("ProductService.findById - id={}", id);
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Product not found"));
        return toResponse(product);
    }

    public ProductRes create(ProductReq req) {
        String kode = normalizeCode(req == null ? null : req.getKodeProduk());
        String nama = normalizeText(req == null ? null : req.getNamaProduk());
        String kategori = normalizeText(req == null ? null : req.getKategori());
        String satuan = normalizeText(req == null ? null : req.getSatuan());
        BigDecimal harga = req == null ? null : req.getHarga();
        Boolean isActive = req == null ? null : req.getIsActive();

        log.info("ProductService.create - kode={}, nama={}", kode, nama);

        if (kode == null || kode.isBlank()) {
            throw new IllegalArgumentException("Kode produk is required");
        }
        if (nama == null || nama.isBlank()) {
            throw new IllegalArgumentException("Nama produk is required");
        }

        if (productRepository.existsByKodeProduk(kode)) {
            throw new IllegalArgumentException("Kode produk already used");
        }

        Product product = Product.builder()
                .kodeProduk(kode)
                .namaProduk(nama)
                .kategori(kategori)
                .satuan(satuan)
                .harga(harga == null ? BigDecimal.ZERO : harga)
                .isActive(isActive == null ? true : isActive)
                .build();

        Product saved = productRepository.save(product);
        log.info("ProductService.create - created id={}, kode={}", saved.getId(), saved.getKodeProduk());
        return toResponse(saved);
    }

    public ProductRes update(Long id, ProductReq req) {
        String kode = normalizeCode(req == null ? null : req.getKodeProduk());
        String nama = normalizeText(req == null ? null : req.getNamaProduk());
        String kategori = normalizeText(req == null ? null : req.getKategori());
        String satuan = normalizeText(req == null ? null : req.getSatuan());
        BigDecimal harga = req == null ? null : req.getHarga();
        Boolean isActive = req == null ? null : req.getIsActive();

        log.info("ProductService.update - id={}, kode={}, nama={}", id, kode, nama);

        Product product = productRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Product not found"));

        if (kode != null) {
            if (kode.isBlank()) {
                throw new IllegalArgumentException("Kode produk cannot be blank");
            }
            productRepository.findByKodeProduk(kode).ifPresent(existing -> {
                if (!existing.getId().equals(product.getId())) {
                    throw new IllegalArgumentException("Kode produk already used");
                }
            });
            product.setKodeProduk(kode);
        }

        if (nama != null) {
            if (nama.isBlank()) {
                throw new IllegalArgumentException("Nama produk cannot be blank");
            }
            product.setNamaProduk(nama);
        }

        if (kategori != null) {
            product.setKategori(kategori);
        }

        if (satuan != null) {
            product.setSatuan(satuan);
        }

        if (harga != null) {
            product.setHarga(harga);
        }

        if (isActive != null) {
            product.setIsActive(isActive);
        }

        Product saved = productRepository.save(product);
        log.info("ProductService.update - updated id={}, kode={}", saved.getId(), saved.getKodeProduk());
        return toResponse(saved);
    }

    public void delete(Long id) {
        log.info("ProductService.delete - id={}", id);

        if (!productRepository.existsById(id)) {
            throw new IllegalArgumentException("Product not found");
        }

        productRepository.deleteById(id);
        log.info("ProductService.delete - deleted id={}", id);
    }
}
