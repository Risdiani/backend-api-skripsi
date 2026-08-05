package com.skripsi.backend_api.service.product;

import com.skripsi.backend_api.dto.excelimportlog.response.ExcelImportLogRes;
import com.skripsi.backend_api.dto.product.request.AddStokReq;
import com.skripsi.backend_api.dto.product.request.ProductReq;
import com.skripsi.backend_api.dto.product.response.ProductRes;
import com.skripsi.backend_api.entity.ExcelImportLog;
import com.skripsi.backend_api.entity.Kategori;
import com.skripsi.backend_api.entity.Product;
import com.skripsi.backend_api.entity.StokMutasi;
import com.skripsi.backend_api.repository.ExcelImportLogRepository;
import com.skripsi.backend_api.repository.KategoriRepository;
import com.skripsi.backend_api.repository.ProductRepository;
import com.skripsi.backend_api.repository.StokMutasiRepository;
import com.skripsi.backend_api.utils.NormalizeUtil;
import com.skripsi.backend_api.utils.ProductCodeGenerator;
import com.skripsi.backend_api.utils.Status;
import com.skripsi.backend_api.entity.User;
import com.skripsi.backend_api.repository.UserRepository;
import com.skripsi.backend_api.utils.AuthContext;
import com.skripsi.backend_api.service.excelimportlog.ExcelImportLogService;
import com.skripsi.backend_api.service.stokmutasi.StokMutasiService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.ss.usermodel.Sheet;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.criteria.Predicate;

import java.io.InputStream;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;
    private final KategoriRepository kategoriRepository;
    private final UserRepository userRepository;
    private final NormalizeUtil normalizeUtil;
    private final ExcelImportLogRepository excelImportLogRepository;
    private final AuthContext authContext;
    private final ExcelImportLogService excelImportLogService;
    private final ProductCodeGenerator productCodeGenerator;
    private final StokMutasiRepository stokMutasiRepository;
    private final StokMutasiService stokMutasiService;

    private ProductRes toResponse(Product p) {
        return ProductRes.builder()
                .id(p.getId())
                .kodeProduk(p.getKodeProduk())
                .namaProduk(p.getNamaProduk())
                .kategori(p.getKategori() != null ? p.getKategori().getNama() : null)
                .satuan(p.getSatuan())
                .harga(p.getHarga())
                .stokTersedia(p.getStokTersedia())
                .isActive(p.getIsActive())
                .createdAt(p.getCreatedAt())
                .updatedAt(p.getUpdatedAt())
                .build();
    }

    // public List<ProductRes> findAll() {
    // log.info("ProductService.findAll - fetching all products");
    // List<ProductRes> result =
    // productRepository.findAll().stream().map(this::toResponse).toList();
    // log.info("ProductService.findAll - total={}", result.size());
    // return result;
    // }

    // Method untuk pemilihan obat ketika ingin melakukan transaksi (non-paginated,
    // filter nama opsional dengan Like, dan hanya mengambil produk aktif)
    public List<ProductRes> getList(String nama) {
        log.info("ProductService.getList - fetching active products with name like={}", nama);

        List<ProductRes> result = productRepository.findAll(criteria(nama, null)).stream()
                .map(this::toResponse)
                .toList();

        log.info("ProductService.getList - total={}", result.size());
        return result;
    }

    // Method API getAll dengan pagination dan filter nama & kategori
    public Page<ProductRes> findAllPage(String nama, String kategori, Integer page, Integer size) {
        log.info("[Start service findAllPage Product]");
        int safePage = page == null || page < 0 ? 0 : page;
        int safeSize = size == null || size < 1 ? 10 : size;
        Pageable pageable = PageRequest.of(safePage, safeSize, Sort.by("createdAt").descending());
        Page<Product> daftar = productRepository.findAll(criteria(nama, kategori), pageable);
        Page<ProductRes> mappedPage = daftar.map(this::toResponse);
        log.info("[End service findAllPage Product]");
        return mappedPage;
    }

    public ProductRes findByKodeProduk(String kodeProduk) {
        log.info("[Start service findByKodeProduk kodeProduk={}]", kodeProduk);
        Product product = productRepository.findByKodeProduk(kodeProduk)
                .orElseThrow(() -> new IllegalArgumentException("Product not found"));

        log.info("[End service findByKodeProduk kodeProduk={}]", kodeProduk);
        return toResponse(product);
    }

    // Method API untuk Penambahan Stok Obat
    @Transactional
    public ProductRes addStock(AddStokReq req) {
        if (req == null) {
            throw new IllegalArgumentException("Request tidak boleh kosong");
        }
        if (req.getKodeProduk() == null || req.getKodeProduk().isBlank()) {
            throw new IllegalArgumentException("Kode produk wajib diisi");
        }
        if (req.getQty() == null || req.getQty() <= 0) {
            throw new IllegalArgumentException("Jumlah qty penambahan stok harus lebih dari 0");
        }
        Product product = productRepository.findByKodeProduk(req.getKodeProduk().trim())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Produk tidak ditemukan dengan kode: " + req.getKodeProduk()));
        int stokSebelum = product.getStokTersedia() != null ? product.getStokTersedia() : 0;
        int stokSesudah = stokSebelum + req.getQty();
        product.setStokTersedia(stokSesudah);
        Product savedProduct = productRepository.save(product);
        String tipeMutasi = (req.getTipe() != null && !req.getTipe().isBlank())
                ? req.getTipe().trim()
                : Status.MASUK.name();
        String keterangan = (req.getKeterangan() != null && !req.getKeterangan().isBlank())
                ? req.getKeterangan().trim()
                : "Penambahan stok obat (" + tipeMutasi + ")";
        // Memanggil StokMutasiService
        stokMutasiService.recordMutasi(savedProduct, tipeMutasi, req.getQty(), stokSebelum, stokSesudah, keterangan);
        log.info("Berhasil menambahkan stok obat kode={}: stokSebelum={}, qty={}, stokSesudah={}",
                req.getKodeProduk(), stokSebelum, req.getQty(), stokSesudah);
        return toResponse(savedProduct);
    }

    @Transactional
    public ProductRes create(ProductReq req) {
        String inputKode = req == null ? null : req.getKodeProduk();
        String kode;
        if (inputKode == null || inputKode.trim().isBlank()) {
            kode = productCodeGenerator.generate();
        } else {
            kode = normalizeUtil.normalizeCode(inputKode);
        }
        String nama = normalizeUtil.normalizeText(req == null ? null : req.getNamaProduk());
        String satuan = normalizeUtil.normalizeText(req == null ? null : req.getSatuan());
        BigDecimal harga = req == null ? null : req.getHarga();
        Boolean isActive = req == null ? null : req.getIsActive();
        Long kategoriId = req == null ? null : req.getKategoriId();
        Integer initialStok = (req != null && req.getStokTersedia() != null) ? req.getStokTersedia() : 0;
        if (nama == null || nama.isBlank())
            throw new IllegalArgumentException("Nama produk is required");
        if (productRepository.existsByKodeProduk(kode))
            throw new IllegalArgumentException("Kode produk already used");
        Kategori kategori = null;
        if (kategoriId != null) {
            kategori = kategoriRepository.findById(kategoriId)
                    .orElseThrow(() -> new IllegalArgumentException("Kategori not found"));
        }
        Product product = Product.builder()
                .kodeProduk(kode)
                .namaProduk(nama)
                .kategori(kategori)
                .satuan(satuan)
                .harga(harga == null ? BigDecimal.ZERO : harga)
                .stokTersedia(initialStok)
                .isActive(isActive == null ? true : isActive)
                .build();
        Product savedProduct = productRepository.save(product);
        // Memanggil StokMutasiService untuk stok awal
        if (initialStok > 0) {
            stokMutasiService.recordMutasi(savedProduct, Status.MASUK.name(), initialStok, 0, initialStok,
                    "Stok awal produk baru");
        }
        return toResponse(savedProduct);
    }

    @Transactional
    public ProductRes update(Long id, ProductReq req) {
        String kode = normalizeUtil.normalizeCode(req == null ? null : req.getKodeProduk());
        String nama = normalizeUtil.normalizeText(req == null ? null : req.getNamaProduk());
        String satuan = normalizeUtil.normalizeText(req == null ? null : req.getSatuan());
        BigDecimal harga = req == null ? null : req.getHarga();
        Boolean isActive = req == null ? null : req.getIsActive();
        Long kategoriId = req == null ? null : req.getKategoriId();
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
        if (kategoriId != null) {
            Kategori kategori = kategoriRepository.findById(kategoriId)
                    .orElseThrow(() -> new IllegalArgumentException("Kategori not found"));
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
        // LOGIKA KOREKSI STOK SAAT UPDATE PRODUK
        if (req != null && req.getStokTersedia() != null) {
            int stokLama = product.getStokTersedia() != null ? product.getStokTersedia() : 0;
            int stokBaru = req.getStokTersedia();
            int selisih = stokBaru - stokLama;
            if (selisih > 0) {
                product.setStokTersedia(stokBaru);
                stokMutasiService.recordMutasi(
                        product, Status.MASUK.name(), selisih, stokLama, stokBaru,
                        "Koreksi/Penyesuaian Stok (Penambahan)");
            } else if (selisih < 0) {
                product.setStokTersedia(stokBaru);
                stokMutasiService.recordMutasi(
                        product, Status.KELUAR.name(), Math.abs(selisih), stokLama, stokBaru,
                        "Koreksi/Penyesuaian Stok (Pengurangan)");
            }
        }
        Product saved = productRepository.save(product);
        log.info("ProductService.update - updated id={}, kode={}", saved.getId(), saved.getKodeProduk());
        return toResponse(saved);
    }

    // METHOD DELETE DENGAN SOFT DELETE AMAN FK CONSTRAINT
    @Transactional
    public void delete(Long id) {
        log.info("ProductService.delete - id={}", id);
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Product not found"));
        // Menggunakan Soft Delete (isActive = false) agar tidak menyebabkan Error FK
        // Constraint
        // pada tabel transaksi_detail dan stok_mutasi, serta menjaga integritas audit
        // transaksi historis.
        product.setIsActive(false);
        productRepository.save(product);
        log.info("ProductService.delete - product id={} successfully soft-deleted (isActive set to false)", id);
    }

    @Transactional
    public ExcelImportLogRes importFromExcel(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File is required");
        }
        String currentUsername = authContext.getCurrentUsername();
        User importer = currentUsername == null
                ? null
                : userRepository.findByUsername(currentUsername).orElse(null);
        ExcelImportLog log = ExcelImportLog.builder()
                .fileName(file.getOriginalFilename())
                .fileSize(file.getSize())
                .totalRows(0)
                .rowsSuccess(0)
                .rowsFailed(0)
                .importedBy(importer)
                .status(com.skripsi.backend_api.utils.Status.PROCESSING)
                .build();
        log = excelImportLogRepository.save(log);
        List<String> errors = new ArrayList<>();
        int success = 0;
        int failed = 0;
        try (InputStream is = file.getInputStream(); Workbook workbook = WorkbookFactory.create(is)) {
            Sheet sheet = workbook.getSheetAt(0);
            DataFormatter formatter = new DataFormatter();
            Row headerRow = sheet.getRow(0);
            if (headerRow == null)
                throw new IllegalArgumentException("Header row is missing");
            Map<String, Integer> headers = new HashMap<>();
            for (Cell cell : headerRow) {
                String key = formatter.formatCellValue(cell).trim().toLowerCase();
                headers.put(key, cell.getColumnIndex());
            }
            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null)
                    continue;
                try {
                    String kode = normalizeUtil.normalizeCode(getCellValue(row, headers, formatter, "kode_produk"));
                    String nama = normalizeUtil.normalizeText(getCellValue(row, headers, formatter, "nama_produk"));
                    String satuan = normalizeUtil.normalizeText(getCellValue(row, headers, formatter, "satuan"));
                    String hargaRaw = getCellValue(row, headers, formatter, "harga");
                    String activeRaw = getCellValue(row, headers, formatter, "is_active");
                    String stokRaw = getCellValue(row, headers, formatter, "stok_tersedia");
                    String kategoriRaw = getCellValue(row, headers, formatter, "kategori_id");
                    if (kode == null || kode.isBlank())
                        throw new IllegalArgumentException("kode_produk wajib diisi");
                    if (nama == null || nama.isBlank())
                        throw new IllegalArgumentException("nama_produk wajib diisi");
                    BigDecimal harga = parseBigDecimal(hargaRaw, BigDecimal.ZERO);
                    Boolean isActive = parseBoolean(activeRaw, true);
                    Integer stok = parseInteger(stokRaw, 0);
                    Long kategoriId = parseLong(kategoriRaw);
                    Kategori kategori = null;
                    if (kategoriId != null) {
                        kategori = kategoriRepository.findById(kategoriId)
                                .orElseThrow(
                                        () -> new IllegalArgumentException("Kategori tidak ditemukan: " + kategoriId));
                    }
                    Product product = productRepository.findByKodeProduk(kode).orElseGet(Product::new);
                    boolean isNewProduct = product.getId() == null;
                    int oldStok = isNewProduct ? 0
                            : (product.getStokTersedia() != null ? product.getStokTersedia() : 0);
                    int newStok = stok != null ? stok : 0;
                    product.setKodeProduk(kode);
                    product.setNamaProduk(nama);
                    product.setKategori(kategori);
                    product.setSatuan(satuan);
                    product.setHarga(harga);
                    product.setIsActive(isActive);
                    product.setStokTersedia(newStok);
                    Product savedProduct = productRepository.save(product);
                    // Memanggil StokMutasiService saat import Excel produk
                    int diff = newStok - oldStok;
                    if (diff > 0) {
                        stokMutasiService.recordMutasi(savedProduct, Status.MASUK.name(), diff, oldStok, newStok,
                                "Import Excel: Penambahan / Stok Awal");
                    } else if (diff < 0) {
                        stokMutasiService.recordMutasi(savedProduct, Status.KELUAR.name(), Math.abs(diff), oldStok,
                                newStok, "Import Excel: Penyesuaian Stok (Pengurangan)");
                    }
                    success++;
                } catch (Exception e) {
                    failed++;
                    errors.add("Baris " + (i + 1) + ": " + e.getMessage());
                }
            }
            log.setTotalRows(success + failed);
            log.setRowsSuccess(success);
            log.setRowsFailed(failed);
            log.setErrorDetail(errors.isEmpty() ? null : String.join("\n", errors));
            log.setStatus(failed == 0
                    ? com.skripsi.backend_api.utils.Status.SUCCESS
                    : (success == 0 ? com.skripsi.backend_api.utils.Status.FAILED
                            : com.skripsi.backend_api.utils.Status.PARTIAL));
            log.setImportedBy(importer);
            ExcelImportLog saved = excelImportLogRepository.save(log);
            return excelImportLogService.toResponse(saved);
        } catch (Exception e) {
            log.setStatus(com.skripsi.backend_api.utils.Status.FAILED);
            log.setErrorDetail(e.getMessage());
            excelImportLogRepository.save(log);
            throw new IllegalArgumentException("Gagal import Excel: " + e.getMessage(), e);
        }
    }

    // Specification builder menggunakan Criteria API
    private Specification<Product> criteria(String nama, String kategori) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (nama != null && !nama.trim().isEmpty()) {
                predicates.add(cb.like(cb.lower(root.get("namaProduk")), "%" + nama.trim().toLowerCase() + "%"));
            }
            if (kategori != null && !kategori.trim().isEmpty()) {
                // Join ke table Kategori untuk mencocokkan nama kategorinya
                predicates.add(cb.like(cb.lower(root.join("kategori").get("nama")),
                        "%" + kategori.trim().toLowerCase() + "%"));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    private String getCellValue(Row row, Map<String, Integer> headers, DataFormatter formatter, String field) {
        Integer idx = headers.get(field.toLowerCase());
        if (idx == null)
            return null;
        Cell cell = row.getCell(idx, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
        return cell == null ? null : formatter.formatCellValue(cell).trim();
    }

    private Long parseLong(String value) {
        if (value == null || value.isBlank())
            return null;
        return Long.parseLong(value.replaceAll("[^0-9]", ""));
    }

    private Integer parseInteger(String value, Integer defaultValue) {
        if (value == null || value.isBlank())
            return defaultValue;
        return Integer.parseInt(value.replaceAll("[^0-9-]", ""));
    }

    private BigDecimal parseBigDecimal(String value, BigDecimal defaultValue) {
        if (value == null || value.isBlank())
            return defaultValue;
        return new BigDecimal(value.replace(",", ""));
    }

    private Boolean parseBoolean(String value, Boolean defaultValue) {
        if (value == null || value.isBlank())
            return defaultValue;
        String v = value.trim().toLowerCase();
        return v.equals("1") || v.equals("true") || v.equals("yes") || v.equals("ya");
    }
}
