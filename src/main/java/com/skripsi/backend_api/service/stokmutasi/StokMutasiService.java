package com.skripsi.backend_api.service.stokmutasi;

import com.skripsi.backend_api.dto.stokmutasi.response.StokMutasiRes;
import com.skripsi.backend_api.entity.Product;
import com.skripsi.backend_api.entity.StokMutasi;
import com.skripsi.backend_api.entity.TransactionDetail;
import com.skripsi.backend_api.repository.StokMutasiRepository;
import com.skripsi.backend_api.utils.Status;
import com.skripsi.backend_api.entity.Transaction;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.criteria.Predicate;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class StokMutasiService {
    private final StokMutasiRepository stokMutasiRepository;

    /**
     * Catat mutasi stok umum (produk baru, tambah stok obat, import excel produk).
     */
    @Transactional
    public StokMutasi recordMutasi(Product product, String tipe, int qty, int stokSebelum, int stokSesudah,
            String keterangan) {
        StokMutasi mutasi = StokMutasi.builder()
                .produk(product)
                .transaksiDetail(null)
                .tipe(tipe != null && !tipe.isBlank() ? tipe : Status.MASUK.name())
                .qty(qty)
                .stokSebelum(stokSebelum)
                .stokSesudah(stokSesudah)
                .keterangan(keterangan)
                .build();
        log.info("Recording StokMutasi umum - Kode: {}, Tipe: {}, Qty: {}, Sebelum: {}, Sesudah: {}",
                product.getKodeProduk(), tipe, qty, stokSebelum, stokSesudah);
        return stokMutasiRepository.save(mutasi);
    }

    /**
     * Catat mutasi stok terkait transaksi penjualan (penjualan manual / import
     * excel transaksi).
     */
    @Transactional
    public StokMutasi recordMutasiDetail(Product product, TransactionDetail detail, String tipe, int qty,
            int stokSebelum, int stokSesudah, String keterangan) {
        StokMutasi mutasi = StokMutasi.builder()
                .produk(product)
                .transaksiDetail(detail)
                .tipe(tipe != null && !tipe.isBlank() ? tipe : Status.KELUAR.name())
                .qty(qty)
                .stokSebelum(stokSebelum)
                .stokSesudah(stokSesudah)
                .keterangan(keterangan)
                .build();
        log.info("Recording StokMutasi detail - Kode: {}, DetailID: {}, Tipe: {}, Qty: {}",
                product.getKodeProduk(), detail != null ? detail.getId() : null, tipe, qty);
        return stokMutasiRepository.save(mutasi);
    }

    /**
     * Catat mutasi REFUND / pengembalian stok saat update/delete transaksi.
     * Otomatis memutus relasi Foreign Key transaksiDetail agar detail aman untuk
     * di-hard delete.
     */
    @Transactional
    public StokMutasi recordRefund(Product product, Long detailId, int qtyRefund, int stokSebelum, int stokSesudah,
            String kodeTransaksi) {
        if (detailId != null) {
            stokMutasiRepository.nullifyTransaksiDetailId(detailId);
        }
        StokMutasi mutasiRefund = StokMutasi.builder()
                .produk(product)
                .transaksiDetail(null)
                .tipe(Status.REFUND.name())
                .qty(qtyRefund)
                .stokSebelum(stokSebelum)
                .stokSesudah(stokSesudah)
                .keterangan("Refund / Pengembalian Stok Transaksi: " + kodeTransaksi)
                .build();
        log.info("Recording StokMutasi REFUND - Kode Produk: {}, Transaksi: {}, Qty Refund: {}",
                product.getKodeProduk(), kodeTransaksi, qtyRefund);
        return stokMutasiRepository.save(mutasiRefund);
    }

    /**
     * API Method: Menampilkan semua data stok mutasi (Paginated)
     * Filter opsional: tipe (LIKE) dan namaProduk (LIKE)
     */
    public Page<StokMutasiRes> findAllPage(String tipe, String namaProduk, Integer page, Integer size) {
        log.info("[Start service findAllPage StokMutasi - tipe={}, namaProduk={}]", tipe, namaProduk);
        int safePage = page == null || page < 0 ? 0 : page;
        int safeSize = size == null || size < 1 ? 10 : size;
        Pageable pageable = PageRequest.of(safePage, safeSize, Sort.by("createdAt").descending());
        Page<StokMutasi> daftar = stokMutasiRepository.findAll(criteriaAll(tipe, namaProduk), pageable);
        Page<StokMutasiRes> mappedPage = daftar.map(this::toResponse);
        log.info("[End service findAllPage StokMutasi]");
        return mappedPage;
    }

    /**
     * API Method: Menampilkan riwayat stok mutasi untuk produk tertentu (Paginated)
     * Parameter: kodeProduk (REQUIRED), tipe (OPTIONAL LIKE)
     */
    public Page<StokMutasiRes> findDetailPage(String kodeProduk, String tipe, Integer page, Integer size) {
        log.info("[Start service findDetailPage StokMutasi - kodeProduk={}, tipe={}]", kodeProduk, tipe);
        if (kodeProduk == null || kodeProduk.isBlank()) {
            throw new IllegalArgumentException("Parameter kodeProduk wajib diisi");
        }
        int safePage = page == null || page < 0 ? 0 : page;
        int safeSize = size == null || size < 1 ? 10 : size;
        Pageable pageable = PageRequest.of(safePage, safeSize, Sort.by("createdAt").descending());
        Page<StokMutasi> daftar = stokMutasiRepository.findAll(criteriaDetail(kodeProduk, tipe), pageable);
        Page<StokMutasiRes> mappedPage = daftar.map(this::toResponse);
        log.info("[End service findDetailPage StokMutasi]");
        return mappedPage;
    }

    /**
     * Helper criteria untuk filter findAllPage (tipe & namaProduk dengan LIKE)
     */
    private Specification<StokMutasi> criteriaAll(String tipe, String namaProduk) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (tipe != null && !tipe.trim().isEmpty()) {
                predicates.add(cb.like(cb.lower(root.get("tipe")), "%" + tipe.trim().toLowerCase() + "%"));
            }
            if (namaProduk != null && !namaProduk.trim().isEmpty()) {
                predicates.add(cb.like(cb.lower(root.join("produk").get("namaProduk")),
                        "%" + namaProduk.trim().toLowerCase() + "%"));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    /**
     * Helper criteria untuk filter findDetailPage (kodeProduk & tipe dengan LIKE)
     */
    private Specification<StokMutasi> criteriaDetail(String kodeProduk, String tipe) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (kodeProduk != null && !kodeProduk.trim().isEmpty()) {
                predicates.add(cb.equal(root.join("produk").get("kodeProduk"), kodeProduk.trim()));
            }
            if (tipe != null && !tipe.trim().isEmpty()) {
                predicates.add(cb.like(cb.lower(root.get("tipe")), "%" + tipe.trim().toLowerCase() + "%"));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    /**
     * Mapping entitas StokMutasi ke DTO StokMutasiRes
     */
    private StokMutasiRes toResponse(StokMutasi sm) {
        Product p = sm.getProduk();
        TransactionDetail td = sm.getTransaksiDetail();
        Transaction t = (td != null) ? td.getTransaksi() : null;
        return StokMutasiRes.builder()
                .id(sm.getId())
                .kodeProduk(p != null ? p.getKodeProduk() : null)
                .namaProduk(p != null ? p.getNamaProduk() : null)
                .harga(p != null ? p.getHarga() : null)
                .satuan(p != null ? p.getSatuan() : null)
                .namaKategori((p != null && p.getKategori() != null) ? p.getKategori().getNama() : null)
                .kodeTransaksi(t != null ? t.getKodeTransaksi() : null)
                .tanggalTransaksi(t != null ? t.getTransactionDate() : null)
                .tipe(sm.getTipe())
                .qty(sm.getQty())
                .stokSebelum(sm.getStokSebelum())
                .stokSesudah(sm.getStokSesudah())
                .keterangan(sm.getKeterangan())
                .createdAt(sm.getCreatedAt())
                .build();
    }
}
