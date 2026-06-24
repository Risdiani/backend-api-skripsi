package com.skripsi.backend_api.repository;

import com.skripsi.backend_api.entity.Kategori;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface KategoriRepository extends JpaRepository<Kategori, Long> {
    boolean existsByNama(String nama);
    boolean existsByPenanda(String penanda);
    Optional<Kategori> findByNama(String nama);
    Optional<Kategori> findByPenanda(String penanda);
}