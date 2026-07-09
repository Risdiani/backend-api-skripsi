package com.skripsi.backend_api.service.apriori;

import com.skripsi.backend_api.dto.apriori.response.AprioriHistoryDetailRes;
import com.skripsi.backend_api.dto.apriori.response.AprioriHistoryItemsetRes;
import com.skripsi.backend_api.dto.apriori.response.AprioriHistoryRuleRes;
import com.skripsi.backend_api.dto.apriori.response.AprioriHistorySummaryRes;
import com.skripsi.backend_api.entity.AprioriProcess;
import com.skripsi.backend_api.repository.AprioriProcessRepository;
import com.skripsi.backend_api.repository.AssociationRuleRepository;
import com.skripsi.backend_api.repository.FrequentItemsetRepository;

import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class HistoryAprioriService {
    
    private final AprioriProcessRepository aprioriProcessRepository;
    private final FrequentItemsetRepository frequentItemsetRepository;
    private final AssociationRuleRepository associationRuleRepository;

    @Transactional(readOnly = true)
    public List<AprioriHistorySummaryRes> getHistory() {
        return aprioriProcessRepository.findAllByOrderByCreatedAtDesc()
                .stream()
                .map(this::toHistorySummary)
                .toList();
    }

    @Transactional(readOnly = true)
    public AprioriHistoryDetailRes getHistoryDetail(Long id) {
        AprioriProcess process = aprioriProcessRepository.findDetailedById(id)
                .orElseThrow(() -> new RuntimeException("History Apriori tidak ditemukan"));

        List<AprioriHistoryItemsetRes> itemsets = frequentItemsetRepository
                .findByProcessIdOrderByLevelAscSupportDesc(id)
                .stream()
                .map(itemset -> AprioriHistoryItemsetRes.builder()
                        .level(itemset.getLevel())
                        .item(itemset.getItems())
                        .supportCount(itemset.getSupportCount())
                        .support(itemset.getSupport())
                        .keterangan(itemset.getSupport() >= process.getMinSupport() ? "Lolos" : "Tidak Lolos")
                        .build())
                .toList();

        List<AprioriHistoryRuleRes> rules = associationRuleRepository
                .findByProcessIdOrderByConfidenceDesc(id)
                .stream()
                .map(rule -> AprioriHistoryRuleRes.builder()
                        .itemsetLevel(rule.getItemsetLevel())
                        .antecedent(rule.getAntecedent())
                        .consequent(rule.getConsequent())
                        .supportCountXy(rule.getSupportCountXy())
                        .supportCountX(rule.getSupportCountX())
                        .supportCountY(rule.getSupportCountY())
                        .support(rule.getSupport())
                        .confidence(rule.getConfidence())
                        .lift(rule.getLift())
                        .lolosFilter(rule.getLolosFilter())
                        .korelasi(rule.getKorelasi() != null ? rule.getKorelasi().name() : null)
                        .keterangan(Boolean.TRUE.equals(rule.getLolosFilter()) ? "Lolos" : "Tidak Lolos")
                        .build())
                .toList();

        return AprioriHistoryDetailRes.builder()
                .id(process.getId())
                .namaProses(process.getNamaProses())
                .minSupport(process.getMinSupport())
                .minConfidence(process.getMinConfidence())
                .tglMulai(process.getTglMulai())
                .tglSelesai(process.getTglSelesai())
                .totalTransaksi(process.getTotalTransaksi())
                .status(process.getStatus() != null ? process.getStatus().name() : null)
                .executedBy(process.getExecutedBy() != null ? process.getExecutedBy().getFullName() : null)
                .createdAt(process.getCreatedAt())
                .frequentItemsets(itemsets)
                .associationRules(rules)
                .build();
    }

    // --- Helper Methods ---

    // -- Mengubah entitas AprioriProcess menjadi AprioriHistorySummaryRes untuk response --
    private AprioriHistorySummaryRes toHistorySummary(AprioriProcess process) {
        return AprioriHistorySummaryRes.builder()
                .id(process.getId())
                .namaProses(process.getNamaProses())
                .minSupport(process.getMinSupport())
                .minConfidence(process.getMinConfidence())
                .tglMulai(process.getTglMulai())
                .tglSelesai(process.getTglSelesai())
                .totalTransaksi(process.getTotalTransaksi())
                .status(process.getStatus() != null ? process.getStatus().name() : null)
                .executedBy(process.getExecutedBy() != null ? process.getExecutedBy().getFullName() : null)
                .createdAt(process.getCreatedAt())
                .build();
    }
}
