package com.senasoft.comunidataapi.chat.service.report;

import com.senasoft.comunidataapi.chat.dto.response.ai.BaseDynamicResponseDTO;
import com.senasoft.comunidataapi.chat.dto.response.ai.SimpleTextResponseDTO;
import com.senasoft.comunidataapi.chat.service.factory.DynamicResponseFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.io.File;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Implementación del servicio de generación de reportes.
 *
 * <p>NOTA: Esta implementación es un stub básico. Para generación de reportes ciudadanos,
 * utiliza {@link CitizenReportGenerationService} que proporciona funcionalidad completa
 * con análisis de IA, gráficos y métricas detalladas.
 *
 * <p>Este servicio se mantiene para compatibilidad con la interfaz {@link ReportGenerationService}
 * utilizada en otros componentes del sistema.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ReportGenerationServiceImpl implements ReportGenerationService {

    private final DynamicResponseFactory responseFactory;
    private final Map<String, ReportMetadata> reportMetadataCache = new ConcurrentHashMap<>();

    /**
     * Genera una respuesta indicando que el reporte está siendo procesado.
     *
     * <p>TODO: Implementar generación de reportes según las necesidades del sistema ComuniData.
     * Por ahora, se recomienda usar CitizenReportGenerationService para reportes ciudadanos.
     *
     * @param prompt Prompt del usuario
     * @param functionName Nombre de la función que solicita el reporte
     * @param data Datos para el reporte
     * @param reportType Tipo de reporte (PDF, EXCEL, etc.)
     * @return Respuesta DTO indicando el estado del reporte
     */
    @Override
    public BaseDynamicResponseDTO generateReportResponse(
            String prompt, String functionName, Object data, String reportType) {

        log.info("Solicitud de generación de reporte - Función: {}, Tipo: {}", functionName, reportType);

        // Por ahora, retornar una respuesta simple
        String message = String.format(
            "La generación de reportes de tipo '%s' está en desarrollo. " +
            "Para reportes de análisis ciudadano, el sistema utiliza el servicio especializado. " +
            "Función solicitada: %s",
            reportType, functionName
        );

        return responseFactory.createSimpleTextResponse(prompt, message);
    }

    /**
     * Descarga un reporte previamente generado.
     *
     * @param reportId ID del reporte a descargar
     * @return ResponseEntity con el recurso del reporte
     */
    @Override
    public ResponseEntity<Resource> downloadReport(String reportId) {
        log.info("Solicitud de descarga de reporte: {}", reportId);

        ReportMetadata metadata = reportMetadataCache.get(reportId);

        if (metadata == null) {
            log.warn("Reporte no encontrado: {}", reportId);
            return ResponseEntity.notFound().build();
        }

        if (LocalDateTime.now().isAfter(metadata.getExpirationDate())) {
            log.warn("Reporte expirado: {}", reportId);
            reportMetadataCache.remove(reportId);

            File file = new File(metadata.getFilePath());
            if (file.exists()) {
                file.delete();
            }

            return ResponseEntity.notFound().build();
        }

        File reportFile = new File(metadata.getFilePath());
        if (!reportFile.exists()) {
            log.error("Archivo de reporte no encontrado en el sistema: {}", metadata.getFilePath());
            return ResponseEntity.notFound().build();
        }

        Resource resource = new FileSystemResource(reportFile);

        MediaType mediaType = "PDF".equalsIgnoreCase(metadata.getReportType())
            ? MediaType.APPLICATION_PDF
            : MediaType.APPLICATION_OCTET_STREAM;

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                       "attachment; filename=\"" + metadata.getFileName() + "\"")
                .contentType(mediaType)
                .contentLength(reportFile.length())
                .body(resource);
    }

    /**
     * Clase interna para almacenar metadata de reportes generados.
     */
    private static class ReportMetadata {
        private final String reportId;
        private final String filePath;
        private final String fileName;
        private final String reportType;
        private final String functionName;
        private final LocalDateTime expirationDate;

        public ReportMetadata(
                String reportId,
                String filePath,
                String fileName,
                String reportType,
                String functionName,
                LocalDateTime expirationDate) {
            this.reportId = reportId;
            this.filePath = filePath;
            this.fileName = fileName;
            this.reportType = reportType;
            this.functionName = functionName;
            this.expirationDate = expirationDate;
        }

        public String getReportId() {
            return reportId;
        }

        public String getFilePath() {
            return filePath;
        }

        public String getFileName() {
            return fileName;
        }

        public String getReportType() {
            return reportType;
        }

        public String getFunctionName() {
            return functionName;
        }

        public LocalDateTime getExpirationDate() {
            return expirationDate;
        }
    }
}
