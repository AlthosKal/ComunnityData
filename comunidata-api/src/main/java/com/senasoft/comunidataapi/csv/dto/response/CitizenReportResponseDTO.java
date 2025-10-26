package com.senasoft.comunidataapi.csv.dto.response;

import com.senasoft.comunidataapi.csv.enums.ProblemCategory;
import com.senasoft.comunidataapi.csv.enums.ProcessingStatus;
import com.senasoft.comunidataapi.csv.enums.UrgencyLevel;
import com.senasoft.comunidataapi.csv.enums.Zone;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO de respuesta para reportes ciudadanos.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CitizenReportResponseDTO {
    private String id;
    private Integer edad;
    private String ciudad;
    private String comentario;
    private ProblemCategory categoriaProblema;
    private UrgencyLevel nivelUrgencia;
    private LocalDate fechaReporte;
    private Boolean atencionPreviaGobierno;
    private Zone zona;
    private Boolean sesgoDetectado;
    private String descripcionSesgo;
    private ProcessingStatus estadoProcesamiento;
    private LocalDateTime fechaCarga;
    private LocalDateTime fechaProcesamiento;
}
