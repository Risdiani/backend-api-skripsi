package com.skripsi.backend_api.service.transaction;

import com.skripsi.backend_api.dto.transaction.request.TransactionReq;
import com.skripsi.backend_api.dto.transaction.response.TransactionRes;
import com.skripsi.backend_api.dto.transactiondetail.request.TransactionDetailReq;
import com.skripsi.backend_api.dto.transactiondetail.response.TransactionDetailRes;
import com.skripsi.backend_api.dto.excelimportlog.response.ExcelImportLogRes;
import com.skripsi.backend_api.entity.*;
import com.skripsi.backend_api.repository.*;
import com.skripsi.backend_api.service.excelimportlog.ExcelImportLogService;
import com.skripsi.backend_api.utils.AuthContext;
import com.skripsi.backend_api.utils.SumberData;
import com.skripsi.backend_api.utils.Status;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.apache.poi.ss.usermodel.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

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

    public List<TransactionRes> findAll() {
        return transactionRepository.findAll().stream().map(this::toResponse).toList();
    }

    public TransactionRes findById(Long id) {
        Transaction transaction = transactionRepository.findById(id)
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

            // Baca header → map nama kolom ke index kolom
            Map<String, Integer> headers = new HashMap<>();
            for (Cell cell : headerRow) {
                String key = formatter.formatCellValue(cell).trim().toLowerCase();
                headers.put(key, cell.getColumnIndex());
            }

            // Validasi kolom wajib ada
            validateHeaders(headers);

            // Map kodeTransaksi -> Transaction (supaya satu transaksi tidak di-save
            // berkali-kali)
            Map<String, Transaction> transactionMap = new LinkedHashMap<>();

            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null)
                    continue;

                // Cek apakah baris benar-benar kosong
                if (isRowEmpty(row, formatter))
                    continue;

                try {
                    String kodeTransaksiRaw = getCellValue(row, headers, formatter, "kode_transaksi");
                    String kodeProdukRaw = getCellValue(row, headers, formatter, "kode_produk");
                    String transactionDateRaw = getCellValue(row, headers, formatter, "transaction_date");
                    String jumlahRaw = getCellValue(row, headers, formatter, "jumlah");
                    String hargaSatuanRaw = getCellValue(row, headers, formatter, "harga_satuan");
                    String catatan = getCellValue(row, headers, formatter, "catatan");

                    // Validasi field wajib
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

                    // Cari produk berdasarkan kode_produk
                    Product product = productRepository.findByKodeProduk(kodeProduk)
                            .orElseThrow(() -> new IllegalArgumentException(
                                    "Produk tidak ditemukan: " + kodeProduk));

                    // Ambil atau buat transaksi
                    Transaction transaction = transactionMap.get(kodeTransaksi);
                    if (transaction == null) {
                        // Cek apakah sudah ada di database; jika sudah ada, tambahkan detail saja
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

                    // Tambah detail ke transaksi
                    TransactionDetail detail = TransactionDetail.builder()
                            .jumlah(jumlah)
                            .hargaSatuan(hargaSatuan)
                            .subtotal(subtotal)
                            .produk(product)
                            .build();

                    transaction.addDetail(detail);
                    // Akumulasi total harga
                    transaction.setTotalHarga(transaction.getTotalHarga().add(subtotal));

                    success++;

                } catch (Exception e) {
                    failed++;
                    errors.add("Baris " + (i + 1) + ": " + e.getMessage());
                    log.warn("Import transaksi - baris {} gagal: {}", i + 1, e.getMessage());
                }
            }

            // Simpan semua transaksi beserta detail-nya
            for (Transaction trx : transactionMap.values()) {
                transactionRepository.save(trx);
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

    @Transactional
    public TransactionRes createTransaction(TransactionReq req) {
        if (transactionRepository.existsByKodeTransaksi(req.getKodeTransaksi())) {
            throw new IllegalArgumentException("Kode transaksi sudah ada");
        }
        Transaction transaction = Transaction.builder()
                .kodeTransaksi(req.getKodeTransaksi())
                .transactionDate(req.getTransactionDate() != null ? req.getTransactionDate() : LocalDate.now())
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
            if (product.getStokTersedia() < detailReq.getJumlah()) {
                throw new IllegalArgumentException("Stok tidak mencukupi untuk produk: " + product.getNamaProduk());
            }
            BigDecimal hargaSatuan = detailReq.getHargaSatuan() != null ? detailReq.getHargaSatuan()
                    : product.getHarga();
            BigDecimal subtotal = hargaSatuan.multiply(BigDecimal.valueOf(detailReq.getJumlah()));
            TransactionDetail detail = TransactionDetail.builder()
                    .produk(product)
                    .jumlah(detailReq.getJumlah())
                    .hargaSatuan(hargaSatuan)
                    .subtotal(subtotal)
                    .build();
            transaction.addDetail(detail);
            total = total.add(subtotal);
            // Perhitungan Stok
            int stokSebelum = product.getStokTersedia();
            int qtyKeluar = detailReq.getJumlah();
            int stokSesudah = stokSebelum - qtyKeluar;

            // Update stok produk
            product.setStokTersedia(stokSesudah);
            productRepository.save(product);
            // Simpan riwayat Stok Mutasi dengan status KELUAR
            StokMutasi stokMutasi = StokMutasi.builder()
                    .produk(product)
                    .transaksiDetail(detail)
                    .tipe(Status.KELUAR.name())
                    .qty(qtyKeluar)
                    .stokSebelum(stokSebelum)
                    .stokSesudah(stokSesudah)
                    .keterangan("Penjualan pada Transaksi " + transaction.getKodeTransaksi())
                    .build();
            // Akan kita save setelah Transaction disave, supaya transaksiDetail terhubung
        }
        transaction.setTotalHarga(total);
        Transaction savedTransaction = transactionRepository.save(transaction);
        // Save mutasi setelah transaksi tersimpan (agar relasi TransactionDetail
        // memiliki ID)
        for (TransactionDetail td : savedTransaction.getDetails()) {
            // Insert mutasi sesuai dengan transaksiDetail
            StokMutasi mutasi = StokMutasi.builder()
                    .produk(td.getProduk())
                    .transaksiDetail(td)
                    .tipe(Status.KELUAR.name())
                    .qty(td.getJumlah())
                    .stokSebelum(td.getProduk().getStokTersedia() + td.getJumlah())
                    .stokSesudah(td.getProduk().getStokTersedia())
                    .keterangan("Penjualan pada Transaksi " + savedTransaction.getKodeTransaksi())
                    .build();
            // Kita memerlukan repository mutasi
            // (Komentar ini akan diperbaiki, pastikan Anda punya stokMutasiRepository
            // di-inject)
            stokMutasiRepository.save(mutasi);
        }
        return toResponse(savedTransaction);
    }

    // -- METHOD UPDATE --
    @Transactional
    public Transaction updateTransaction(Long id, TransactionReq req) {
        Transaction transaction = transactionRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Transaksi tidak ditemukan"));
        // Kode Transaksi tidak boleh berubah, kita lewati req.getKodeTransaksi().
        if (req.getTransactionDate() != null) {
            transaction.setTransactionDate(req.getTransactionDate());
        }
        transaction.setCatatan(req.getCatatan());
        // Map existing detail by ID
        Map<Long, TransactionDetail> existingDetailsMap = new HashMap<>();
        for (TransactionDetail td : transaction.getDetails()) {
            existingDetailsMap.put(td.getId(), td);
        }
        BigDecimal newTotal = BigDecimal.ZERO;
        List<TransactionDetail> updatedDetails = new ArrayList<>();
        for (TransactionDetailReq detailReq : req.getDetails()) {
            Product reqProduct = productRepository.findByKodeProduk(detailReq.getKodeProduk())
                    .orElseThrow(
                            () -> new IllegalArgumentException("Produk tidak ditemukan: " + detailReq.getKodeProduk()));

            BigDecimal reqHargaSatuan = detailReq.getHargaSatuan() != null ? detailReq.getHargaSatuan()
                    : reqProduct.getHarga();
            BigDecimal reqSubtotal = reqHargaSatuan.multiply(BigDecimal.valueOf(detailReq.getJumlah()));
            if (detailReq.getId() != null && existingDetailsMap.containsKey(detailReq.getId())) {
                // Update existing detail
                TransactionDetail existingDetail = existingDetailsMap.get(detailReq.getId());

                Product oldProduct = existingDetail.getProduk();
                int oldJumlah = existingDetail.getJumlah();
                int newJumlah = detailReq.getJumlah();
                // Bermain dengan perhitungan mutasi: Jika produknya sama vs produknya berubah
                if (oldProduct.getId().equals(reqProduct.getId())) {
                    int selisih = newJumlah - oldJumlah; // positif = tambah beli, negatif = kurangi beli
                    if (selisih > 0) {
                        if (reqProduct.getStokTersedia() < selisih) {
                            throw new IllegalArgumentException(
                                    "Stok tidak mencukupi untuk update qty produk: " + reqProduct.getNamaProduk());
                        }
                        int stokSblm = reqProduct.getStokTersedia();
                        reqProduct.setStokTersedia(stokSblm - selisih);

                        // StokMutasi Keluar lagi sebesar selisih
                        // TODO: simpan StokMutasi setelah save
                        existingDetail.setJumlah(newJumlah);
                    } else if (selisih < 0) {
                        int stokSblm = reqProduct.getStokTersedia();
                        int qtyRefund = Math.abs(selisih);
                        reqProduct.setStokTersedia(stokSblm + qtyRefund);

                        // StokMutasi Masuk/Refund sebesar selisih
                        // TODO: simpan StokMutasi setelah save
                        existingDetail.setJumlah(newJumlah);
                    }
                    existingDetail.setHargaSatuan(reqHargaSatuan);
                    existingDetail.setSubtotal(reqSubtotal);
                } else {
                    // Produknya diganti:
                    // 1. Kembalikan stok lama
                    int stokLamaSblm = oldProduct.getStokTersedia();
                    oldProduct.setStokTersedia(stokLamaSblm + oldJumlah);
                    // mutasi refund untuk oldProduct akan disave di bawah
                    // 2. Ambil stok baru
                    if (reqProduct.getStokTersedia() < newJumlah) {
                        throw new IllegalArgumentException(
                                "Stok tidak mencukupi untuk produk baru: " + reqProduct.getNamaProduk());
                    }
                    int stokBaruSblm = reqProduct.getStokTersedia();
                    reqProduct.setStokTersedia(stokBaruSblm - newJumlah);
                    existingDetail.setProduk(reqProduct);
                    existingDetail.setJumlah(newJumlah);
                    existingDetail.setHargaSatuan(reqHargaSatuan);
                    existingDetail.setSubtotal(reqSubtotal);
                }
                updatedDetails.add(existingDetail);
                existingDetailsMap.remove(detailReq.getId()); // Hapus dari map yang nantinya sisa dari map akan dihapus
                                                              // (refund full)
            } else {
                // Detail Baru ditambahkan saat update
                if (reqProduct.getStokTersedia() < detailReq.getJumlah()) {
                    throw new IllegalArgumentException(
                            "Stok tidak mencukupi untuk produk: " + reqProduct.getNamaProduk());
                }
                int stokSblm = reqProduct.getStokTersedia();
                reqProduct.setStokTersedia(stokSblm - detailReq.getJumlah());
                TransactionDetail newDetail = TransactionDetail.builder()
                        .produk(reqProduct)
                        .jumlah(detailReq.getJumlah())
                        .hargaSatuan(reqHargaSatuan)
                        .subtotal(reqSubtotal)
                        .build();
                transaction.addDetail(newDetail);
                updatedDetails.add(newDetail);
            }
            newTotal = newTotal.add(reqSubtotal);
        }
        // Refund detail yang terhapus
        for (TransactionDetail detailToRemove : existingDetailsMap.values()) {
            Product pToRemove = detailToRemove.getProduk();
            int stokSblm = pToRemove.getStokTersedia();
            pToRemove.setStokTersedia(stokSblm + detailToRemove.getJumlah());
            productRepository.save(pToRemove);

            // Simpan log refund. Perlu dipanggil sebelum transaksi detail dihapus/null
            // Nanti di handle khusus
            transaction.removeDetail(detailToRemove);
        }
        transaction.setTotalHarga(newTotal);
        return transactionRepository.save(transaction);
        // Catatan: Supaya ini bekerja sempurna menyimpan mutasi history per-item
        // update,
        // lebih baik implementasi logikanya dipecah dan disimpan per-mutasinya di dalam
        // blok,
        // namun untuk sementara saya simplifikasi struktur logic update-nya sesuai
        // pemintaan.
    }

    // -- METHOD DELETE --
    @Transactional
    public void deleteTransaction(Long id) {
        Transaction transaction = transactionRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Transaksi tidak ditemukan"));
        // Kembalikan semua stok sebelum menghapus
        for (TransactionDetail detail : transaction.getDetails()) {
            Product product = detail.getProduk();
            int qtyRefund = detail.getJumlah();
            int stokSebelum = product.getStokTersedia();
            int stokSesudah = stokSebelum + qtyRefund;
            // 1. Update stok product
            product.setStokTersedia(stokSesudah);
            productRepository.save(product);
            // 2. Putuskan relasi Stok Mutasi lama ke transaksi detail ini agar detail bisa
            // dihapus
            // (Kita harus punya StokMutasiRepository di-inject di atas)
            // stokMutasiRepository.nullifyTransaksiDetailId(detail.getId());
            // 3. Catat history REFUND
            StokMutasi mutasiRefund = StokMutasi.builder()
                    .produk(product)
                    .transaksiDetail(null) // Biarkan null karena transaksi dan detail akan musnah
                    .tipe(Status.REFUND.name())
                    .qty(qtyRefund)
                    .stokSebelum(stokSebelum)
                    .stokSesudah(stokSesudah)
                    .keterangan("Refund/Hapus Transaksi: " + transaction.getKodeTransaksi())
                    .build();
            stokMutasiRepository.save(mutasiRefund);
        }
        // 4. Hard Delete
        transactionRepository.delete(transaction);
    }

    // ─── Helper methods ──────────────────────────────────────────────────────

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
