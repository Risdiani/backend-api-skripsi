package com.skripsi.backend_api.repository;

import com.skripsi.backend_api.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ProductRepository extends JpaRepository<Product, Long> {
    Optional<Product> findByKodeProduk(String kodeProduk);
    boolean existsByKodeProduk(String kodeProduk);
}
