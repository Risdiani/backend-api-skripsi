package com.skripsi.backend_api.dto.excelimportlog.response;

import com.skripsi.backend_api.utils.Status;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class ExcelImportLogRes {
    private Long id;
    private String fileName;
    private Long fileSize;
    private Integer totalRows;
    private Integer rowsSuccess;
    private Integer rowsFailed;
    private String errorDetail;
    private Status status;
    private String importedBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
