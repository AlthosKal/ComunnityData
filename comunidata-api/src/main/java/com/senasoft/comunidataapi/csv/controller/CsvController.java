package com.senasoft.comunidataapi.csv.controller;

import com.senasoft.comunidataapi.csv.dto.response.BatchProcessingStatusDTO;
import com.senasoft.comunidataapi.csv.dto.response.CitizenReportResponseDTO;
import com.senasoft.comunidataapi.csv.dto.response.CsvUploadResponseDTO;
import com.senasoft.comunidataapi.csv.service.CsvProcessingOrchestrator;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/**
 * Controlador único para gestión de CSV de reportes ciudadanos.
 *
 * <p>Endpoints:
 * 1. POST /csv - Cargar y procesar CSV
 * 2. GET /csv - Listar reportes filtrados y normalizados
 * 3. GET /csv/export - Exportar reportes como CSV
 */
@Slf4j
@RestController
@RequestMapping("/csv")
@RequiredArgsConstructor
@Tag(name = "CSV Management", description = "API para gestión de reportes ciudadanos en CSV")
public class CsvController {

    private final CsvProcessingOrchestrator orchestrator;

    // ==================== ENDPOINT 1: Cargar CSV ====================

    /**
     * Endpoint 1: Carga de CSV para filtración y normalización.
     *
     * <p>Recibe un archivo CSV, lo normaliza y opcionalmente lo procesa con IA (IBM Granite +
     * OpenAI Embeddings).
     *
     * @param file Archivo CSV con reportes ciudadanos
     * @param procesarInmediatamente Si true, procesa con IA inmediatamente
     * @return Respuesta con estadísticas del procesamiento
     */
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(
            summary = "Cargar CSV para filtración y normalización",
            description =
                    "Carga un archivo CSV con reportes ciudadanos, lo normaliza y opcionalmente lo procesa con IA (IBM Granite para detección de sesgos + OpenAI para embeddings)")
    public ResponseEntity<CsvUploadResponseDTO> uploadCsv(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "procesarInmediatamente", defaultValue = "true")
                    Boolean procesarInmediatamente) {

        log.info(
                "Received CSV upload request. File: {}, Size: {} bytes",
                file.getOriginalFilename(),
                file.getSize());

        // Validar archivo
        if (file.isEmpty()) {
            log.error("Uploaded file is empty");
            return ResponseEntity.badRequest().build();
        }

        // Validar extensión
        String filename = file.getOriginalFilename();
        if (filename == null || !filename.toLowerCase().endsWith(".csv")) {
            log.error("Invalid file extension. Expected .csv, got: {}", filename);
            return ResponseEntity.status(HttpStatus.UNSUPPORTED_MEDIA_TYPE).build();
        }

        try {
            CsvUploadResponseDTO response = orchestrator.processCSV(file, procesarInmediatamente);
            log.info(
                    "CSV processed successfully. Batch ID: {}, Total: {}, Errors: {}",
                    response.getBatchId(),
                    response.getTotalRegistros(),
                    response.getRegistrosConError());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error processing CSV upload", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // ==================== ENDPOINT 2: Listar Reportes ====================

    /**
     * Endpoint 2: Obtener lista de CSV ya filtrados y normalizados.
     *
     * <p>Retorna todos los reportes ciudadanos que han sido procesados exitosamente.
     *
     * @return Lista de reportes ciudadanos
     */
    @GetMapping
    @Operation(
            summary = "Obtener lista de CSV filtrados y normalizados",
            description =
                    "Retorna todos los reportes ciudadanos que han sido procesados, filtrados y normalizados exitosamente")
    public ResponseEntity<List<CitizenReportResponseDTO>> getAllReports() {
        log.info("Request to get all processed reports");
        List<CitizenReportResponseDTO> reports = orchestrator.getAllProcessedReports();
        log.info("Returning {} processed reports", reports.size());
        return ResponseEntity.ok(reports);
    }

    /**
     * Obtener reporte por ID.
     *
     * @param id ID del reporte
     * @return Reporte ciudadano
     */
    @GetMapping("/{id}")
    @Operation(
            summary = "Obtener reporte por ID",
            description = "Obtiene un reporte ciudadano específico por su ID")
    public ResponseEntity<CitizenReportResponseDTO> getReportById(@PathVariable String id) {
        log.info("Request to get report by ID: {}", id);
        try {
            CitizenReportResponseDTO report = orchestrator.getReportById(id);
            return ResponseEntity.ok(report);
        } catch (RuntimeException e) {
            log.error("Report not found with ID: {}", id);
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Obtener estado del procesamiento de un batch.
     *
     * @param batchId ID del batch
     * @return Estado del procesamiento
     */
    @GetMapping("/batch/{batchId}/status")
    @Operation(
            summary = "Obtener estado del batch",
            description =
                    "Consulta el estado de procesamiento de un batch específico (útil para tracking de progreso)")
    public ResponseEntity<BatchProcessingStatusDTO> getBatchStatus(@PathVariable String batchId) {
        log.info("Request to get batch status for: {}", batchId);
        try {
            BatchProcessingStatusDTO status = orchestrator.getBatchStatus(batchId);
            return ResponseEntity.ok(status);
        } catch (RuntimeException e) {
            log.error("Batch not found: {}", batchId);
            return ResponseEntity.notFound().build();
        }
    }

    // ==================== ENDPOINT 3: Exportar CSV ====================

    /**
     * Endpoint 3: Exportar CSV ya filtrado y normalizado.
     *
     * <p>Exporta reportes procesados en formato CSV para descarga. Puede exportar todos los
     * reportes o un conjunto específico por IDs.
     *
     * @param reportIds Lista opcional de IDs de reportes a exportar (si es null, exporta todos)
     * @return Archivo CSV descargable
     */
    @GetMapping(value = "/export", produces = "text/csv")
    @Operation(
            summary = "Exportar CSV filtrado y normalizado",
            description =
                    "Exporta todos los reportes ciudadanos filtrados y normalizados en formato CSV")
    public ResponseEntity<byte[]> exportAllReports() {
        log.info("Request to export all reports as CSV");
        return buildCsvResponse(orchestrator.exportReportsAsCsv(null));
    }

    /**
     * Exportar reportes seleccionados por IDs.
     *
     * @param reportIds Lista de IDs de reportes a exportar
     * @return Archivo CSV descargable
     */
    @PostMapping(value = "/export", produces = "text/csv")
    @Operation(
            summary = "Exportar reportes seleccionados",
            description = "Exporta un conjunto específico de reportes por sus IDs")
    public ResponseEntity<byte[]> exportSelectedReports(@RequestBody List<String> reportIds) {
        log.info("Request to export {} selected reports", reportIds.size());
        return buildCsvResponse(orchestrator.exportReportsAsCsv(reportIds));
    }

    // ==================== Helper Methods ====================

    private ResponseEntity<byte[]> buildCsvResponse(byte[] csvBytes) {
        String filename =
                String.format(
                        "reportes_ciudadanos_%s.csv",
                        LocalDateTime.now()
                                .format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")));

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType("text/csv"));
        headers.setContentDispositionFormData("attachment", filename);
        headers.setContentLength(csvBytes.length);

        log.info("Exporting CSV file: {} ({} bytes)", filename, csvBytes.length);
        return ResponseEntity.ok().headers(headers).body(csvBytes);
    }
}
