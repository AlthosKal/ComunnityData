package com.senasoft.comunidataapi.csv.service;

import com.senasoft.comunidataapi.csv.dto.response.BatchProcessingStatusDTO;
import com.senasoft.comunidataapi.csv.dto.response.CitizenReportResponseDTO;
import com.senasoft.comunidataapi.csv.dto.response.CsvUploadResponseDTO;
import java.io.InputStream;
import java.util.List;
import org.springframework.web.multipart.MultipartFile;

/**
 * Servicio orquestador que coordina todo el flujo de procesamiento de CSV.
 *
 * <p>Flujo: 1. Normalización del CSV 2. Procesamiento con IBM Granite (validación y detección de
 * sesgos) 3. Generación de embeddings con OpenAI 4. Guardado en MongoDB
 */
public interface CsvProcessingOrchestrator {

    /**
     * Procesa un archivo CSV completo con todo el pipeline.
     *
     * @param file Archivo CSV cargado
     * @param procesarInmediatamente Si se debe procesar con IA inmediatamente
     * @return Respuesta con estadísticas del procesamiento
     */
    CsvUploadResponseDTO processCSV(MultipartFile file, Boolean procesarInmediatamente);

    /**
     * Obtiene todos los reportes filtrados y normalizados.
     *
     * @return Lista de reportes ciudadanos
     */
    List<CitizenReportResponseDTO> getAllProcessedReports();

    /**
     * Obtiene un reporte específico por ID.
     *
     * @param id ID del reporte
     * @return Reporte ciudadano
     */
    CitizenReportResponseDTO getReportById(String id);

    /**
     * Exporta reportes filtrados como CSV.
     *
     * @param reportIds IDs de los reportes a exportar (null = todos)
     * @return Contenido del CSV como bytes
     */
    byte[] exportReportsAsCsv(List<String> reportIds);

    /**
     * Obtiene el estado del procesamiento de un batch.
     *
     * @param batchId ID del batch
     * @return Estado del procesamiento
     */
    BatchProcessingStatusDTO getBatchStatus(String batchId);
}
