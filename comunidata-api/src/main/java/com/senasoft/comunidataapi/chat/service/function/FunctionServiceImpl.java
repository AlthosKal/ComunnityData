package com.senasoft.comunidataapi.chat.service.function;

import com.senasoft.comunidataapi.chat.dto.request.ChatDTO;
import com.senasoft.comunidataapi.chat.service.function.list.FilterByAgeFunction;
import com.senasoft.comunidataapi.chat.service.function.list.FilterByCategoryProblemFunction;
import com.senasoft.comunidataapi.chat.service.function.list.FilterByCityFunction;
import com.senasoft.comunidataapi.chat.service.function.list.FilterByGovernmentAttentionFunction;
import com.senasoft.comunidataapi.chat.service.function.list.FilterByReportDateFunction;
import com.senasoft.comunidataapi.chat.service.function.list.FilterByUrgencyLevelFunction;
import com.senasoft.comunidataapi.chat.service.function.list.FilterByZoneFunction;
import com.senasoft.comunidataapi.chat.service.function.list.GenerateReportFunction;
import com.senasoft.comunidataapi.chat.service.function.list.SemanticSearchFunction;
import com.senasoft.comunidataapi.csv.entity.CitizenReport;
import com.senasoft.comunidataapi.csv.enums.ProblemCategory;
import com.senasoft.comunidataapi.csv.enums.UrgencyLevel;
import com.senasoft.comunidataapi.csv.enums.Zone;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StreamUtils;

/**
 * Servicio de funciones para ComuniData.
 *
 * <p>Gestiona la detección y ejecución de function calling para análisis de reportes ciudadanos.
 *
 * <p>NOTA IMPORTANTE: En una implementación real con Spring AI Native Function Calling, este
 * servicio puede simplificarse significativamente, ya que el modelo GPT-5 puede invocar las
 * funciones directamente sin necesidad de detección manual.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class FunctionServiceImpl implements FunctionService {

    private final FilterByAgeFunction filterByAgeFunction;
    private final FilterByCityFunction filterByCityFunction;
    private final FilterByCategoryProblemFunction filterByCategoryFunction;
    private final FilterByUrgencyLevelFunction filterByUrgencyFunction;
    private final FilterByGovernmentAttentionFunction filterByGovernmentAttentionFunction;
    private final FilterByReportDateFunction filterByReportDateFunction;
    private final FilterByZoneFunction filterByZoneFunction;
    private final SemanticSearchFunction semanticSearchFunction;
    private final GenerateReportFunction generateReportFunction;

    /**
     * Detecta la función apropiada basándose en palabras clave del prompt.
     *
     * <p>NOTA: Este método es un fallback. Con Spring AI Native Function Calling, GPT-5 puede
     * elegir automáticamente la función correcta sin necesidad de esta lógica.
     *
     * @param prompt Texto del usuario
     * @return Nombre de la función detectada
     */
    @Override
    public String detectFunctionFromPrompt(String prompt) {
        String lowerPrompt = prompt.toLowerCase();
        String detectedFunction = "general";

        // Búsqueda semántica (prioridad alta)
        if (lowerPrompt.contains("busca")
                || lowerPrompt.contains("similar")
                || lowerPrompt.contains("parecido")
                || lowerPrompt.contains("encuentra reportes sobre")) {
            detectedFunction = "semanticSearch";
        }
        // Filtro por edad
        else if (lowerPrompt.contains("edad")
                || lowerPrompt.contains("años")
                || (lowerPrompt.contains("jóvenes") || lowerPrompt.contains("jovenes"))
                || lowerPrompt.contains("adultos")) {
            detectedFunction = "filterByAge";
        }
        // Filtro por ciudad
        else if (lowerPrompt.contains("ciudad")
                || lowerPrompt.contains("manizales")
                || lowerPrompt.contains("bogotá")
                || lowerPrompt.contains("cali")
                || lowerPrompt.contains("medellín")) {
            detectedFunction = "filterByCity";
        }
        // Filtro por categoría
        else if (lowerPrompt.contains("salud")
                || lowerPrompt.contains("educación")
                || lowerPrompt.contains("educacion")
                || lowerPrompt.contains("medio ambiente")
                || lowerPrompt.contains("seguridad")
                || lowerPrompt.contains("categoría")
                || lowerPrompt.contains("categoria")) {
            detectedFunction = "filterByCategory";
        }
        // Filtro por urgencia
        else if (lowerPrompt.contains("urgente")
                || lowerPrompt.contains("urgencia")
                || lowerPrompt.contains("prioritario")
                || lowerPrompt.contains("prioridad")) {
            detectedFunction = "filterByUrgency";
        }
        // Filtro por atención gobierno
        else if (lowerPrompt.contains("atendido")
                || lowerPrompt.contains("gobierno")
                || lowerPrompt.contains("sin atención")
                || lowerPrompt.contains("no atendido")) {
            detectedFunction = "filterByGovernmentAttention";
        }
        // Filtro por fecha
        else if (lowerPrompt.contains("fecha")
                || lowerPrompt.contains("último mes")
                || lowerPrompt.contains("ultima semana")
                || lowerPrompt.matches(".*\\d{4}-\\d{2}-\\d{2}.*")) {
            detectedFunction = "filterByReportDate";
        }
        // Filtro por zona
        else if (lowerPrompt.contains("rural")
                || lowerPrompt.contains("urbana")
                || lowerPrompt.contains("zona")) {
            detectedFunction = "filterByZone";
        }
        // Generación de reporte
        else if (lowerPrompt.contains("generar reporte")
                || lowerPrompt.contains("crear reporte")
                || lowerPrompt.contains("reporte pdf")
                || lowerPrompt.contains("informe")) {
            detectedFunction = "generateReport";
        }

        log.info("Detected function '{}' from prompt: '{}'", detectedFunction, prompt);
        return detectedFunction;
    }

    @Override
    public Object getFunctionData(String functionName, ChatDTO request) {
        return getFunctionData(functionName, request, null);
    }

    /**
     * Ejecuta la función detectada y retorna los datos.
     *
     * @param functionName Nombre de la función a ejecutar
     * @param request DTO con el prompt del usuario
     * @param authToken Token de autenticación (no usado actualmente)
     * @return Datos retornados por la función
     */
    @Override
    public Object getFunctionData(String functionName, ChatDTO request, String authToken) {
        try {
            return switch (functionName) {
                case "filterByAge" -> executeFilterByAge(request);
                case "filterByCity" -> executeFilterByCity(request);
                case "filterByCategory" -> executeFilterByCategory(request);
                case "filterByUrgency" -> executeFilterByUrgency(request);
                case "filterByGovernmentAttention" -> executeFilterByGovernmentAttention(request);
                case "filterByReportDate" -> executeFilterByReportDate(request);
                case "filterByZone" -> executeFilterByZone(request);
                case "semanticSearch" -> executeSemanticSearch(request);
                case "generateReport" -> executeGenerateReport(request);
                default -> {
                    log.warn("Unknown function: {}", functionName);
                    yield null;
                }
            };
        } catch (Exception e) {
            log.error("Error executing function '{}': {}", functionName, e.getMessage(), e);
            return null;
        }
    }

    /**
     * Crea un prompt contextual con información sobre los datos de reportes ciudadanos.
     *
     * @param originalPrompt El prompt original del usuario
     * @param functionName La función detectada
     * @param functionData Los datos obtenidos de la función
     * @return Prompt mejorado con contexto
     */
    @Override
    public String createContextualPrompt(
            String originalPrompt, String functionName, Object functionData) {
        StringBuilder contextBuilder = new StringBuilder();

        // Agregar contexto sobre datos disponibles
        contextBuilder.append("📊 CONTEXTO DE REPORTES CIUDADANOS DISPONIBLE:\n\n");

        if (functionData != null) {
            contextBuilder.append(
                    "✅ Tengo acceso a los datos de reportes ciudadanos del sistema ComuniData.\n");
            contextBuilder
                    .append("📈 He consultado información usando: ")
                    .append(getFunctionDisplayName(functionName))
                    .append("\n\n");

            // Agregar información específica según el tipo de datos
            if (functionData instanceof List<?> list) {
                int size = list.size();
                contextBuilder
                        .append("📋 Encontré ")
                        .append(size)
                        .append(" reportes que coinciden con tu consulta.\n");

                if (size > 0) {
                    // Agregar estadísticas básicas si son reportes ciudadanos
                    if (list.get(0) instanceof CitizenReport) {
                        addReportStatistics(contextBuilder, (List<CitizenReport>) list);
                    }
                }
            } else if (functionData instanceof String) {
                contextBuilder.append("🔍 Resultados de búsqueda semántica disponibles.\n");
            }
        } else {
            contextBuilder.append(
                    "⚠️ No se encontraron reportes específicos para esta consulta, pero puedo brindarte información general sobre el sistema.\n");
        }

        contextBuilder.append("\n🗣️ CONSULTA DEL USUARIO:\n");
        contextBuilder.append(originalPrompt);

        contextBuilder.append("\n\n🎯 INSTRUCCIONES PARA LA RESPUESTA:\n");
        contextBuilder.append("- Analiza los datos de reportes ciudadanos proporcionados\n");
        contextBuilder.append(
                "- Si hay datos disponibles, NO menciones que necesitas más información\n");
        contextBuilder.append("- Proporciona insights útiles sobre los problemas reportados\n");
        contextBuilder.append("- Identifica patrones, tendencias o áreas de preocupación\n");
        contextBuilder.append("- Sugiere acciones concretas basadas en los hallazgos\n");
        contextBuilder.append("- Usa un tono profesional, empático y orientado a soluciones\n");
        contextBuilder.append(
                "- Si los datos incluyen información sobre sesgos detectados, menciónalo\n");

        return contextBuilder.toString();
    }

    @Override
    public Prompt getPrompt(String userInput) {
        PromptTemplate promptTemplate = new PromptTemplate(loadPromptFromClasspath());
        Map<String, Object> params = Map.of("prompt", userInput);
        return promptTemplate.create(params);
    }

    @Override
    public Prompt getPrompt(ChatDTO request, String fileContent) {
        PromptTemplate promptTemplate = new PromptTemplate(loadPromptFromClasspath());
        Map<String, Object> params =
                Map.of("fileContent", fileContent, "prompt", request.getPrompt());
        return promptTemplate.create(params);
    }

    // ==================== Métodos de Ejecución de Funciones ====================

    private List<CitizenReport> executeFilterByAge(ChatDTO request) {
        Map<String, Integer> ageParams = extractAgeParameters(request.getPrompt());
        FilterByAgeFunction.Request functionRequest =
                new FilterByAgeFunction.Request(ageParams.get("minAge"), ageParams.get("maxAge"));
        return filterByAgeFunction.apply(functionRequest);
    }

    private List<CitizenReport> executeFilterByCity(ChatDTO request) {
        String city = extractCityParameter(request.getPrompt());
        FilterByCityFunction.Request functionRequest = new FilterByCityFunction.Request(city);
        return filterByCityFunction.apply(functionRequest);
    }

    private List<CitizenReport> executeFilterByCategory(ChatDTO request) {
        String category = extractCategoryParameter(request.getPrompt());
        FilterByCategoryProblemFunction.Request functionRequest =
                new FilterByCategoryProblemFunction.Request(category);
        return filterByCategoryFunction.apply(functionRequest);
    }

    private List<CitizenReport> executeFilterByUrgency(ChatDTO request) {
        String urgency = extractUrgencyParameter(request.getPrompt());
        FilterByUrgencyLevelFunction.Request functionRequest =
                new FilterByUrgencyLevelFunction.Request(urgency);
        return filterByUrgencyFunction.apply(functionRequest);
    }

    private List<CitizenReport> executeFilterByGovernmentAttention(ChatDTO request) {
        Boolean hasAttention = extractGovernmentAttentionParameter(request.getPrompt());
        FilterByGovernmentAttentionFunction.Request functionRequest =
                new FilterByGovernmentAttentionFunction.Request(hasAttention);
        return filterByGovernmentAttentionFunction.apply(functionRequest);
    }

    private List<CitizenReport> executeFilterByReportDate(ChatDTO request) {
        Map<String, String> dateParams = extractDateParameters(request.getPrompt());
        FilterByReportDateFunction.Request functionRequest =
                new FilterByReportDateFunction.Request(
                        dateParams.get("startDate"), dateParams.get("endDate"));
        return filterByReportDateFunction.apply(functionRequest);
    }

    private List<CitizenReport> executeFilterByZone(ChatDTO request) {
        String zone = extractZoneParameter(request.getPrompt());
        FilterByZoneFunction.Request functionRequest = new FilterByZoneFunction.Request(zone);
        return filterByZoneFunction.apply(functionRequest);
    }

    private List<String> executeSemanticSearch(ChatDTO request) {
        String query = request.getPrompt();
        SemanticSearchFunction.Request functionRequest =
                new SemanticSearchFunction.Request(query, 5);
        return semanticSearchFunction.apply(functionRequest);
    }

    private Object executeGenerateReport(ChatDTO request) {
        String analysisType = extractAnalysisType(request.getPrompt());
        // Los filtros se pasan como String JSON, por ahora null (pueden ser extraídos del prompt si
        // es necesario)
        String filters = null;
        GenerateReportFunction.Request functionRequest =
                new GenerateReportFunction.Request(analysisType, filters);
        return generateReportFunction.apply(functionRequest);
    }

    // ==================== Métodos de Extracción de Parámetros ====================

    private Map<String, Integer> extractAgeParameters(String prompt) {
        Map<String, Integer> params = new HashMap<>();
        Pattern agePattern = Pattern.compile("(\\d+)\\s*(a|y|hasta)\\s*(\\d+)\\s*años");
        Matcher matcher = agePattern.matcher(prompt.toLowerCase());

        if (matcher.find()) {
            params.put("minAge", Integer.parseInt(matcher.group(1)));
            params.put("maxAge", Integer.parseInt(matcher.group(3)));
        } else {
            // Valores por defecto
            params.put("minAge", 0);
            params.put("maxAge", 120);
        }

        log.debug("Extracted age parameters: {}", params);
        return params;
    }

    private String extractCityParameter(String prompt) {
        String lowerPrompt = prompt.toLowerCase();
        List<String> cities =
                List.of(
                        "manizales",
                        "bogotá",
                        "cali",
                        "medellín",
                        "barranquilla",
                        "cartagena",
                        "bucaramanga",
                        "pereira",
                        "armenia");

        for (String city : cities) {
            if (lowerPrompt.contains(city)) {
                // Capitalizar primera letra
                return city.substring(0, 1).toUpperCase() + city.substring(1);
            }
        }

        return "Manizales"; // Ciudad por defecto
    }

    private String extractCategoryParameter(String prompt) {
        String lowerPrompt = prompt.toLowerCase();

        if (lowerPrompt.contains("salud")) return "SALUD";
        if (lowerPrompt.contains("educación") || lowerPrompt.contains("educacion"))
            return "EDUCACION";
        if (lowerPrompt.contains("medio ambiente") || lowerPrompt.contains("ambiental"))
            return "MEDIO_AMBIENTE";
        if (lowerPrompt.contains("seguridad")) return "SEGURIDAD";

        return "SALUD"; // Categoría por defecto
    }

    private String extractUrgencyParameter(String prompt) {
        String lowerPrompt = prompt.toLowerCase();

        if (lowerPrompt.contains("urgente")) return "URGENTE";
        if (lowerPrompt.contains("alta")) return "ALTA";
        if (lowerPrompt.contains("media")) return "MEDIA";
        if (lowerPrompt.contains("baja")) return "BAJA";

        return "URGENTE"; // Por defecto los más urgentes
    }

    private Boolean extractGovernmentAttentionParameter(String prompt) {
        String lowerPrompt = prompt.toLowerCase();

        if (lowerPrompt.contains("sin atención") || lowerPrompt.contains("no atendido")) {
            return false;
        }
        if (lowerPrompt.contains("atendido") || lowerPrompt.contains("con atención")) {
            return true;
        }

        return false; // Por defecto los no atendidos
    }

    private Map<String, String> extractDateParameters(String prompt) {
        Map<String, String> params = new HashMap<>();
        LocalDate now = LocalDate.now();

        // Patrón para fechas explícitas
        Pattern datePattern = Pattern.compile("(\\d{4}-\\d{2}-\\d{2})");
        Matcher dateMatcher = datePattern.matcher(prompt);
        List<String> foundDates = new ArrayList<>();

        while (dateMatcher.find()) {
            foundDates.add(dateMatcher.group(1));
        }

        if (foundDates.size() >= 2) {
            params.put("startDate", foundDates.get(0));
            params.put("endDate", foundDates.get(1));
        } else if (foundDates.size() == 1) {
            params.put("startDate", now.minusMonths(1).toString());
            params.put("endDate", foundDates.get(0));
        } else {
            // Detectar períodos relativos
            String lowerPrompt = prompt.toLowerCase();
            if (lowerPrompt.contains("último mes") || lowerPrompt.contains("ultimo mes")) {
                params.put("startDate", now.minusMonths(1).toString());
            } else if (lowerPrompt.contains("última semana")
                    || lowerPrompt.contains("ultima semana")) {
                params.put("startDate", now.minusWeeks(1).toString());
            } else if (lowerPrompt.contains("último año") || lowerPrompt.contains("ultimo ano")) {
                params.put("startDate", now.minusYears(1).toString());
            } else {
                // Por defecto: último mes
                params.put("startDate", now.minusMonths(1).toString());
            }
            params.put("endDate", now.toString());
        }

        log.debug("Extracted date parameters: {}", params);
        return params;
    }

    private String extractZoneParameter(String prompt) {
        String lowerPrompt = prompt.toLowerCase();

        if (lowerPrompt.contains("rural")) return "RURAL";
        if (lowerPrompt.contains("urbana")) return "URBANA";

        return "RURAL"; // Por defecto rural (generalmente menos atendida)
    }

    private String extractAnalysisType(String prompt) {
        String lowerPrompt = prompt.toLowerCase();

        if (lowerPrompt.contains("por categoría") || lowerPrompt.contains("por categoria")) {
            return "por_categoria";
        }
        if (lowerPrompt.contains("por ciudad")) {
            return "por_ciudad";
        }
        if (lowerPrompt.contains("por urgencia")) {
            return "por_urgencia";
        }

        return "general"; // Análisis general
    }

    // ==================== Métodos Auxiliares ====================

    private String getFunctionDisplayName(String functionName) {
        return switch (functionName) {
            case "filterByAge" -> "Filtro por Edad";
            case "filterByCity" -> "Filtro por Ciudad";
            case "filterByCategory" -> "Filtro por Categoría del Problema";
            case "filterByUrgency" -> "Filtro por Nivel de Urgencia";
            case "filterByGovernmentAttention" -> "Filtro por Atención del Gobierno";
            case "filterByReportDate" -> "Filtro por Fecha de Reporte";
            case "filterByZone" -> "Filtro por Zona (Rural/Urbana)";
            case "semanticSearch" -> "Búsqueda Semántica con RAG";
            case "generateReport" -> "Generación de Reporte PDF";
            default -> functionName;
        };
    }

    private void addReportStatistics(StringBuilder contextBuilder, List<CitizenReport> reports) {
        contextBuilder.append("\n📊 ESTADÍSTICAS DE LOS REPORTES:\n");

        // Conteo por categoría
        Map<ProblemCategory, Long> byCategory = new HashMap<>();
        Map<UrgencyLevel, Long> byUrgency = new HashMap<>();
        Map<Zone, Long> byZone = new HashMap<>();
        long withoutAttention = 0;
        long withBias = 0;

        for (CitizenReport report : reports) {
            if (report.getCategoryProblem() != null) {
                byCategory.merge(report.getCategoryProblem(), 1L, Long::sum);
            }
            if (report.getUrgencyLevel() != null) {
                byUrgency.merge(report.getUrgencyLevel(), 1L, Long::sum);
            }
            if (report.getArea() != null) {
                byZone.merge(report.getArea(), 1L, Long::sum);
            }
            if (Boolean.FALSE.equals(report.getGovernmentPreAttention())) {
                withoutAttention++;
            }
            if (Boolean.TRUE.equals(report.getBiasDetected())) {
                withBias++;
            }
        }

        if (!byCategory.isEmpty()) {
            contextBuilder.append("  • Por Categoría: ");
            byCategory.forEach(
                    (cat, count) ->
                            contextBuilder
                                    .append(cat.getDisplayName())
                                    .append(" (")
                                    .append(count)
                                    .append("), "));
            contextBuilder.append("\n");
        }

        if (!byUrgency.isEmpty()) {
            contextBuilder.append("  • Por Urgencia: ");
            byUrgency.forEach(
                    (urg, count) ->
                            contextBuilder
                                    .append(urg.getDisplayName())
                                    .append(" (")
                                    .append(count)
                                    .append("), "));
            contextBuilder.append("\n");
        }

        if (withoutAttention > 0) {
            contextBuilder
                    .append("  • Sin atención del gobierno: ")
                    .append(withoutAttention)
                    .append("\n");
        }

        if (withBias > 0) {
            contextBuilder
                    .append("  • ⚠️ Con sesgos detectados por IA: ")
                    .append(withBias)
                    .append("\n");
        }

        contextBuilder.append("\n");
    }

    private String loadPromptFromClasspath() {
        try (InputStream inputStream =
                getClass().getClassLoader().getResourceAsStream("prompts/ai_prompt_template.txt")) {
            if (inputStream == null) {
                log.warn("Prompt template file not found, using default");
                return "{prompt}"; // Fallback simple
            }
            return StreamUtils.copyToString(inputStream, StandardCharsets.UTF_8);
        } catch (IOException e) {
            log.error("Failed to load prompt template", e);
            return "{prompt}"; // Fallback simple
        }
    }
}
