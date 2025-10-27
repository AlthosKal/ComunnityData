package com.senasoft.comunidataapi.csv.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO que representa una fila cruda del CSV antes de normalización.
 *
 * <p>Estructura original del CSV: ID, Nombre, Edad, Género, Ciudad, Comentario, Categoría del
 * problema, Nivel de urgencia, Fecha del reporte, Acceso a internet, Atención previa del gobierno,
 * Zona rural
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RawCsvRowDTO {
    private String id;
    private String name; // Se descartará por privacidad
    private String age;
    private String gender; // Se descartará por privacidad
    private String city;
    private String comment;
    private String categoryProblem;
    private String urgencyLevel;
    private String dateReport;
    private String internetAccess; // Se descartará
    private String governmentPreAttention;
    private String ruralArea;
}
