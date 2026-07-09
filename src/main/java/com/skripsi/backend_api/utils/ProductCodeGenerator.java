package com.skripsi.backend_api.utils;

import org.springframework.stereotype.Component;

import com.skripsi.backend_api.repository.ProductRepository;
import com.skripsi.backend_api.entity.Product;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class ProductCodeGenerator {

    private final ProductRepository repository;

    // Utility method untuk generate kodeProduk otomatis berformat OBT + 4 digit
    // sequence (e.g. OBT0001, OBT1000)
    public String generate() {
        String prefix = "OBT";

        // Cari produk dengan kode teratas/terakhir yang memiliki prefix "OBT"
        String lastCode = repository
                .findTopByKodeProdukStartingWithOrderByKodeProdukDesc(prefix)
                .map(Product::getKodeProduk)
                .orElse(null);
        int nextSequence = 1;
        if (lastCode != null && lastCode.length() > prefix.length()) {
            String lastSequencePart = lastCode.substring(prefix.length());
            try {
                nextSequence = Integer.parseInt(lastSequencePart) + 1;
            } catch (NumberFormatException ignored) {
                // Jika format suffix di DB tidak valid angka, fallback ke 1
            }
        }
        // Format sequence dengan zero-padding minimal 4 digit (e.g., 0001, 1000, 1500)
        return prefix + String.format("%04d", nextSequence);
    }
}
