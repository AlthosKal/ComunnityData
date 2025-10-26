package com.senasoft.comunidataapi.csv.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO de respuesta para la carga de CSV.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CsvUploadResponseDTO {
    private String mensaje;
    private Integer totalRegistros;
    private Integer registrosNormalizados;
    private Integer registrosConError;
    private String batchId;
    private String estadoProcesamiento;
}
