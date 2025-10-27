package com.senasoft.comunidataapi.csv.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** DTO de respuesta para la carga de CSV. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CsvUploadResponseDTO {
    private String message;
    private Integer totalRecords;
    private Integer normalizedRecords;
    private Integer recordsWithErros;
    private String batchId;
    private String processingStatus;
}
