package com.skripsi.backend_api.repository;

import com.skripsi.backend_api.entity.ExcelImportLog;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ExcelImportLogRepository extends JpaRepository<ExcelImportLog, Long> {
}