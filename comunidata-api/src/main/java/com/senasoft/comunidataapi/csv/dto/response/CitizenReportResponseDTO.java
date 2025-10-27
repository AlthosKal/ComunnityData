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

/** DTO de respuesta para reportes ciudadanos. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CitizenReportResponseDTO {
    private String id;
    private Integer age;
    private String city;
    private String comment;
    private ProblemCategory categoryProblem;
    private UrgencyLevel urgencyLevel;
    private LocalDate reportDate;
    private Boolean governmentPreAttention;
    private Zone zone;
    private Boolean biasDetected;
    private String descriptionBias;
    private ProcessingStatus processingStatus;
    private LocalDateTime importDate;
    private LocalDateTime processDate;
}
