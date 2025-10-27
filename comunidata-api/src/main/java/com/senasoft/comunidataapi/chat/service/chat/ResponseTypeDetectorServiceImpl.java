package com.senasoft.comunidataapi.chat.service.chat;

import com.senasoft.comunidataapi.chat.dto.response.ai.BaseDynamicResponseDTO;
import com.senasoft.comunidataapi.chat.service.factory.DynamicResponseFactory;
import com.senasoft.comunidataapi.csv.entity.CitizenReport;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import org.springframework.stereotype.Service;

/**
 * Servicio para detectar el tipo de respuesta y crear el DTO apropiado.
 *
 * <p>Este servicio maneja las respuestas de las funciones de análisis de reportes ciudadanos del
 * sistema ComuniData, transformando los datos en respuestas estructuradas para el frontend.
 *
 * <p>Funciones soportadas: - filterByAge: Filtrado por rango de edad del ciudadano - filterByCity:
 * Filtrado por ciudad - filterByCategory: Filtrado por categoría de problema (salud, educación,
 * ambiente, seguridad) - filterByUrgency: Filtrado por nivel de urgencia -
 * filterByGovernmentAttention: Filtrado por atención del gobierno - filterByReportDate: Filtrado
 * por rango de fechas - filterByZone: Filtrado por zona geográfica - semanticSearch: Búsqueda
 * semántica usando RAG con embeddings - generateReport: Generación de reportes con análisis de IA
 */
@Service
public class ResponseTypeDetectorServiceImpl implements ResponseTypeDetectorService {

    private final DynamicResponseFactory responseFactory;
    private final Map<String, BiFunction<String, Object, BaseDynamicResponseDTO>> responseHandlers;

    public ResponseTypeDetectorServiceImpl(DynamicResponseFactory responseFactory) {
        this.responseFactory = responseFactory;
        this.responseHandlers = buildHandlers();
    }

    @Override
    public BaseDynamicResponseDTO detectAndCreateResponse(
            String prompt, String functionName, Object data) {
        return responseHandlers
                .getOrDefault(
                        functionName,
                        (p, d) ->
                                responseFactory.createSimpleTextResponse(
                                        p, "Respuesta generada por IA para: " + p))
                .apply(prompt, data);
    }

    /**
     * Construye el mapa de handlers para cada función de ComuniData.
     *
     * <p>NOTA: Para las funciones de ComuniData, los datos son procesados por el ChatService que
     * agrega el contexto enriquecido al prompt antes de enviarlo a GPT-5. Por lo tanto, la mayoría
     * de las funciones retornan respuestas de texto simple que permiten a GPT-5 generar la
     * respuesta final con análisis inteligente.
     *
     * <p>El flujo es: 1. Function detectada y ejecutada (ej: filterByAge) 2. Datos obtenidos
     * (List<CitizenReport>) 3. FunctionService crea prompt contextual con estadísticas 4.
     * ChatService envía prompt enriquecido a GPT-5 5. GPT-5 genera análisis inteligente final
     */
    private Map<String, BiFunction<String, Object, BaseDynamicResponseDTO>> buildHandlers() {
        Map<String, BiFunction<String, Object, BaseDynamicResponseDTO>> map = new HashMap<>();

        // Funciones de filtrado de reportes ciudadanos
        map.put("filterByAge", citizenReportListHandler());
        map.put("filterByCity", citizenReportListHandler());
        map.put("filterByCategory", citizenReportListHandler());
        map.put("filterByUrgency", citizenReportListHandler());
        map.put("filterByGovernmentAttention", citizenReportListHandler());
        map.put("filterByReportDate", citizenReportListHandler());
        map.put("filterByZone", citizenReportListHandler());

        // Búsqueda semántica con RAG (retorna List<String> de IDs)
        map.put("semanticSearch", semanticSearchHandler());

        // Generación de reportes PDF (retorna String con URL de descarga)
        map.put("generateReport", reportGenerationHandler());

        return map;
    }

    /**
     * Handler para funciones que retornan List<CitizenReport>.
     *
     * <p>Este handler NO genera la respuesta final. Simplemente valida que hay datos y retorna una
     * respuesta simple. El ChatService tomará estos datos, generará estadísticas y contexto, y
     * permitirá que GPT-5 cree el análisis final inteligente.
     */
    private BiFunction<String, Object, BaseDynamicResponseDTO> citizenReportListHandler() {
        return (prompt, data) -> {
            List<CitizenReport> reports = safeCastList(data, CitizenReport.class);

            if (reports.isEmpty()) {
                return responseFactory.createSimpleTextResponse(
                        prompt,
                        "No se encontraron reportes ciudadanos que cumplan con los criterios especificados.");
            }

            // Retornar respuesta simple que será enriquecida por ChatService + GPT-5
            String message =
                    String.format(
                            "Se encontraron %d reportes ciudadanos. El sistema está analizando los datos para proporcionar un análisis detallado.",
                            reports.size());

            return responseFactory.createSimpleTextResponse(prompt, message);
        };
    }

    /**
     * Handler para búsqueda semántica que retorna List<String> de IDs.
     *
     * <p>La búsqueda semántica usa RAG (Retrieval-Augmented Generation) con embeddings de OpenAI y
     * MongoDB Atlas Vector Store para encontrar reportes similares semánticamente.
     */
    private BiFunction<String, Object, BaseDynamicResponseDTO> semanticSearchHandler() {
        return (prompt, data) -> {
            if (data instanceof List<?> rawList) {
                // La búsqueda semántica retorna List<String> de IDs
                if (!rawList.isEmpty() && rawList.get(0) instanceof String) {
                    @SuppressWarnings("unchecked")
                    List<String> reportIds = (List<String>) rawList;

                    String message =
                            String.format(
                                    "Se encontraron %d reportes semánticamente similares a tu búsqueda. El sistema está recuperando los detalles para análisis.",
                                    reportIds.size());

                    return responseFactory.createSimpleTextResponse(prompt, message);
                }
            }

            return responseFactory.createSimpleTextResponse(
                    prompt, "No se encontraron reportes similares a tu búsqueda.");
        };
    }

    /**
     * Handler para generación de reportes PDF.
     *
     * <p>La función generateReport utiliza el CitizenReportGenerationService para crear un PDF con
     * análisis de IA, gráficos y métricas detalladas. Retorna una URL de descarga.
     */
    private BiFunction<String, Object, BaseDynamicResponseDTO> reportGenerationHandler() {
        return (prompt, data) -> {
            if (data instanceof String downloadUrl) {
                return responseFactory.createReportDownloadResponse(
                        "Reporte de Análisis Ciudadano",
                        "Reporte generado con análisis de IA sobre reportes ciudadanos de ComuniData",
                        downloadUrl,
                        "PDF");
            }

            return responseFactory.createSimpleTextResponse(
                    prompt,
                    "El reporte está siendo generado. Por favor, intenta de nuevo en unos momentos.");
        };
    }

    /**
     * Realiza un cast seguro de Object a List<T>.
     *
     * @param data Datos a convertir
     * @param clazz Clase del tipo T
     * @return Lista tipada o lista vacía si no es posible el cast
     */
    @SuppressWarnings("unchecked")
    private <T> List<T> safeCastList(Object data, Class<T> clazz) {
        if (data instanceof List<?> rawList) {
            if (!rawList.isEmpty() && clazz.isInstance(rawList.get(0))) {
                return (List<T>) rawList;
            }
        }
        return Collections.emptyList();
    }
}
