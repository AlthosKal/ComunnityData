package com.senasoft.comunidataapi.csv.service.processing;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.senasoft.comunidataapi.csv.entity.CitizenReport;
import com.senasoft.comunidataapi.csv.enums.ProblemCategory;
import com.senasoft.comunidataapi.csv.enums.ProcessingStatus;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.watsonx.WatsonxAiChatModel;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

/**
 * Implementación del servicio de procesamiento con IBM Granite.
 *
 * <p>Características: - Procesamiento en batches de 50 reportes - Paralelización con 3 batches
 * simultáneos usando ExecutorService - Retry logic con exponential backoff - Circuit breaker para
 * resiliencia - Validación de categorías y detección de sesgos
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GraniteProcessingServiceImpl implements GraniteProcessingService {

    @Qualifier("watsonxAiChatModel")
    private final WatsonxAiChatModel watsonxChatModel;

    private final ObjectMapper objectMapper;

    private static final int BATCH_SIZE = 50;
    private static final int PARALLEL_BATCHES = 3;
    private static final int THREAD_POOL_SIZE = 3;

    private final ExecutorService executorService = Executors.newFixedThreadPool(THREAD_POOL_SIZE);

    @Override
    public List<CitizenReport> processReportsInBatches(
            List<CitizenReport> reports, String batchId) {
        log.info("Starting batch processing for {} reports in batch {}", reports.size(), batchId);

        // Dividir en batches de 50
        List<List<CitizenReport>> batches = partitionList(reports, BATCH_SIZE);
        log.info("Created {} batches of max {} reports each", batches.size(), BATCH_SIZE);

        List<CitizenReport> processedReports = new ArrayList<>();
        List<Future<List<CitizenReport>>> futures = new ArrayList<>();

        // Procesar batches en paralelo (max 3 simultáneos)
        for (int i = 0; i < batches.size(); i++) {
            final List<CitizenReport> batch = batches.get(i);
            final int batchNumber = i + 1;

            Future<List<CitizenReport>> future =
                    executorService.submit(
                            () -> {
                                log.info(
                                        "Processing batch {}/{} with {} reports",
                                        batchNumber,
                                        batches.size(),
                                        batch.size());
                                return processSingleBatch(batch);
                            });

            futures.add(future);

            // Limitar a 3 batches paralelos
            if (futures.size() >= PARALLEL_BATCHES || i == batches.size() - 1) {
                // Esperar a que terminen los batches actuales
                for (Future<List<CitizenReport>> f : futures) {
                    try {
                        processedReports.addAll(f.get(10, TimeUnit.MINUTES));
                    } catch (InterruptedException | ExecutionException | TimeoutException e) {
                        log.error("Error processing batch", e);
                    }
                }
                futures.clear();
            }
        }

        log.info(
                "Completed batch processing. Processed {}/{} reports successfully",
                processedReports.size(),
                reports.size());

        return processedReports;
    }

    @Override
    @CircuitBreaker(name = "app-resilience-config", fallbackMethod = "processSingleBatchFallback")
    @Retry(name = "app-resilience-config")
    public List<CitizenReport> processSingleBatch(List<CitizenReport> reportBatch) {
        if (reportBatch.isEmpty()) {
            return reportBatch;
        }

        log.debug("Processing batch of {} reports with IBM Granite", reportBatch.size());

        try {
            // Construir el prompt para el batch
            String batchPrompt = buildBatchValidationPrompt(reportBatch);

            // Llamar a IBM Granite
            Prompt prompt = new Prompt(batchPrompt);
            String response = watsonxChatModel.call(prompt).getResult().getOutput().getText();

            log.debug("Received response from IBM Granite: {}", response);

            // Parsear respuesta JSON
            List<Map<String, Object>> validations = parseGraniteResponse(response);

            // Actualizar reportes con validaciones
            updateReportsWithValidations(reportBatch, validations);

            // Marcar como procesados
            reportBatch.forEach(
                    report -> report.setProcessingStatus(ProcessingStatus.PROCESANDO_IA));

            return reportBatch;

        } catch (Exception e) {
            log.error("Error processing batch with IBM Granite", e);
            // Marcar reportes con error
            reportBatch.forEach(
                    report -> {
                        report.setProcessingStatus(ProcessingStatus.ERROR);
                        report.setErrorMessage("Error en procesamiento IA: " + e.getMessage());
                    });
            return reportBatch;
        }
    }

    @Override
    public CitizenReport validateReport(CitizenReport report) {
        List<CitizenReport> batch = List.of(report);
        List<CitizenReport> processed = processSingleBatch(batch);
        return processed.isEmpty() ? report : processed.get(0);
    }

    // ==================== Fallback Methods ====================

    public List<CitizenReport> processSingleBatchFallback(
            List<CitizenReport> reportBatch, Exception e) {
        log.error("Circuit breaker activated. Fallback method called.", e);
        reportBatch.forEach(
                report -> {
                    report.setProcessingStatus(ProcessingStatus.ERROR);
                    report.setErrorMessage(
                            "Servicio de IA temporalmente no disponible. Reintentando...");
                });
        return reportBatch;
    }

    // ==================== Prompt Building ====================

    /**
     * Construye el prompt para validación en batch con IBM Granite.
     *
     * <p>El prompt instruye a Granite para: 1. Validar si la categoría es correcta 2. Detectar
     * sesgos en el comentario 3. Determinar si el reporte es legítimo o spam
     */
    private String buildBatchValidationPrompt(List<CitizenReport> reports) {
        StringBuilder prompt = new StringBuilder();

        prompt.append("Eres un sistema de validación de reportes ciudadanos. ")
                .append(
                        "Tu tarea es analizar cada reporte y detectar sesgos, validar categorías y determinar legitimidad.\n\n");

        prompt.append("CATEGORÍAS VÁLIDAS:\n");
        prompt.append("- Salud: problemas de salud pública, hospitales, medicamentos, etc.\n");
        prompt.append(
                "- Educación: problemas educativos, escuelas, profesores, infraestructura educativa.\n");
        prompt.append("- Medio Ambiente: contaminación, basuras, deforestación, agua, aire.\n");
        prompt.append("- Seguridad: delincuencia, violencia, iluminación, policía.\n\n");

        prompt.append("SESGOS A DETECTAR:\n");
        prompt.append("- Lenguaje discriminatorio (racismo, sexismo, xenofobia)\n");
        prompt.append("- Información claramente falsa o exagerada\n");
        prompt.append("- Ataques personales o difamación\n");
        prompt.append("- Propaganda política\n\n");

        prompt.append(
                "Para cada reporte, responde ÚNICAMENTE con un array JSON (sin texto adicional). Cada objeto debe tener:\n");
        prompt.append("- id: identificador del reporte\n");
        prompt.append("- sesgoDetectado: true/false\n");
        prompt.append("- descripcionSesgo: descripción del sesgo si existe, o null\n");
        prompt.append("- categoriaValidada: la categoría correcta validada\n");
        prompt.append("- esReporteLegitimo: true/false\n\n");

        prompt.append("REPORTES A ANALIZAR:\n");
        for (int i = 0; i < reports.size(); i++) {
            CitizenReport report = reports.get(i);
            prompt.append(String.format("%d. ID: %s\n", i + 1, report.getId()));
            prompt.append(String.format("   Comentario: %s\n", report.getComment()));
            prompt.append(
                    String.format(
                            "   Categoría sugerida: %s\n",
                            report.getCategoryProblem() != null
                                    ? report.getCategoryProblem().getDisplayName()
                                    : "No especificada"));
            prompt.append(String.format("   Ciudad: %s\n", report.getCity()));
            prompt.append("\n");
        }

        prompt.append(
                "\nResponde SOLO con el array JSON, sin explicaciones adicionales. Formato: [{\"id\":\"...\", \"sesgoDetectado\":true, \"descripcionSesgo\":\"...\", \"categoriaValidada\":\"Salud\", \"esReporteLegitimo\":true}, ...]\n");

        return prompt.toString();
    }

    // ==================== Response Parsing ====================

    /** Parsea la respuesta JSON de IBM Granite. */
    private List<Map<String, Object>> parseGraniteResponse(String response) {
        try {
            // Limpiar la respuesta de posible texto adicional
            String cleanedResponse = extractJsonArray(response);

            return objectMapper.readValue(
                    cleanedResponse, new TypeReference<List<Map<String, Object>>>() {});
        } catch (Exception e) {
            log.error("Error parsing Granite response: {}", response, e);
            return new ArrayList<>();
        }
    }

    /** Extrae el array JSON de la respuesta, ignorando texto adicional. */
    private String extractJsonArray(String response) {
        int startIndex = response.indexOf('[');
        int endIndex = response.lastIndexOf(']');

        if (startIndex >= 0 && endIndex > startIndex) {
            return response.substring(startIndex, endIndex + 1);
        }

        // Si no encuentra corchetes, intentar con todo el string
        return response.trim();
    }

    /** Actualiza los reportes con las validaciones de Granite. */
    private void updateReportsWithValidations(
            List<CitizenReport> reports, List<Map<String, Object>> validations) {

        for (CitizenReport report : reports) {
            // Buscar la validación correspondiente por ID
            validations.stream()
                    .filter(v -> v.get("id") != null && v.get("id").equals(report.getId()))
                    .findFirst()
                    .ifPresent(
                            validation -> {
                                // Actualizar sesgo
                                Boolean sesgoDetectado = (Boolean) validation.get("sesgoDetectado");
                                report.setBiasDetected(
                                        sesgoDetectado != null ? sesgoDetectado : false);

                                String descripcionSesgo =
                                        (String) validation.get("descripcionSesgo");
                                report.setDescriptionBias(descripcionSesgo);

                                // Actualizar categoría validada
                                String categoriaValidada =
                                        (String) validation.get("categoriaValidada");
                                if (categoriaValidada != null) {
                                    ProblemCategory categoria =
                                            ProblemCategory.fromString(categoriaValidada);
                                    if (categoria != null) {
                                        report.setCategoryProblem(categoria);
                                    }
                                }

                                // Verificar legitimidad
                                Boolean esLegitimo = (Boolean) validation.get("esReporteLegitimo");
                                if (esLegitimo != null && !esLegitimo) {
                                    report.setProcessingStatus(ProcessingStatus.ERROR);
                                    report.setErrorMessage("Reporte marcado como no legítimo");
                                }
                            });
        }
    }

    // ==================== Utility Methods ====================

    /** Divide una lista en sublistas de tamaño específico. */
    private <T> List<List<T>> partitionList(List<T> list, int partitionSize) {
        List<List<T>> partitions = new ArrayList<>();
        for (int i = 0; i < list.size(); i += partitionSize) {
            partitions.add(
                    new ArrayList<>(list.subList(i, Math.min(i + partitionSize, list.size()))));
        }
        return partitions;
    }
}
