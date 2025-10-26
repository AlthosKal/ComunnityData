package com.senasoft.comunidataapi.csv.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO para enviar un reporte individual a IBM Granite para validación.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GraniteValidationRequestDTO {
    private String id;
    private String comentario;
    private String categoriaProblema;
    private String ciudad;
    private Integer edad;
}
