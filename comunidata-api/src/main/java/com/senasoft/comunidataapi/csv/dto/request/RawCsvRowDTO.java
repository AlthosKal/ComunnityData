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
    private String nombre; // Se descartará por privacidad
    private String edad;
    private String genero; // Se descartará por privacidad
    private String ciudad;
    private String comentario;
    private String categoriaProblema;
    private String nivelUrgencia;
    private String fechaReporte;
    private String accesoInternet; // Se descartará
    private String atencionPreviaGobierno;
    private String zonaRural;
}
