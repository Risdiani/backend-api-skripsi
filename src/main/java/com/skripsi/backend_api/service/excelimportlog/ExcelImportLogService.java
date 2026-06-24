package com.skripsi.backend_api.service.excelimportlog;


import org.springframework.stereotype.Service;

import com.skripsi.backend_api.dto.excelimportlog.response.ExcelImportLogRes;
import com.skripsi.backend_api.entity.ExcelImportLog;

@Service
public class ExcelImportLogService {
    
    public ExcelImportLogRes toResponse(ExcelImportLog e) {
        return ExcelImportLogRes.builder()
                .id(e.getId())
                .fileName(e.getFileName())
                .fileSize(e.getFileSize())
                .totalRows(e.getTotalRows())
                .rowsSuccess(e.getRowsSuccess())
                .rowsFailed(e.getRowsFailed())
                .errorDetail(e.getErrorDetail())
                .status(e.getStatus())
                .importedBy(e.getImportedBy() != null ? e.getImportedBy().getUsername() : null)
                .createdAt(e.getCreatedAt())
                .updatedAt(e.getUpdatedAt())
                .build();
    }
}