package com.senasoft.comunidataapi.csv.service.processing;

import com.senasoft.comunidataapi.csv.entity.CitizenReport;
import com.senasoft.comunidataapi.csv.enums.ProcessingStatus;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.embedding.EmbeddingResponse;
import org.springframework.stereotype.Service;

/**
 * Implementación del servicio de generación de embeddings.
 *
 * <p>Usa OpenAI text-embedding-3-small (1536 dimensiones) para generar embeddings vectoriales de
 * los comentarios de reportes ciudadanos.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EmbeddingGenerationServiceImpl implements EmbeddingGenerationService {

    private final EmbeddingModel embeddingModel;

    @Override
    public List<CitizenReport> generateEmbeddings(List<CitizenReport> reports) {
        log.info("Generating embeddings for {} reports", reports.size());

        List<CitizenReport> processedReports = new ArrayList<>();

        for (CitizenReport report : reports) {
            try {
                CitizenReport processedReport = generateEmbedding(report);
                processedReports.add(processedReport);
            } catch (Exception e) {
                log.error("Error generating embedding for report {}", report.getId(), e);
                report.setProcessingStatus(ProcessingStatus.ERROR);
                report.setErrorMessage("Error generando embedding: " + e.getMessage());
                processedReports.add(report);
            }
        }

        log.info(
                "Successfully generated embeddings for {}/{} reports",
                processedReports.stream().filter(r -> r.getEmbedding() != null).count(),
                reports.size());

        return processedReports;
    }

    @Override
    @CircuitBreaker(name = "app-resilience-config", fallbackMethod = "generateEmbeddingFallback")
    @Retry(name = "app-resilience-config")
    public CitizenReport generateEmbedding(CitizenReport report) {
        // Validar que el comentario existe
        if (report.getComment() == null || report.getComment().trim().isEmpty()) {
            log.warn("Report {} has no comment, skipping embedding generation", report.getId());
            report.setProcessingStatus(ProcessingStatus.ERROR);
            report.setErrorMessage("No hay comentario para generar embedding");
            return report;
        }

        try {
            // Actualizar estado
            report.setProcessingStatus(ProcessingStatus.GENERANDO_EMBEDDINGS);

            // Crear documento con metadatos para mejor contexto
            String textToEmbed = buildEmbeddingText(report);

            // Generar embedding
            EmbeddingResponse response = embeddingModel.embedForResponse(List.of(textToEmbed));

            if (response != null && !response.getResults().isEmpty()) {
                // Convertir float[] a List<Double>
                float[] embeddingArray = response.getResults().get(0).getOutput();
                List<Double> embedding = new ArrayList<>();
                for (float value : embeddingArray) {
                    embedding.add((double) value);
                }

                // Guardar embedding en el reporte
                report.setEmbedding(embedding);
                report.setProcessingStatus(ProcessingStatus.COMPLETADO);

                log.debug(
                        "Generated embedding for report {} with {} dimensions",
                        report.getId(),
                        embedding.size());
            } else {
                throw new RuntimeException("Empty embedding response from OpenAI");
            }

        } catch (Exception e) {
            log.error("Error generating embedding for report {}", report.getId(), e);
            report.setProcessingStatus(ProcessingStatus.ERROR);
            report.setErrorMessage("Error generando embedding: " + e.getMessage());
        }

        return report;
    }

    // ==================== Fallback Methods ====================

    public CitizenReport generateEmbeddingFallback(CitizenReport report, Exception e) {
        log.error("Circuit breaker activated for embedding generation. Fallback method called.", e);
        report.setProcessingStatus(ProcessingStatus.ERROR);
        report.setErrorMessage("Servicio de embeddings temporalmente no disponible");
        return report;
    }

    // ==================== Helper Methods ====================

    /**
     * Construye el texto para embedding incluyendo contexto del reporte.
     *
     * <p>Incluye comentario + metadatos relevantes para mejorar la búsqueda semántica.
     */
    private String buildEmbeddingText(CitizenReport report) {
        StringBuilder text = new StringBuilder();

        // Comentario principal (más peso en el embedding)
        text.append(report.getComment());

        // Agregar contexto adicional
        if (report.getCategoryProblem() != null) {
            text.append(" [Categoría: ")
                    .append(report.getCategoryProblem().getDisplayName())
                    .append("]");
        }

        if (report.getCity() != null) {
            text.append(" [Ciudad: ").append(report.getCity()).append("]");
        }

        if (report.getUrgencyLevel() != null) {
            text.append(" [Urgencia: ")
                    .append(report.getUrgencyLevel().getDisplayName())
                    .append("]");
        }

        return text.toString();
    }
}
