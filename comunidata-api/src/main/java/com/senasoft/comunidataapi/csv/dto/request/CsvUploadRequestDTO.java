package com.senasoft.comunidataapi.csv.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.web.multipart.MultipartFile;

/**
 * DTO para la carga de archivos CSV con reportes ciudadanos.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CsvUploadRequestDTO {

    @NotNull(message = "El archivo CSV es requerido")
    private MultipartFile file;

    private String descripcion; // Descripción opcional del dataset

    private Boolean procesarInmediatamente; // Si se debe procesar inmediatamente con IA
}
