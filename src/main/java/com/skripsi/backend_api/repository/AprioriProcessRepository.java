package com.skripsi.backend_api.repository;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import com.skripsi.backend_api.entity.AprioriProcess;

import java.util.List;
import java.util.Optional;

public interface AprioriProcessRepository extends JpaRepository<AprioriProcess, Long> {
    @EntityGraph(attributePaths = {"executedBy"})
    List<AprioriProcess> findAllByOrderByCreatedAtDesc();

    @EntityGraph(attributePaths = {"executedBy"})
    Optional<AprioriProcess> findDetailedById(Long id);
}
