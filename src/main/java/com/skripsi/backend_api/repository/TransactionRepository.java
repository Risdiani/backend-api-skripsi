package com.skripsi.backend_api.repository;

import com.skripsi.backend_api.entity.Transaction;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface TransactionRepository extends JpaRepository<Transaction, Long>, JpaSpecificationExecutor<Transaction> {
    Optional<Transaction> findByKodeTransaksi(String kodeTransaksi);

    Optional<Transaction> findTopByKodeTransaksiStartingWithOrderByKodeTransaksiDesc(String kodeTransaksiPrefix);

    boolean existsByKodeTransaksi(String kodeTransaksi);

    List<Transaction> findByTransactionDateBetween(LocalDate startDate, LocalDate endDate);

    Page<Transaction> findByTransactionDateBetween(LocalDate startDate, LocalDate endDate, Pageable pageable);
}