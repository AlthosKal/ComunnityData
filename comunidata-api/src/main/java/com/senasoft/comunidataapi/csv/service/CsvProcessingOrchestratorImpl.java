package com.senasoft.comunidataapi.csv.service;

import com.senasoft.comunidataapi.csv.dto.response.CitizenReportResponseDTO;
import com.senasoft.comunidataapi.csv.dto.response.CsvUploadResponseDTO;
import com.senasoft.comunidataapi.csv.entity.CitizenReport;
import com.senasoft.comunidataapi.csv.enums.ProcessingStatus;
import com.senasoft.comunidataapi.csv.mapper.CitizenReportMapper;
import com.senasoft.comunidataapi.csv.repository.CitizenReportRepository;
import com.senasoft.comunidataapi.csv.service.normalization.CsvNormalizationService;
import com.senasoft.comunidataapi.csv.service.processing.EmbeddingGenerationService;
import com.senasoft.comunidataapi.csv.service.processing.GraniteProcessingService;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

/** Implementación del orquestador de procesamiento de CSV. */
@Slf4j
@Service
@RequiredArgsConstructor
public class CsvProcessingOrchestratorImpl implements CsvProcessingOrchestrator {

    private final CsvNormalizationService normalizationService;
    private final GraniteProcessingService graniteService;
    private final EmbeddingGenerationService embeddingService;
    private final CitizenReportRepository repository;
    private final CitizenReportMapper mapper;

    @Override
    @Transactional
    public CsvUploadResponseDTO processCSV(MultipartFile file, Boolean procesarInmediatamente) {
        String batchId = UUID.randomUUID().toString();
        log.info("Starting CSV processing for batch {}", batchId);

        try {
            // 1. Normalizar CSV
            List<CitizenReport> normalizedReports =
                    normalizationService.parseAndNormalizeCsv(file.getInputStream(), batchId);

            log.info("Normalized {} reports from CSV", normalizedReports.size());

            // 2. Guardar reportes normalizados en MongoDB
            List<CitizenReport> savedReports = repository.saveAll(normalizedReports);

            int registrosConError = 0;

            // 3. Procesar con IA si se solicita
            if (Boolean.TRUE.equals(procesarInmediatamente)) {
                log.info("Processing reports with IBM Granite...");

                // Procesar con Granite
                List<CitizenReport> processedReports =
                        graniteService.processReportsInBatches(savedReports, batchId);

                // Filtrar reportes válidos (sin errores)
                List<CitizenReport> validReports =
                        processedReports.stream()
                                .filter(
                                        r ->
                                                !ProcessingStatus.ERROR.equals(
                                                        r.getProcessingStatus()))
                                .collect(Collectors.toList());

                registrosConError = processedReports.size() - validReports.size();

                // Generar embeddings para reportes válidos
                if (!validReports.isEmpty()) {
                    log.info("Generating embeddings for {} valid reports", validReports.size());
                    List<CitizenReport> reportsWithEmbeddings =
                            embeddingService.generateEmbeddings(validReports);

                    // Actualizar reportes en DB
                    repository.saveAll(reportsWithEmbeddings);
                }

                // Guardar reportes con error también
                if (registrosConError > 0) {
                    List<CitizenReport> errorReports =
                            processedReports.stream()
                                    .filter(
                                            r ->
                                                    ProcessingStatus.ERROR.equals(
                                                            r.getProcessingStatus()))
                                    .collect(Collectors.toList());
                    repository.saveAll(errorReports);
                }
            }

            return CsvUploadResponseDTO.builder()
                    .message("CSV procesado exitosamente")
                    .totalRecords(normalizedReports.size())
                    .normalizedRecords(normalizedReports.size())
                    .recordsWithErros(registrosConError)
                    .batchId(batchId)
                    .processingStatus(
                            Boolean.TRUE.equals(procesarInmediatamente)
                                    ? "PROCESAMIENTO_COMPLETO"
                                    : "NORMALIZADO")
                    .build();

        } catch (IOException e) {
            log.error("Error processing CSV file", e);
            throw new RuntimeException("Error procesando archivo CSV: " + e.getMessage());
        }
    }

    @Override
    public List<CitizenReportResponseDTO> getAllProcessedReports() {
        List<CitizenReport> reports = repository.findAllCompletedReports();
        return reports.stream().map(mapper::toResponseDTO).collect(Collectors.toList());
    }

    @Override
    public byte[] exportReportsAsCsv(List<String> reportIds) {
        List<CitizenReport> reports;

        if (reportIds == null || reportIds.isEmpty()) {
            reports = repository.findAllValidReports();
        } else {
            reports = repository.findAllById(reportIds);
        }

        return generateCsvBytes(reports);
    }

    // ==================== Helper Methods ====================

    private byte[] generateCsvBytes(List<CitizenReport> reports) {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        try (PrintWriter writer = new PrintWriter(outputStream)) {
            // Header
            writer.println(
                    "ID,Edad,Ciudad,Comentario,Categoría,Nivel Urgencia,Fecha Reporte,Atención Gobierno,Zona,Sesgo Detectado");

            // Rows
            for (CitizenReport report : reports) {
                writer.printf(
                        "%s,%s,%s,\"%s\",%s,%s,%s,%s,%s,%s%n",
                        report.getId(),
                        report.getAge() != null ? report.getAge() : "",
                        report.getCity() != null ? report.getCity() : "",
                        report.getComment() != null
                                ? report.getComment().replace("\"", "\"\"")
                                : "",
                        report.getCategoryProblem() != null
                                ? report.getCategoryProblem().getDisplayName()
                                : "",
                        report.getUrgencyLevel() != null
                                ? report.getUrgencyLevel().getDisplayName()
                                : "",
                        report.getReportDate() != null
                                ? report.getReportDate().format(DateTimeFormatter.ISO_LOCAL_DATE)
                                : "",
                        report.getGovernmentPreAttention() != null
                                ? report.getGovernmentPreAttention()
                                : "",
                        report.getArea() != null ? report.getArea().getDisplayName() : "",
                        report.getBiasDetected() != null ? report.getBiasDetected() : "");
            }

            writer.flush();
        }

        return outputStream.toByteArray();
    }
}
