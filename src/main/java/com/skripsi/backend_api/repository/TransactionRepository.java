package com.skripsi.backend_api.repository;

import com.skripsi.backend_api.entity.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {
    Optional<Transaction> findByKodeTransaksi(String kodeTransaksi);

    boolean existsByKodeTransaksi(String kodeTransaksi);

    List<Transaction> findByTransactionDateBetween(LocalDate startDate, LocalDate endDate);
}
