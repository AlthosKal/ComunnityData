package com.senasoft.comunidataapi.csv.controller;

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
 * <p>Endpoints: 1. POST /csv - Cargar y procesar CSV 2. GET /csv - Listar reportes filtrados y
 * normalizados 3. GET /csv/export - Exportar reportes como CSV
 */
@Slf4j
@RestController
@RequestMapping("/csv")
@RequiredArgsConstructor
@Tag(name = "CSV Management", description = "API para gestión de reportes ciudadanos en CSV")
public class CsvController {

    private final CsvProcessingOrchestrator orchestrator;

    // ==================== ENDPOINT 1: Lista de los Csv ya categorizados y normalizados
    // ====================

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
    public ResponseEntity<?> getAllProcessedCsvs() {
        log.info("Request to get all processed reports");
        List<CitizenReportResponseDTO> reports = orchestrator.getAllProcessedReports();
        log.info("Returning {} processed reports", reports.size());
        return ResponseEntity.ok(reports);
    }

    // ==================== ENDPOINT 2: Exportar un CSV ya categorizado y ya
    // normalizado====================

    /**
     * Endpoint 3: Exportar CSV ya filtrado y normalizado.
     *
     * <p>Exporta reportes procesados en formato CSV para descarga. Puede exportar todos los
     * reportes o un conjunto específico por IDs.
     *
     * @return Archivo CSV descargable
     */
    @GetMapping(value = "/{id}", produces = "text/csv")
    @Operation(
            summary = "Exportar CSV filtrado y normalizado",
            description =
                    "Exporta todos los reportes ciudadanos filtrados y normalizados en formato CSV")
    public ResponseEntity<?> exportAllReports(@PathVariable Integer id) {
        log.info("Request to export all reports as CSV");
        return buildCsvResponse(orchestrator.exportReportsAsCsv(null));
    }

    // ==================== Helper Methods ====================

    private ResponseEntity<byte[]> buildCsvResponse(byte[] csvBytes) {
        String filename =
                String.format(
                        "reportes_ciudadanos_%s.csv",
                        LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")));

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType("text/csv"));
        headers.setContentDispositionFormData("attachment", filename);
        headers.setContentLength(csvBytes.length);

        log.info("Exporting CSV file: {} ({} bytes)", filename, csvBytes.length);
        return ResponseEntity.ok().headers(headers).body(csvBytes);
    }

    // ==================== ENDPOINT 3: Importar un CSV para su categorización y normalización
    // ====================

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
    @PostMapping(value = "/add", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
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
                    response.getTotalRecords(),
                    response.getRecordsWithErros());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error processing CSV upload", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}
