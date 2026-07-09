package com.skripsi.backend_api.utils;

import com.skripsi.backend_api.entity.Transaction;
import com.skripsi.backend_api.repository.TransactionRepository;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@Component
@RequiredArgsConstructor
public class TransactionCodeGenerator {

    private final TransactionRepository transactionRepository;

    public String generate(LocalDate transactionDate) {
        LocalDate safeDate = transactionDate != null ? transactionDate : LocalDate.now();
        String datePart = safeDate.format(DateTimeFormatter.BASIC_ISO_DATE);
        String prefix = "TRX" + datePart;

        String lastCode = transactionRepository
                .findTopByKodeTransaksiStartingWithOrderByKodeTransaksiDesc(prefix)
                .map(Transaction::getKodeTransaksi)
                .orElse(null);

        int nextSequence = 1;
        if (lastCode != null && lastCode.length() >= prefix.length() + 3) {
            String lastSequencePart = lastCode.substring(prefix.length());
            try {
                nextSequence = Integer.parseInt(lastSequencePart) + 1;
            } catch (NumberFormatException ignored) {
                nextSequence = 1;
            }
        }

        return prefix + String.format("%03d", nextSequence);
    }
}