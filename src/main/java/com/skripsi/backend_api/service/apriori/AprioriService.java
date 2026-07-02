package com.skripsi.backend_api.service.apriori;

import com.skripsi.backend_api.dto.apriori.request.AprioriReq;
import com.skripsi.backend_api.dto.apriori.response.AprioriItemsetRes;
import com.skripsi.backend_api.dto.apriori.response.AprioriRuleRes;
import com.skripsi.backend_api.entity.Transaction;
import com.skripsi.backend_api.repository.TransactionRepository;

import java.util.stream.Collectors;
import java.util.*;

import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class AprioriService {

    private final TransactionRepository transactionRepository;

    // ─── AMBIL DATA TRANSAKSI ───────────────────────────────────────────────────
    private List<Set<String>> getTransactionItems(AprioriReq req) {
        log.info("Mengambil transaksi dari database untuk rentang tanggal: {} s/d {}", req.getStartDate(),
                req.getEndDate());
        List<Transaction> transactions = transactionRepository.findByTransactionDateBetween(req.getStartDate(),
                req.getEndDate());

        log.info("Berhasil mengambil {} data transaksi dari database.", transactions.size());
        return transactions.stream()
                .map(t -> t.getDetails().stream()
                        .map(d -> {
                            if (d.getProduk() != null) {
                                return d.getProduk().getNamaProduk();
                            }
                            return "Unknown Product";
                        })
                        .collect(Collectors.toSet()))
                .collect(Collectors.toList());
    }

    // ─── HITUNG ITEMSET 1 ───────────────────────────────────────────────────────
    public List<AprioriItemsetRes> hitungItemset1(AprioriReq req) {
        log.info("=== Memulai perhitungan Itemset 1 ===");
        log.info("Parameter: minSupport={}%, minConfidence={}%", req.getMinSupport(), req.getMinConfidence());
        List<Set<String>> transactions = getTransactionItems(req);
        int totalTx = transactions.size();

        if (totalTx == 0) {
            log.warn("Total transaksi adalah 0. Mengembalikan list kosong untuk Itemset 1.");
            return Collections.emptyList();
        }
        Map<String, Integer> countMap = new HashMap<>();
        for (Set<String> tx : transactions) {
            for (String item : tx) {
                countMap.put(item, countMap.getOrDefault(item, 0) + 1);
            }
        }

        log.info("Berhasil mengekstrak {} item unik dari keseluruhan transaksi.", countMap.size());
        List<AprioriItemsetRes> results = formatItemsetResult(countMap, totalTx, req.getMinSupport());
        log.info("Selesai memformat kembalian data Itemset 1.");

        return results;
    }

    // ─── HITUNG ITEMSET 2 ───────────────────────────────────────────────────────
    public List<AprioriItemsetRes> hitungItemset2(AprioriReq req) {
        log.info("=== Memulai perhitungan Itemset 2 ===");
        List<Set<String>> transactions = getTransactionItems(req);
        int totalTx = transactions.size();
        if (totalTx == 0) {
            log.warn("Total transaksi adalah 0. Mengembalikan list kosong untuk Itemset 2.");
            return Collections.emptyList();
        }
        // Cari Itemset 1 yang lolos dulu
        log.info("Mencari kandidat Itemset 1 yang lolos sebagai bahan Itemset 2...");
        List<AprioriItemsetRes> itemset1 = hitungItemset1(req);
        List<String> lolosItemset1 = itemset1.stream()
                .filter(i -> i.getKeterangan().equals("Lolos"))
                .map(AprioriItemsetRes::getItem)
                .toList();

        log.info("Ditemukan {} item dari Itemset 1 yang lolos (>= {}%).", lolosItemset1.size(), req.getMinSupport());
        // Kombinasikan menjadi pasangan Itemset 2
        Map<String, Integer> countMap2 = new HashMap<>();
        for (int i = 0; i < lolosItemset1.size(); i++) {
            for (int j = i + 1; j < lolosItemset1.size(); j++) {
                String itemA = lolosItemset1.get(i);
                String itemB = lolosItemset1.get(j);

                int count = 0;
                for (Set<String> tx : transactions) {
                    if (tx.contains(itemA) && tx.contains(itemB)) {
                        count++;
                    }
                }
                String key = (itemA.compareTo(itemB) < 0) ? itemA + ", " + itemB : itemB + ", " + itemA;

                // Tambahkan SEMUA kombinasi itemset 2 ke map (meskipun count = 0)
                // Agar tetap muncul di response JSON dengan keterangan "Tidak Lolos"
                countMap2.put(key, count);
            }
        }

        log.info("Selesai menghitung kombinasi Itemset 2. Total Kombinasi: {}. Memformat hasil...", countMap2.size());
        return formatItemsetResult(countMap2, totalTx, req.getMinSupport());
    }

    // ─── HITUNG ITEMSET 3 ───────────────────────────────────────────────────────
    public List<AprioriItemsetRes> hitungItemset3(AprioriReq req) {
        log.info("=== Memulai perhitungan Itemset 3 ===");
        List<Set<String>> transactions = getTransactionItems(req);
        int totalTx = transactions.size();
        if (totalTx == 0) {
            log.warn("Total transaksi adalah 0. Mengembalikan list kosong untuk Itemset 3.");
            return Collections.emptyList();
        }
        log.info("Mencari kandidat pasangan Itemset 2 yang lolos sebagai bahan Itemset 3...");
        List<AprioriItemsetRes> itemset2 = hitungItemset2(req);
        List<String> lolosItemset2 = itemset2.stream()
                .filter(i -> i.getKeterangan().equals("Lolos"))
                .map(AprioriItemsetRes::getItem)
                .toList();

        log.info("Ditemukan {} pasangan dari Itemset 2 yang lolos.", lolosItemset2.size());
        Set<String> validItems = new HashSet<>();
        for (String pair : lolosItemset2) {
            validItems.addAll(Arrays.asList(pair.split(", ")));
        }
        List<String> validItemsList = new ArrayList<>(validItems);
        Map<String, Integer> countMap3 = new HashMap<>();
        for (int i = 0; i < validItemsList.size(); i++) {
            for (int j = i + 1; j < validItemsList.size(); j++) {
                for (int k = j + 1; k < validItemsList.size(); k++) {
                    String itemA = validItemsList.get(i);
                    String itemB = validItemsList.get(j);
                    String itemC = validItemsList.get(k);

                    int count = 0;
                    for (Set<String> tx : transactions) {
                        if (tx.contains(itemA) && tx.contains(itemB) && tx.contains(itemC)) {
                            count++;
                        }
                    }

                    String[] arr = { itemA, itemB, itemC };
                    Arrays.sort(arr);
                    String key = String.join(", ", arr);

                    // Masukkan semua kemungkinan (termasuk yang bernilai 0 kemunculan)
                    countMap3.put(key, count);
                }
            }
        }
        log.info("Selesai menghitung kombinasi Itemset 3. Total Kombinasi: {}. Memformat hasil...", countMap3.size());
        return formatItemsetResult(countMap3, totalTx, req.getMinSupport());
    }

    // ─── HITUNG ATURAN ASOSIASI (CONFIDENCE) ────────────────────────────────────
    public List<AprioriRuleRes> hitungAssociationRules(AprioriReq req) {
        log.info("=== Memulai perhitungan Aturan Asosiasi (Confidence) ===");
        List<Set<String>> transactions = getTransactionItems(req);
        if (transactions.isEmpty()) {
            log.warn("Total transaksi adalah 0. Mengembalikan list kosong untuk Aturan Asosiasi.");
            return Collections.emptyList();
        }
        
        List<AprioriRuleRes> rules = new ArrayList<>();
        // 1. Cek Itemset 3 terlebih dahulu
        log.info("Memeriksa Itemset 3...");
        List<AprioriItemsetRes> itemset3 = hitungItemset3(req);
        List<AprioriItemsetRes> lolosItemset3 = itemset3.stream()
                .filter(i -> i.getKeterangan().equals("Lolos")).toList();
        if (!lolosItemset3.isEmpty()) {
            log.info("Ditemukan {} kombinasi Itemset 3 yang lolos. Membentuk Aturan dari Itemset 3 (A&B -> C).", lolosItemset3.size());
            
            // Kita butuh data jumlah kemunculan dari Itemset 2 untuk membagi support (count antecedent)
            List<AprioriItemsetRes> itemset2 = hitungItemset2(req);
            Map<String, Integer> itemset2Counts = new HashMap<>();
            for (AprioriItemsetRes res : itemset2) {
                itemset2Counts.put(res.getItem(), res.getJumlah());
            }
            for (AprioriItemsetRes i3 : lolosItemset3) {
                String[] items = i3.getItem().split(", ");
                // items selalu berurutan karena Arrays.sort() di hitungItemset3
                String itemA = items[0];
                String itemB = items[1];
                String itemC = items[2];
                int countABC = i3.getJumlah(); // Jumlah kemunculan (A, B, dan C) bersamaan
                // Confidence 1: A & B -> C
                String antecedentAB = itemA + ", " + itemB;
                int countAB = itemset2Counts.getOrDefault(antecedentAB, 0);
                if (countAB > 0) {
                    double conf = ((double) countABC / countAB) * 100;
                    rules.add(AprioriRuleRes.builder()
                            .antecedent(antecedentAB)
                            .consequent(itemC)
                            .confidence(conf)
                            .keterangan(conf >= req.getMinConfidence() ? "Lolos" : "Tidak Lolos")
                            .build());
                }
                // Confidence 2: A & C -> B
                String antecedentAC = itemA + ", " + itemC;
                int countAC = itemset2Counts.getOrDefault(antecedentAC, 0);
                if (countAC > 0) {
                    double conf = ((double) countABC / countAC) * 100;
                    rules.add(AprioriRuleRes.builder()
                            .antecedent(antecedentAC)
                            .consequent(itemB)
                            .confidence(conf)
                            .keterangan(conf >= req.getMinConfidence() ? "Lolos" : "Tidak Lolos")
                            .build());
                }
                // Confidence 3: B & C -> A
                String antecedentBC = itemB + ", " + itemC;
                int countBC = itemset2Counts.getOrDefault(antecedentBC, 0);
                if (countBC > 0) {
                    double conf = ((double) countABC / countBC) * 100;
                    rules.add(AprioriRuleRes.builder()
                            .antecedent(antecedentBC)
                            .consequent(itemA)
                            .confidence(conf)
                            .keterangan(conf >= req.getMinConfidence() ? "Lolos" : "Tidak Lolos")
                            .build());
                }
            }
        } else {
            log.info("Itemset 3 tidak ada yang lolos. Membentuk Aturan Asosiasi dari Itemset 2 (A -> B)...");
            List<AprioriItemsetRes> itemset2 = hitungItemset2(req);
            List<AprioriItemsetRes> lolosItemset2 = itemset2.stream()
                    .filter(i -> i.getKeterangan().equals("Lolos")).toList();
                    
            if (!lolosItemset2.isEmpty()) {
                log.info("Ditemukan {} pasangan Itemset 2 yang lolos.", lolosItemset2.size());
                
                // Butuh data count itemset 1 untuk pembagi (count antecedent)
                List<AprioriItemsetRes> itemset1 = hitungItemset1(req);
                Map<String, Integer> itemset1Counts = new HashMap<>();
                for (AprioriItemsetRes res : itemset1) {
                    itemset1Counts.put(res.getItem(), res.getJumlah());
                }
                for (AprioriItemsetRes i2 : lolosItemset2) {
                    String[] items = i2.getItem().split(", ");
                    String itemA = items[0];
                    String itemB = items[1];
                    int countAB = i2.getJumlah();
                    // Confidence A -> B
                    int countA = itemset1Counts.getOrDefault(itemA, 0);
                    if (countA > 0) {
                        double confAB = ((double) countAB / countA) * 100;
                        rules.add(AprioriRuleRes.builder()
                                .antecedent(itemA)
                                .consequent(itemB)
                                .confidence(confAB)
                                .keterangan(confAB >= req.getMinConfidence() ? "Lolos" : "Tidak Lolos")
                                .build());
                    }
                    // Confidence B -> A
                    int countB = itemset1Counts.getOrDefault(itemB, 0);
                    if (countB > 0) {
                        double confBA = ((double) countAB / countB) * 100;
                        rules.add(AprioriRuleRes.builder()
                                .antecedent(itemB)
                                .consequent(itemA)
                                .confidence(confBA)
                                .keterangan(confBA >= req.getMinConfidence() ? "Lolos" : "Tidak Lolos")
                                .build());
                    }
                }
            } else {
                log.warn("Itemset 2 juga tidak ada yang lolos. Tidak ada aturan asosiasi yang dapat dibentuk.");
            }
        }
        // Urutkan berdasarkan nilai confidence tertinggi ke terendah
        rules.sort((r1, r2) -> Double.compare(r2.getConfidence(), r1.getConfidence()));
        log.info("Berhasil membentuk {} Aturan Asosiasi.", rules.size());
        return rules;
    }

    // ─── HELPER FORMATTING ──────────────────────────────────────────────────────
    private List<AprioriItemsetRes> formatItemsetResult(Map<String, Integer> countMap, int totalTx, double minSupport) {
        List<AprioriItemsetRes> result = new ArrayList<>();

        for (Map.Entry<String, Integer> entry : countMap.entrySet()) {
            double support = ((double) entry.getValue() / totalTx) * 100;

            // LOGIKA FILTER: Di sini kita TIDAK MENYARING data yang "Tidak Lolos"
            // Semua data akan dimasukkan dengan keterangan lolos / tidak lolos.
            result.add(AprioriItemsetRes.builder()
                    .item(entry.getKey())
                    .jumlah(entry.getValue())
                    .support(support)
                    .keterangan(support >= minSupport ? "Lolos" : "Tidak Lolos")
                    .build());
        }
        // Urutkan berdasarkan support terbesar ke terkecil
        result.sort((r1, r2) -> Double.compare(r2.getSupport(), r1.getSupport()));
        return result;
    }
}
