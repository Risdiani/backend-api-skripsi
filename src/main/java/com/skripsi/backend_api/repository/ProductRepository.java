package com.skripsi.backend_api.repository;

import com.skripsi.backend_api.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;

public interface ProductRepository extends JpaRepository<Product, Long>, JpaSpecificationExecutor<Product> {
    Optional<Product> findByKodeProduk(String kodeProduk);

    boolean existsByKodeProduk(String kodeProduk);

    // Query untuk mengambil kode produk terakhir dengan awalan tertentu untuk
    // generate sequence
    Optional<Product> findTopByKodeProdukStartingWithOrderByKodeProdukDesc(String prefix);
}
