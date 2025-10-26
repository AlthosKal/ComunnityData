package com.senasoft.comunidataapi.csv.entity;

import com.senasoft.comunidataapi.csv.enums.ProblemCategory;
import com.senasoft.comunidataapi.csv.enums.ProcessingStatus;
import com.senasoft.comunidataapi.csv.enums.UrgencyLevel;
import com.senasoft.comunidataapi.csv.enums.Zone;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * Entidad que representa un reporte ciudadano en el sistema ComuniData.
 *
 * <p>Esta entidad almacena reportes ciudadanos sobre problemas comunitarios en áreas de educación,
 * salud, medio ambiente y seguridad. Incluye el embedding vectorial del comentario para búsquedas
 * semánticas mediante RAG.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "citizen_reports")
public class CitizenReport {

    @Id private String id;

    // Campos normalizados del CSV
    private Integer edad;

    @Indexed private String ciudad;

    private String comentario;

    @Indexed private ProblemCategory categoriaProblema;

    private UrgencyLevel nivelUrgencia;

    @Indexed private LocalDate fechaReporte;

    private Boolean atencionPreviaGobierno;

    @Indexed private Zone zona;

    // Campos de procesamiento IA
    @Indexed private Boolean sesgoDetectado;

    private String descripcionSesgo;

    private String categoriaOriginal; // Categoría antes de validación de IA

    // Vector embedding para RAG (text-embedding-3-small genera 1536 dimensiones)
    private List<Double> embedding;

    // Control de procesamiento
    @Indexed private ProcessingStatus estadoProcesamiento;

    @Indexed private LocalDateTime fechaCarga;

    private LocalDateTime fechaProcesamiento;

    private String errorMensaje;

    // Metadatos adicionales
    private String batchId; // ID del batch de procesamiento

    private Integer batchIndex; // Posición en el batch

    // Campos raw para auditoría (opcional)
    private String comentarioOriginal; // Comentario antes de normalización
}
