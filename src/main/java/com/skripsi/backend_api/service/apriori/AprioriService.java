package com.skripsi.backend_api.service.apriori;

import com.skripsi.backend_api.dto.apriori.request.AprioriReq;
import com.skripsi.backend_api.dto.apriori.response.AprioriHistoryDetailRes;
import com.skripsi.backend_api.dto.apriori.response.AprioriHistoryItemsetRes;
import com.skripsi.backend_api.dto.apriori.response.AprioriHistoryRuleRes;
import com.skripsi.backend_api.dto.apriori.response.AprioriHistorySummaryRes;
import com.skripsi.backend_api.dto.apriori.response.AprioriItemsetRes;
import com.skripsi.backend_api.dto.apriori.response.AprioriRuleRes;
import com.skripsi.backend_api.dto.apriori.response.AprioriStepRes;
import com.skripsi.backend_api.entity.AprioriProcess;
import com.skripsi.backend_api.entity.AssociationRule;
import com.skripsi.backend_api.entity.FrequentItemset;
import com.skripsi.backend_api.entity.Transaction;
import com.skripsi.backend_api.entity.User;
import com.skripsi.backend_api.repository.AprioriProcessRepository;
import com.skripsi.backend_api.repository.AssociationRuleRepository;
import com.skripsi.backend_api.repository.FrequentItemsetRepository;
import com.skripsi.backend_api.repository.TransactionRepository;
import com.skripsi.backend_api.repository.UserRepository;
import com.skripsi.backend_api.utils.AuthContext;
import com.skripsi.backend_api.utils.Korelasi;
import com.skripsi.backend_api.utils.Status;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class AprioriService {

    private final TransactionRepository transactionRepository;
    private final AprioriProcessRepository aprioriProcessRepository;
    private final FrequentItemsetRepository frequentItemsetRepository;
    private final AssociationRuleRepository associationRuleRepository;
    private final UserRepository userRepository;
    private final AuthContext authContext;


    @Transactional
    public AprioriStepRes<List<AprioriItemsetRes>> hitungItemset1(AprioriReq req) {
        validateRequest(req);

        List<Set<String>> transactions = getTransactionItemsFromRequest(req);
        int totalTx = transactions.size();

        AprioriProcess process = createProcess(req, totalTx);

        if (totalTx == 0) {
            process.setStatus(Status.DONE);
            aprioriProcessRepository.save(process);

            return AprioriStepRes.<List<AprioriItemsetRes>>builder()
                    .processId(process.getId())
                    .namaProses(process.getNamaProses())
                    .status(process.getStatus().name())
                    .totalTransaksi(process.getTotalTransaksi())
                    .data(Collections.emptyList())
                    .build();
        }

        Map<String, Integer> countMap = new HashMap<>();
        for (Set<String> tx : transactions) {
            for (String item : tx) {
                countMap.put(item, countMap.getOrDefault(item, 0) + 1);
            }
        }

        List<AprioriItemsetRes> results = formatItemsetResult(countMap, totalTx, req.getMinSupport());
        saveFrequentItemsets(process, 1, results);

        return AprioriStepRes.<List<AprioriItemsetRes>>builder()
                .processId(process.getId())
                .namaProses(process.getNamaProses())
                .status(process.getStatus().name())
                .totalTransaksi(process.getTotalTransaksi())
                .data(results)
                .build();
    }

    @Transactional
    public AprioriStepRes<List<AprioriItemsetRes>> hitungItemset2(AprioriReq req) {
        validateRequest(req);

        AprioriProcess process = loadProcess(req.getProcessId());
        List<Set<String>> transactions = getTransactionItems(process);
        int totalTx = transactions.size();

        List<FrequentItemset> itemset1Entities = frequentItemsetRepository
                .findByProcessIdAndLevelOrderBySupportDesc(process.getId(), 1);

        List<String> lolosItemset1 = buildItemsetResponse(itemset1Entities, process.getMinSupport()).stream()
                .filter(i -> "Lolos".equals(i.getKeterangan()))
                .map(AprioriItemsetRes::getItem)
                .toList();

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
                countMap2.put(key, count);
            }
        }

        List<AprioriItemsetRes> results = formatItemsetResult(countMap2, totalTx, process.getMinSupport());
        saveFrequentItemsets(process, 2, results);

        return AprioriStepRes.<List<AprioriItemsetRes>>builder()
                .processId(process.getId())
                .namaProses(process.getNamaProses())
                .status(process.getStatus().name())
                .totalTransaksi(process.getTotalTransaksi())
                .data(results)
                .build();
    }

    @Transactional
    public AprioriStepRes<List<AprioriItemsetRes>> hitungItemset3(AprioriReq req) {
        validateRequest(req);

        AprioriProcess process = loadProcess(req.getProcessId());
        List<Set<String>> transactions = getTransactionItems(process);
        int totalTx = transactions.size();

        List<FrequentItemset> itemset2Entities = frequentItemsetRepository
                .findByProcessIdAndLevelOrderBySupportDesc(process.getId(), 2);

        List<String> lolosItemset2 = buildItemsetResponse(itemset2Entities, process.getMinSupport()).stream()
                .filter(i -> "Lolos".equals(i.getKeterangan()))
                .map(AprioriItemsetRes::getItem)
                .toList();

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
                    countMap3.put(key, count);
                }
            }
        }

        List<AprioriItemsetRes> results = formatItemsetResult(countMap3, totalTx, process.getMinSupport());
        saveFrequentItemsets(process, 3, results);

        return AprioriStepRes.<List<AprioriItemsetRes>>builder()
                .processId(process.getId())
                .namaProses(process.getNamaProses())
                .status(process.getStatus().name())
                .totalTransaksi(process.getTotalTransaksi())
                .data(results)
                .build();
    }

    @Transactional
    public AprioriStepRes<List<AprioriRuleRes>> hitungAssociationRules(AprioriReq req) {
        validateRequest(req);

        AprioriProcess process = loadProcess(req.getProcessId());
        List<Set<String>> transactions = getTransactionItems(process);
        int totalTx = transactions.size();

        List<FrequentItemset> itemset1Entities = frequentItemsetRepository
                .findByProcessIdAndLevelOrderBySupportDesc(process.getId(), 1);
        List<FrequentItemset> itemset2Entities = frequentItemsetRepository
                .findByProcessIdAndLevelOrderBySupportDesc(process.getId(), 2);
        List<FrequentItemset> itemset3Entities = frequentItemsetRepository
                .findByProcessIdAndLevelOrderBySupportDesc(process.getId(), 3);

        if (itemset1Entities.isEmpty()) {
            throw new RuntimeException("Itemset 1 belum tersedia. Jalankan endpoint /itemset1 terlebih dahulu.");
        }
        if (itemset2Entities.isEmpty()) {
            throw new RuntimeException("Itemset 2 belum tersedia. Jalankan endpoint /itemset2 terlebih dahulu.");
        }

        List<AssociationRule> ruleEntities = buildAssociationRuleEntities(
                req, itemset1Entities, itemset2Entities, itemset3Entities, totalTx);

        associationRuleRepository.deleteByProcessId(process.getId());
        for (AssociationRule rule : ruleEntities) {
            rule.setProcess(process);
        }
        associationRuleRepository.saveAll(ruleEntities);

        process.setStatus(Status.DONE);
        aprioriProcessRepository.save(process);

        List<AprioriRuleRes> response = ruleEntities.stream()
                .map(this::toRuleResponse)
                .toList();

        return AprioriStepRes.<List<AprioriRuleRes>>builder()
                .processId(process.getId())
                .namaProses(process.getNamaProses())
                .status(process.getStatus().name())
                .totalTransaksi(process.getTotalTransaksi())
                .data(response)
                .build();
    }

    // ---------- HELPER METHODS --------------------------------------------------------------


    // -- Validasi input request --
    private void validateRequest(AprioriReq req) {
        if (req.getMinSupport() == null) {
            throw new IllegalArgumentException("minSupport wajib diisi");
        }
        if (req.getMinConfidence() == null) {
            throw new IllegalArgumentException("minConfidence wajib diisi");
        }
        if (req.getStartDate() == null || req.getEndDate() == null) {
            throw new IllegalArgumentException("startDate dan endDate wajib diisi");
        }
        if (req.getStartDate().isAfter(req.getEndDate())) {
            throw new IllegalArgumentException("startDate tidak boleh lebih besar dari endDate");
        }
    }

    // -- Mengambil transaksi dari database berdasarkan rentang tanggal yang ada di process --
    private List<Set<String>> getTransactionItems(AprioriProcess process) {
        log.info("Mengambil transaksi dari database untuk rentang tanggal: {} s/d {}", process.getTglMulai(),
                process.getTglSelesai());

        List<Transaction> transactions = transactionRepository.findByTransactionDateBetween(
                process.getTglMulai(), process.getTglSelesai());

        log.info("Berhasil mengambil {} data transaksi dari database.", transactions.size());

        return transactions.stream()
                .map(t -> t.getDetails().stream()
                        .map(d -> d.getProduk() != null ? d.getProduk().getNamaProduk() : "Unknown Product")
                        .collect(Collectors.toSet()))
                .collect(Collectors.toList());
    }

    // -- Membuat entitas AprioriProcess baru dan menyimpannya ke database --
    private AprioriProcess createProcess(AprioriReq req, int totalTx) {
        User executedBy = resolveCurrentUser();

        AprioriProcess process = AprioriProcess.builder()
                .namaProses("Apriori " + req.getStartDate() + " s/d " + req.getEndDate())
                .minSupport(req.getMinSupport())
                .minConfidence(req.getMinConfidence())
                .tglMulai(req.getStartDate())
                .tglSelesai(req.getEndDate())
                .totalTransaksi(totalTx)
                .status(Status.RUNNING)
                .executedBy(executedBy)
                .build();

        return aprioriProcessRepository.save(process);
    }

    // -- Mengambil entitas AprioriProcess dari database berdasarkan processId --
    private AprioriProcess loadProcess(Long processId) {
        if (processId == null) {
            throw new IllegalArgumentException("processId wajib diisi");
        }
        return aprioriProcessRepository.findById(processId)
                .orElseThrow(() -> new RuntimeException("Process Apriori tidak ditemukan"));
    }

    // -- Mengubah daftar FrequentItemset menjadi daftar AprioriItemsetRes untuk response --
    private List<AprioriItemsetRes> buildItemsetResponse(List<FrequentItemset> itemsets, double minSupport) {
        List<AprioriItemsetRes> result = new ArrayList<>();

        for (FrequentItemset itemset : itemsets) {
            result.add(AprioriItemsetRes.builder()
                    .item(itemset.getItems())
                    .jumlah(itemset.getSupportCount())
                    .support(itemset.getSupport())
                    .keterangan(itemset.getSupport() >= minSupport ? "Lolos" : "Tidak Lolos")
                    .build());
        }

        result.sort((r1, r2) -> Double.compare(r2.getSupport(), r1.getSupport()));
        return result;
    }

    // -- Mengubah map itemset dan jumlahnya menjadi daftar AprioriItemsetRes untuk response --
    private List<AprioriItemsetRes> formatItemsetResult(Map<String, Integer> countMap, int totalTx, double minSupport) {
        List<AprioriItemsetRes> result = new ArrayList<>();

        for (Map.Entry<String, Integer> entry : countMap.entrySet()) {
            double support = ((double) entry.getValue() / totalTx) * 100;
            support = Math.round(support * 100.0) / 100.0;

            result.add(AprioriItemsetRes.builder()
                    .item(entry.getKey())
                    .jumlah(entry.getValue())
                    .support(support)
                    .keterangan(support >= minSupport ? "Lolos" : "Tidak Lolos")
                    .build());
        }

        result.sort((r1, r2) -> Double.compare(r2.getSupport(), r1.getSupport()));
        return result;
    }

    // -- Menyimpan daftar FrequentItemset ke database --
    private void saveFrequentItemsets(AprioriProcess process, int level, List<AprioriItemsetRes> results) {
        frequentItemsetRepository.deleteByProcessIdAndLevel(process.getId(), level);

        List<FrequentItemset> entities = results.stream()
                .map(res -> FrequentItemset.builder()
                        .level(level)
                        .items(res.getItem())
                        .supportCount(res.getJumlah())
                        .support(res.getSupport())
                        .process(process)
                        .build())
                .toList();

        frequentItemsetRepository.saveAll(entities);
    }

    // -- Mengubah daftar FrequentItemset menjadi map itemset dan jumlahnya --
    private Map<String, Integer> toCountMap(List<FrequentItemset> itemsets) {
        Map<String, Integer> map = new LinkedHashMap<>();
        for (FrequentItemset itemset : itemsets) {
            map.put(itemset.getItems(), itemset.getSupportCount());
        }
        return map;
    }

    // -- Memisahkan string itemset menjadi array item --
    private String[] splitItems(String items) {
        return items.split(", ");
    }

    // -- Mengambil daftar AprioriItemsetRes yang lolos filter dari database berdasarkan processId, level, dan minSupport --
    private List<AprioriItemsetRes> getLolosItemsetsFromDb(Long processId, Integer level, double minSupport) {
        List<FrequentItemset> itemsets = frequentItemsetRepository
                .findByProcessIdAndLevelOrderBySupportDesc(processId, level);

        return buildItemsetResponse(itemsets, minSupport).stream()
                .filter(i -> "Lolos".equals(i.getKeterangan()))
                .toList();
    }

    // -- Membangun daftar AssociationRule dari itemset level 1, 2, dan 3 --
    private List<AssociationRule> buildAssociationRuleEntities(
            AprioriReq req,
            List<FrequentItemset> itemset1Entities,
            List<FrequentItemset> itemset2Entities,
            List<FrequentItemset> itemset3Entities,
            int totalTx) {

        Map<String, Integer> itemset1Counts = toCountMap(itemset1Entities);
        Map<String, Integer> itemset2Counts = toCountMap(itemset2Entities);

        List<AssociationRule> rules = new ArrayList<>();

        List<FrequentItemset> lolosItemset3 = itemset3Entities.stream()
                .filter(i -> i.getSupport() >= req.getMinSupport())
                .toList();

        if (!lolosItemset3.isEmpty()) {
            for (FrequentItemset i3 : lolosItemset3) {
                String[] items = splitItems(i3.getItems());
                if (items.length != 3) {
                    continue;
                }

                String itemA = items[0];
                String itemB = items[1];
                String itemC = items[2];
                int countABC = i3.getSupportCount();

                addRuleEntity(rules, req, totalTx, 3,
                        itemA + ", " + itemB, itemC,
                        countABC,
                        itemset2Counts.getOrDefault(itemA + ", " + itemB, 0),
                        itemset1Counts.getOrDefault(itemC, 0));

                addRuleEntity(rules, req, totalTx, 3,
                        itemA + ", " + itemC, itemB,
                        countABC,
                        itemset2Counts.getOrDefault(itemA + ", " + itemC, 0),
                        itemset1Counts.getOrDefault(itemB, 0));

                addRuleEntity(rules, req, totalTx, 3,
                        itemB + ", " + itemC, itemA,
                        countABC,
                        itemset2Counts.getOrDefault(itemB + ", " + itemC, 0),
                        itemset1Counts.getOrDefault(itemA, 0));
            }
        } else {
            List<FrequentItemset> lolosItemset2 = itemset2Entities.stream()
                    .filter(i -> i.getSupport() >= req.getMinSupport())
                    .toList();

            for (FrequentItemset i2 : lolosItemset2) {
                String[] items = splitItems(i2.getItems());
                if (items.length != 2) {
                    continue;
                }

                String itemA = items[0];
                String itemB = items[1];
                int countAB = i2.getSupportCount();

                addRuleEntity(rules, req, totalTx, 2,
                        itemA, itemB,
                        countAB,
                        itemset1Counts.getOrDefault(itemA, 0),
                        itemset1Counts.getOrDefault(itemB, 0));

                addRuleEntity(rules, req, totalTx, 2,
                        itemB, itemA,
                        countAB,
                        itemset1Counts.getOrDefault(itemB, 0),
                        itemset1Counts.getOrDefault(itemA, 0));
            }
        }

        rules.sort((r1, r2) -> Double.compare(r2.getConfidence(), r1.getConfidence()));
        return rules;
    }

    // -- Menambahkan entitas AssociationRule ke daftar rules berdasarkan parameter yang diberikan --
    private void addRuleEntity(
            List<AssociationRule> rules,
            AprioriReq req,
            int totalTx,
            int itemsetLevel,
            String antecedent,
            String consequent,
            int supportCountXy,
            int supportCountX,
            int supportCountY) {

        if (supportCountX == 0 || supportCountY == 0) {
            return;
        }

        double support = ((double) supportCountXy / totalTx) * 100;
        double confidence = ((double) supportCountXy / supportCountX) * 100;
        double supportY = ((double) supportCountY / totalTx) * 100;
        double lift = supportY == 0 ? 0D : confidence / supportY;

        support = Math.round(support * 100.0) / 100.0;
        confidence = Math.round(confidence * 100.0) / 100.0;
        supportY = Math.round(supportY * 100.0) / 100.0;
        lift = Math.round(lift * 100.0) / 100.0;

        boolean lolosFilter = support >= req.getMinSupport() && confidence >= req.getMinConfidence();

        Korelasi korelasi;
        if (lift > 1D) {
            korelasi = Korelasi.POSITIF;
        } else if (lift < 1D) {
            korelasi = Korelasi.NEGATIF;
        } else {
            korelasi = Korelasi.INDEPENDEN;
        }

        rules.add(AssociationRule.builder()
                .itemsetLevel(itemsetLevel)
                .antecedent(antecedent)
                .consequent(consequent)
                .supportCountXy(supportCountXy)
                .supportCountX(supportCountX)
                .supportCountY(supportCountY)
                .support(support)
                .confidence(confidence)
                .lift(lift)
                .lolosFilter(lolosFilter)
                .korelasi(korelasi)
                .build());
    }

    // -- Mengubah entitas AssociationRule menjadi AprioriRuleRes untuk response --
    private AprioriRuleRes toRuleResponse(AssociationRule rule) {
        return AprioriRuleRes.builder()
                .antecedent(rule.getAntecedent())
                .consequent(rule.getConsequent())
                .confidence(rule.getConfidence())
                .keterangan(Boolean.TRUE.equals(rule.getLolosFilter()) ? "Lolos" : "Tidak Lolos")
                .build();
    }

    // -- Mengambil entitas User saat ini berdasarkan username dari AuthContext --
    private User resolveCurrentUser() {
        String username = authContext.getCurrentUsername();
        if (username == null) {
            return null;
        }
        return userRepository.findByUsername(username).orElse(null);
    }

    // -- Mengambil transaksi dari database berdasarkan rentang tanggal yang ada di request --
    private List<Set<String>> getTransactionItemsFromRequest(AprioriReq req) {
        log.info("Mengambil transaksi dari database untuk rentang tanggal: {} s/d {}", req.getStartDate(),
                req.getEndDate());

        List<Transaction> transactions = transactionRepository.findByTransactionDateBetween(
                req.getStartDate(), req.getEndDate());

        log.info("Berhasil mengambil {} data transaksi dari database.", transactions.size());

        return transactions.stream()
                .map(t -> t.getDetails().stream()
                        .map(d -> d.getProduk() != null ? d.getProduk().getNamaProduk() : "Unknown Product")
                        .collect(Collectors.toSet()))
                .collect(Collectors.toList());
    }
}