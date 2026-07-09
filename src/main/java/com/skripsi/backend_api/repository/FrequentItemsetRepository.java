package com.skripsi.backend_api.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.skripsi.backend_api.entity.FrequentItemset;
import java.util.List;

public interface FrequentItemsetRepository extends JpaRepository<FrequentItemset, Long> {
    List<FrequentItemset> findByProcessIdOrderByLevelAscSupportDesc(Long processId);

    List<FrequentItemset> findByProcessIdAndLevelOrderBySupportDesc(Long processId, Integer level);

    void deleteByProcessIdAndLevel(Long processId, Integer level);
}
