package com.skripsi.backend_api.service.transaction;

import com.skripsi.backend_api.dto.transaction.request.TransactionReq;
import com.skripsi.backend_api.dto.transaction.response.TransactionRes;
import com.skripsi.backend_api.dto.transactiondetail.request.TransactionDetailReq;
import com.skripsi.backend_api.dto.transactiondetail.response.TransactionDetailRes;
import com.skripsi.backend_api.dto.BaseResponse;
import com.skripsi.backend_api.dto.excelimportlog.response.ExcelImportLogRes;
import com.skripsi.backend_api.entity.*;
import com.skripsi.backend_api.repository.*;
import com.skripsi.backend_api.service.excelimportlog.ExcelImportLogService;
import com.skripsi.backend_api.service.stokmutasi.StokMutasiService;
import com.skripsi.backend_api.utils.AuthContext;
import com.skripsi.backend_api.utils.SumberData;
import com.skripsi.backend_api.utils.TransactionCodeGenerator;
import com.skripsi.backend_api.utils.Status;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.apache.poi.ss.usermodel.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import jakarta.persistence.criteria.Predicate;

import java.io.InputStream;
import java.math.BigDecimal;
import java.time.LocalDate;

import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class TransactionService {

    private final TransactionRepository transactionRepository;

    private final ProductRepository productRepository;
    private final ExcelImportLogRepository excelImportLogRepository;
    private final UserRepository userRepository;
    private final AuthContext authContext;
    private final ExcelImportLogService excelImportLogService;
    private final StokMutasiRepository stokMutasiRepository;
    private final TransactionCodeGenerator transactionCodeGenerator;
    private final StokMutasiService stokMutasiService;

    public Page<TransactionRes> findAllPage(LocalDate tglAwal, LocalDate tglAkhir, Integer page, Integer size) {
        log.info("[Start service findAllPage Transaction]");

        int safePage = page == null || page < 0 ? 0 : page;
        int safeSize = size == null || size < 1 ? 10 : size;
        Pageable pageable = PageRequest.of(safePage, safeSize, Sort.by("createdAt").descending());

        Page<Transaction> daftar = transactionRepository.findAll(criteria(tglAwal, tglAkhir), pageable);
        Page<TransactionRes> mappedPage = daftar.map(this::toResponse);

        log.info("[End service findAllPage Transaction]");
        return mappedPage;
    }

    public TransactionRes findByKodeTransaksi(String kodeTransaksi) {
        Transaction transaction = transactionRepository.findByKodeTransaksi(kodeTransaksi)
                .orElseThrow(() -> new IllegalArgumentException("Transaksi tidak ditemukan"));
        return toResponse(transaction);
    }

    /**
     * Import transaksi dari file Excel dengan format kolom:
     * kode_transaksi | transaction_date | created_at | kode_produk | jumlah |
     * harga_satuan | catatan
     *
     * Setiap baris adalah satu detail transaksi.
     * Baris-baris dengan kode_transaksi yang sama dikelompokkan menjadi satu
     * transaksi.
     */
    @Transactional
    public ExcelImportLogRes importFromExcel(MultipartFile file) {
        log.info("Starting Import Transaksi From Excel");
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File is required");
        }
        String currentUsername = authContext.getCurrentUsername();
        User importer = currentUsername == null
                ? null
                : userRepository.findByUsername(currentUsername).orElse(null);
        ExcelImportLog importLog = ExcelImportLog.builder()
                .fileName(file.getOriginalFilename())
                .fileSize(file.getSize())
                .totalRows(0)
                .rowsSuccess(0)
                .rowsFailed(0)
                .importedBy(importer)
                .status(Status.PROCESSING)
                .build();
        importLog = excelImportLogRepository.save(importLog);
        List<String> errors = new ArrayList<>();
        int success = 0;
        int failed = 0;
        try (InputStream is = file.getInputStream();
                Workbook workbook = WorkbookFactory.create(is)) {
            Sheet sheet = workbook.getSheetAt(0);
            DataFormatter formatter = new DataFormatter();
            Row headerRow = sheet.getRow(0);
            if (headerRow == null) {
                throw new IllegalArgumentException("Header row is missing");
            }
            Map<String, Integer> headers = new HashMap<>();
            for (Cell cell : headerRow) {
                String key = formatter.formatCellValue(cell).trim().toLowerCase();
                headers.put(key, cell.getColumnIndex());
            }
            validateHeaders(headers);
            Map<String, Transaction> transactionMap = new LinkedHashMap<>();
            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null || isRowEmpty(row, formatter))
                    continue;
                try {
                    String kodeTransaksiRaw = getCellValue(row, headers, formatter, "kode_transaksi");
                    String kodeProdukRaw = getCellValue(row, headers, formatter, "kode_produk");
                    String transactionDateRaw = getCellValue(row, headers, formatter, "transaction_date");
                    String jumlahRaw = getCellValue(row, headers, formatter, "jumlah");
                    String hargaSatuanRaw = getCellValue(row, headers, formatter, "harga_satuan");
                    String catatan = getCellValue(row, headers, formatter, "catatan");
                    if (kodeTransaksiRaw == null || kodeTransaksiRaw.isBlank())
                        throw new IllegalArgumentException("kode_transaksi wajib diisi");
                    if (transactionDateRaw == null || transactionDateRaw.isBlank())
                        throw new IllegalArgumentException("transaction_date wajib diisi");
                    if (kodeProdukRaw == null || kodeProdukRaw.isBlank())
                        throw new IllegalArgumentException("kode_produk wajib diisi");
                    final String kodeTransaksi = kodeTransaksiRaw.trim();
                    final String kodeProduk = kodeProdukRaw.trim();
                    LocalDate transactionDate = parseDate(transactionDateRaw.trim());
                    int jumlah = parseIntegerValue(jumlahRaw, 1);
                    BigDecimal hargaSatuan = parseBigDecimal(hargaSatuanRaw, BigDecimal.ZERO);
                    BigDecimal subtotal = hargaSatuan.multiply(BigDecimal.valueOf(jumlah));
                    Product product = productRepository.findByKodeProduk(kodeProduk)
                            .orElseThrow(() -> new IllegalArgumentException("Produk tidak ditemukan: " + kodeProduk));
                    Transaction transaction = transactionMap.get(kodeTransaksi);
                    if (transaction == null) {
                        transaction = transactionRepository.findByKodeTransaksi(kodeTransaksi)
                                .orElseGet(() -> Transaction.builder()
                                        .kodeTransaksi(kodeTransaksi)
                                        .transactionDate(transactionDate)
                                        .totalHarga(BigDecimal.ZERO)
                                        .sumberData(SumberData.excel_import)
                                        .importedBy(importer)
                                        .catatan(catatan != null ? catatan : "Import Excel")
                                        .build());
                        transactionMap.put(kodeTransaksi, transaction);
                    }
                    TransactionDetail detail = TransactionDetail.builder()
                            .jumlah(jumlah)
                            .hargaSatuan(hargaSatuan)
                            .subtotal(subtotal)
                            .produk(product)
                            .build();
                    transaction.addDetail(detail);
                    transaction.setTotalHarga(transaction.getTotalHarga().add(subtotal));
                    // Perbarui stok produk dan simpan mutasi
                    int stokSebelum = product.getStokTersedia();
                    int stokSesudah = Math.max(0, stokSebelum - jumlah);
                    product.setStokTersedia(stokSesudah);
                    productRepository.save(product);
                    success++;
                } catch (Exception e) {
                    failed++;
                    errors.add("Baris " + (i + 1) + ": " + e.getMessage());
                    log.warn("Import transaksi - baris {} gagal: {}", i + 1, e.getMessage());
                }
            }
            // Simpan transaksi dan catat mutasi stok per-detail
            for (Transaction trx : transactionMap.values()) {
                Transaction savedTrx = transactionRepository.save(trx);
                for (TransactionDetail detail : savedTrx.getDetails()) {
                    Product p = detail.getProduk();
                    int qty = detail.getJumlah();
                    // Catat log mutasi penjualan via StokMutasiService
                    stokMutasiService.recordMutasiDetail(
                            p, detail, Status.KELUAR.name(), qty,
                            p.getStokTersedia() + qty, p.getStokTersedia(),
                            "Penjualan Import Excel: " + trx.getKodeTransaksi());
                }
            }
            importLog.setTotalRows(success + failed);
            importLog.setRowsSuccess(success);
            importLog.setRowsFailed(failed);
            importLog.setErrorDetail(errors.isEmpty() ? null : String.join("\n", errors));
            importLog.setStatus(failed == 0
                    ? Status.SUCCESS
                    : (success == 0 ? Status.FAILED : Status.PARTIAL));
            importLog.setImportedBy(importer);
            ExcelImportLog saved = excelImportLogRepository.save(importLog);
            log.info("Import transaksi selesai - success={}, failed={}", success, failed);
            return excelImportLogService.toResponse(saved);
        } catch (Exception e) {
            importLog.setStatus(Status.FAILED);
            importLog.setErrorDetail(e.getMessage());
            excelImportLogRepository.save(importLog);
            log.error("Import transaksi gagal: {}", e.getMessage(), e);
            throw new IllegalArgumentException("Gagal import Excel transaksi: " + e.getMessage(), e);
        }
    }

    public BaseResponse<Object> previewTransaction(TransactionReq req) {
        if (req == null || req.getDetails() == null || req.getDetails().isEmpty()) {
            throw new IllegalArgumentException("Detail transaksi wajib diisi");
        }

        BigDecimal total = BigDecimal.ZERO;
        List<TransactionDetailRes> detailResult = new ArrayList<>();

        for (TransactionDetailReq detailReq : req.getDetails()) {
            Product product = productRepository.findByKodeProduk(detailReq.getKodeProduk())
                    .orElseThrow(
                            () -> new IllegalArgumentException("Produk tidak ditemukan: " + detailReq.getKodeProduk()));

            BigDecimal hargaSatuan = detailReq.getHargaSatuan() != null
                    ? detailReq.getHargaSatuan()
                    : product.getHarga();

            int jumlah = detailReq.getJumlah() != null ? detailReq.getJumlah() : 1;
            BigDecimal subtotal = hargaSatuan.multiply(BigDecimal.valueOf(jumlah));
            total = total.add(subtotal);

            detailResult.add(TransactionDetailRes.builder()
                    .id(detailReq.getId())
                    .produkId(product.getId())
                    .kodeProduk(product.getKodeProduk())
                    .namaProduk(product.getNamaProduk())
                    .jumlah(jumlah)
                    .hargaSatuan(hargaSatuan)
                    .subtotal(subtotal)
                    .build());
        }

        Map<String, Object> result = new HashMap<>();
        result.put("kodeTransaksi", req.getKodeTransaksi());
        result.put("transactionDate", req.getTransactionDate() != null ? req.getTransactionDate() : LocalDate.now());
        result.put("catatan", req.getCatatan());
        result.put("totalHarga", total);
        result.put("details", detailResult);

        return BaseResponse.ok("Total transaksi berhasil dihitung", result);
    }

    @Transactional
    public TransactionRes createTransaction(TransactionReq req) {
        LocalDate transactionDate = req.getTransactionDate() != null ? req.getTransactionDate() : LocalDate.now();
        String generatedKodeTransaksi = transactionCodeGenerator.generate(transactionDate);
        Transaction transaction = Transaction.builder()
                .kodeTransaksi(generatedKodeTransaksi)
                .transactionDate(transactionDate)
                .catatan(req.getCatatan())
                .sumberData(SumberData.manual)
                .totalHarga(BigDecimal.ZERO)
                .build();
        String currentUsername = authContext.getCurrentUsername();
        if (currentUsername != null) {
            transaction.setImportedBy(userRepository.findByUsername(currentUsername).orElse(null));
        }
        BigDecimal total = BigDecimal.ZERO;
        for (TransactionDetailReq detailReq : req.getDetails()) {
            Product product = productRepository.findByKodeProduk(detailReq.getKodeProduk())
                    .orElseThrow(
                            () -> new IllegalArgumentException("Produk tidak ditemukan: " + detailReq.getKodeProduk()));
            int jumlah = detailReq.getJumlah() != null ? detailReq.getJumlah() : 1;
            if (product.getStokTersedia() < jumlah) {
                throw new IllegalArgumentException("Stok tidak mencukupi untuk produk: " + product.getNamaProduk());
            }
            BigDecimal hargaSatuan = detailReq.getHargaSatuan() != null ? detailReq.getHargaSatuan()
                    : product.getHarga();
            BigDecimal subtotal = hargaSatuan.multiply(BigDecimal.valueOf(jumlah));
            TransactionDetail detail = TransactionDetail.builder()
                    .produk(product)
                    .jumlah(jumlah)
                    .hargaSatuan(hargaSatuan)
                    .subtotal(subtotal)
                    .build();
            transaction.addDetail(detail);
            total = total.add(subtotal);
            // Perbarui stok produk
            int stokSebelum = product.getStokTersedia();
            int stokSesudah = stokSebelum - jumlah;
            product.setStokTersedia(stokSesudah);
            productRepository.save(product);
        }
        transaction.setTotalHarga(total);
        Transaction savedTransaction = transactionRepository.save(transaction);
        // Catat mutasi stok via StokMutasiService
        for (TransactionDetail detail : savedTransaction.getDetails()) {
            Product product = detail.getProduk();
            int jumlah = detail.getJumlah();
            int stokSesudah = product.getStokTersedia();
            int stokSebelum = stokSesudah + jumlah;
            stokMutasiService.recordMutasiDetail(
                    product, detail, Status.KELUAR.name(), jumlah,
                    stokSebelum, stokSesudah,
                    "Penjualan pada Transaksi " + generatedKodeTransaksi);
        }
        return toResponse(savedTransaction);
    }

    @Transactional
    public Transaction updateTransaction(Long id, TransactionReq req) {
        Transaction transaction = transactionRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Transaksi tidak ditemukan"));
        if (req.getTransactionDate() != null) {
            transaction.setTransactionDate(req.getTransactionDate());
        }
        transaction.setCatatan(req.getCatatan());
        Map<Long, TransactionDetail> existingDetailsMap = new HashMap<>();
        for (TransactionDetail td : transaction.getDetails()) {
            existingDetailsMap.put(td.getId(), td);
        }
        BigDecimal newTotal = BigDecimal.ZERO;
        for (TransactionDetailReq detailReq : req.getDetails()) {
            Product reqProduct = productRepository.findByKodeProduk(detailReq.getKodeProduk())
                    .orElseThrow(
                            () -> new IllegalArgumentException("Produk tidak ditemukan: " + detailReq.getKodeProduk()));
            BigDecimal reqHargaSatuan = detailReq.getHargaSatuan() != null ? detailReq.getHargaSatuan()
                    : reqProduct.getHarga();
            BigDecimal reqSubtotal = reqHargaSatuan.multiply(BigDecimal.valueOf(detailReq.getJumlah()));
            if (detailReq.getId() != null && existingDetailsMap.containsKey(detailReq.getId())) {
                TransactionDetail existingDetail = existingDetailsMap.get(detailReq.getId());
                Product oldProduct = existingDetail.getProduk();
                int oldJumlah = existingDetail.getJumlah();
                int newJumlah = detailReq.getJumlah();
                if (oldProduct.getId().equals(reqProduct.getId())) {
                    int selisih = newJumlah - oldJumlah;
                    if (selisih > 0) {
                        // Beli lebih banyak -> stok berkurang
                        if (reqProduct.getStokTersedia() < selisih) {
                            throw new IllegalArgumentException(
                                    "Stok tidak mencukupi untuk update qty produk: " + reqProduct.getNamaProduk());
                        }
                        int stokSblm = reqProduct.getStokTersedia();
                        int stokSsdh = stokSblm - selisih;
                        reqProduct.setStokTersedia(stokSsdh);
                        productRepository.save(reqProduct);
                        stokMutasiService.recordMutasiDetail(
                                reqProduct, existingDetail, Status.KELUAR.name(), selisih,
                                stokSblm, stokSsdh, "Update Transaksi (Tambah Qty): " + transaction.getKodeTransaksi());
                    } else if (selisih < 0) {
                        // Beli lebih sedikit -> stok dikembalikan (refund)
                        int qtyRefund = Math.abs(selisih);
                        int stokSblm = reqProduct.getStokTersedia();
                        int stokSsdh = stokSblm + qtyRefund;
                        reqProduct.setStokTersedia(stokSsdh);
                        productRepository.save(reqProduct);
                        stokMutasiService.recordMutasiDetail(
                                reqProduct, existingDetail, Status.REFUND.name(), qtyRefund,
                                stokSblm, stokSsdh,
                                "Update Transaksi (Kurangi Qty): " + transaction.getKodeTransaksi());
                    }
                    existingDetail.setJumlah(newJumlah);
                    existingDetail.setHargaSatuan(reqHargaSatuan);
                    existingDetail.setSubtotal(reqSubtotal);
                } else {
                    // Produk diganti: Kembalikan stok lama & Kurangi stok baru
                    int stokLamaSblm = oldProduct.getStokTersedia();
                    int stokLamaSsdh = stokLamaSblm + oldJumlah;
                    oldProduct.setStokTersedia(stokLamaSsdh);
                    productRepository.save(oldProduct);
                    stokMutasiService.recordRefund(oldProduct, existingDetail.getId(), oldJumlah, stokLamaSblm,
                            stokLamaSsdh, transaction.getKodeTransaksi());
                    if (reqProduct.getStokTersedia() < newJumlah) {
                        throw new IllegalArgumentException(
                                "Stok tidak mencukupi untuk produk baru: " + reqProduct.getNamaProduk());
                    }
                    int stokBaruSblm = reqProduct.getStokTersedia();
                    int stokBaruSsdh = stokBaruSblm - newJumlah;
                    reqProduct.setStokTersedia(stokBaruSsdh);
                    productRepository.save(reqProduct);
                    existingDetail.setProduk(reqProduct);
                    existingDetail.setJumlah(newJumlah);
                    existingDetail.setHargaSatuan(reqHargaSatuan);
                    existingDetail.setSubtotal(reqSubtotal);
                    stokMutasiService.recordMutasiDetail(
                            reqProduct, existingDetail, Status.KELUAR.name(), newJumlah,
                            stokBaruSblm, stokBaruSsdh,
                            "Update Transaksi (Ganti Produk): " + transaction.getKodeTransaksi());
                }
                existingDetailsMap.remove(detailReq.getId());
            } else {
                // Item detail baru ditambahkan saat update transaksi
                if (reqProduct.getStokTersedia() < detailReq.getJumlah()) {
                    throw new IllegalArgumentException(
                            "Stok tidak mencukupi untuk produk: " + reqProduct.getNamaProduk());
                }
                int stokSblm = reqProduct.getStokTersedia();
                int stokSsdh = stokSblm - detailReq.getJumlah();
                reqProduct.setStokTersedia(stokSsdh);
                productRepository.save(reqProduct);
                TransactionDetail newDetail = TransactionDetail.builder()
                        .produk(reqProduct)
                        .jumlah(detailReq.getJumlah())
                        .hargaSatuan(reqHargaSatuan)
                        .subtotal(reqSubtotal)
                        .build();
                transaction.addDetail(newDetail);
                stokMutasiService.recordMutasiDetail(
                        reqProduct, newDetail, Status.KELUAR.name(), detailReq.getJumlah(),
                        stokSblm, stokSsdh, "Update Transaksi (Item Baru): " + transaction.getKodeTransaksi());
            }
            newTotal = newTotal.add(reqSubtotal);
        }
        // Refund detail item yang dihapus pada transaksi
        for (TransactionDetail detailToRemove : existingDetailsMap.values()) {
            Product pToRemove = detailToRemove.getProduk();
            int qtyRefund = detailToRemove.getJumlah();
            int stokSblm = pToRemove.getStokTersedia();
            int stokSsdh = stokSblm + qtyRefund;
            pToRemove.setStokTersedia(stokSsdh);
            productRepository.save(pToRemove);
            stokMutasiService.recordRefund(pToRemove, detailToRemove.getId(), qtyRefund, stokSblm, stokSsdh,
                    transaction.getKodeTransaksi());
            transaction.removeDetail(detailToRemove);
        }
        transaction.setTotalHarga(newTotal);
        return transactionRepository.save(transaction);
    }

    // -- METHOD DELETE --
    @Transactional
    public void deleteTransaction(Long id) {
        Transaction transaction = transactionRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Transaksi tidak ditemukan"));
        // Mengembalikan stok semua produk terkait & mencatat log REFUND via
        // StokMutasiService
        for (TransactionDetail detail : transaction.getDetails()) {
            Product product = detail.getProduk();
            int qtyRefund = detail.getJumlah();
            int stokSebelum = product.getStokTersedia();
            int stokSesudah = stokSebelum + qtyRefund;
            // 1. Kembalikan stok obat
            product.setStokTersedia(stokSesudah);
            productRepository.save(product);
            // 2. Putuskan relasi FK & Catat mutasi REFUND melalui StokMutasiService
            stokMutasiService.recordRefund(
                    product,
                    detail.getId(),
                    qtyRefund,
                    stokSebelum,
                    stokSesudah,
                    transaction.getKodeTransaksi());
        }
        // 3. Hard Delete data transaksi
        transactionRepository.delete(transaction);
        log.info("Transaksi {} berhasil dihapus dan stok obat telah dikembalikan.", transaction.getKodeTransaksi());
    }

    // ─── Helper methods ──────────────────────────────────────────────────────

    // Membuat Specification untuk filter tanggal transaksi
    private Specification<Transaction> criteria(LocalDate tglAwal, LocalDate tglAkhir) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (tglAwal != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("transactionDate"), tglAwal));
            }

            if (tglAkhir != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("transactionDate"), tglAkhir));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    private TransactionRes toResponse(Transaction t) {
        List<TransactionDetailRes> detailResList = t.getDetails().stream().map(d -> TransactionDetailRes.builder()
                .id(d.getId())
                .produkId(d.getProduk() != null ? d.getProduk().getId() : null)
                .kodeProduk(d.getProduk() != null ? d.getProduk().getKodeProduk() : null)
                .namaProduk(d.getProduk() != null ? d.getProduk().getNamaProduk() : null)
                .jumlah(d.getJumlah())
                .hargaSatuan(d.getHargaSatuan())
                .subtotal(d.getSubtotal())
                .createdAt(d.getCreatedAt())
                .build()).toList();
        return TransactionRes.builder()
                .id(t.getId())
                .kodeTransaksi(t.getKodeTransaksi())
                .transactionDate(t.getTransactionDate())
                .totalHarga(t.getTotalHarga())
                .sumberData(t.getSumberData() != null ? t.getSumberData().name() : null)
                .importedBy(t.getImportedBy() != null ? t.getImportedBy().getUsername() : null)
                .catatan(t.getCatatan())
                .createdAt(t.getCreatedAt())
                .updatedAt(t.getUpdatedAt())
                .details(detailResList)
                .build();
    }

    private void validateHeaders(Map<String, Integer> headers) {
        List<String> required = List.of(
                "kode_transaksi", "transaction_date", "kode_produk", "jumlah", "harga_satuan");
        List<String> missing = required.stream()
                .filter(h -> !headers.containsKey(h))
                .toList();
        if (!missing.isEmpty()) {
            throw new IllegalArgumentException("Kolom wajib tidak ditemukan: " + String.join(", ", missing));
        }
    }

    private boolean isRowEmpty(Row row, DataFormatter formatter) {
        for (Cell cell : row) {
            if (cell != null && !formatter.formatCellValue(cell).trim().isEmpty()) {
                return false;
            }
        }
        return true;
    }

    private String getCellValue(Row row, Map<String, Integer> headers,
            DataFormatter formatter, String field) {
        Integer idx = headers.get(field.toLowerCase());
        if (idx == null)
            return null;
        Cell cell = row.getCell(idx, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
        return cell == null ? null : formatter.formatCellValue(cell).trim();
    }

    private LocalDate parseDate(String value) {
        if (value == null || value.isBlank())
            return LocalDate.now();

        // Coba berbagai format tanggal yang umum
        List<DateTimeFormatter> formatters = List.of(
                DateTimeFormatter.ofPattern("yyyy-MM-dd"),
                DateTimeFormatter.ofPattern("dd/MM/yyyy"),
                DateTimeFormatter.ofPattern("dd-MM-yyyy"),
                DateTimeFormatter.ofPattern("M/d/yyyy"),
                DateTimeFormatter.ofPattern("d/M/yyyy"));

        for (DateTimeFormatter dtf : formatters) {
            try {
                return LocalDate.parse(value, dtf);
            } catch (DateTimeParseException ignored) {
                // coba format berikutnya
            }
        }

        // Coba parse sebagai number (Excel date serial)
        try {
            double serial = Double.parseDouble(value.replaceAll("[^0-9.]", ""));
            return org.apache.poi.ss.usermodel.DateUtil.getLocalDateTime(serial).toLocalDate();
        } catch (Exception ignored) {
            // bukan angka, biarkan throw
        }

        throw new IllegalArgumentException("Format tanggal tidak dikenali: " + value);
    }

    private int parseIntegerValue(String value, int defaultValue) {
        if (value == null || value.isBlank())
            return defaultValue;
        try {
            return Integer.parseInt(value.replaceAll("[^0-9-]", ""));
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    private BigDecimal parseBigDecimal(String value, BigDecimal defaultValue) {
        if (value == null || value.isBlank())
            return defaultValue;
        try {
            // Hapus karakter non-numerik kecuali titik dan minus
            String cleaned = value.replaceAll("[^0-9.,\\-]", "").replace(",", "");
            return new BigDecimal(cleaned);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }
}
