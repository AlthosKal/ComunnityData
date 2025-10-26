package com.senasoft.comunidataapi.csv.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO para el estado del procesamiento de batches.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BatchProcessingStatusDTO {
    private String batchId;
    private Integer totalRegistros;
    private Integer registrosProcesados;
    private Integer registrosCompletados;
    private Integer registrosConError;
    private Double porcentajeCompletado;
    private String estadoGeneral;
    private Long tiempoEstimadoRestante; // en segundos
}
