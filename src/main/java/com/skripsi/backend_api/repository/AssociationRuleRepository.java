package com.skripsi.backend_api.repository;

import com.skripsi.backend_api.entity.AssociationRule;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AssociationRuleRepository extends JpaRepository<AssociationRule, Long> {
    List<AssociationRule> findByProcessIdOrderByConfidenceDesc(Long processId);

    void deleteByProcessId(Long processId);
}
