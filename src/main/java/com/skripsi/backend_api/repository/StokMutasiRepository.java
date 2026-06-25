package com.skripsi.backend_api.repository;

import com.skripsi.backend_api.entity.StokMutasi;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface StokMutasiRepository extends JpaRepository<StokMutasi, Long> {
    // Memutus relasi FK ke transaksi_detail agar transaksi_detail bisa di
    // hard-delete
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE StokMutasi sm SET sm.transaksiDetail = null WHERE sm.transaksiDetail.id = :detailId")
    void nullifyTransaksiDetailId(@Param("detailId") Long detailId);
}
