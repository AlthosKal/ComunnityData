package com.senasoft.comunidataapi.csv.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO que representa la respuesta de IBM Granite después de validar un reporte.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GraniteValidationResponseDTO {
    private String id; // ID del reporte
    private Boolean sesgoDetectado;
    private String descripcionSesgo;
    private String categoriaValidada; // Categoría validada/corregida por IA
    private Boolean esReporteLegitimo; // Si el reporte parece legítimo o es spam
}
