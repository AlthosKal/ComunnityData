package com.senasoft.comunidataapi.chat.config;

import com.senasoft.comunidataapi.chat.service.function.list.*;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.function.FunctionToolCallback;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Description;

/**
 * Configuración de funciones de IA para ComuniData.
 *
 * <p>Define las funciones que el modelo GPT-5 puede invocar usando native function calling. Estas
 * funciones permiten al chatbot interactuar con los datos de reportes ciudadanos.
 *
 * <p>Cada función se envuelve en un FunctionToolCallback para que Spring AI 1.0.1+ pueda detectarla
 * correctamente.
 */
@Configuration
public class AiConfiguration {

    @Bean
    @Description("Filtra reportes ciudadanos por rango de edad")
    public ToolCallback filterByAgeFunctionCallback(FilterByAgeFunction function) {
        return FunctionToolCallback.builder("filterByAge", function)
                .description("Filtra reportes ciudadanos por rango de edad. Parámetros: minAge (edad mínima), maxAge (edad máxima)")
                .inputType(FilterByAgeFunction.Request.class)
                .build();
    }

    @Bean
    @Description("Filtra reportes ciudadanos por ciudad")
    public ToolCallback filterByCityFunctionCallback(FilterByCityFunction function) {
        return FunctionToolCallback.builder("filterByCity", function)
                .description("Filtra reportes ciudadanos por ciudad. Parámetro: city (nombre de la ciudad)")
                .inputType(FilterByCityFunction.Request.class)
                .build();
    }

    @Bean
    @Description("Filtra reportes por categoría del problema")
    public ToolCallback filterByCategoryProblemFunctionCallback(
            FilterByCategoryProblemFunction function) {
        return FunctionToolCallback.builder("filterByCategoryProblem", function)
                .description(
                        "Filtra reportes por categoría: Salud, Educación, Medio Ambiente o Seguridad. Parámetro: category")
                .inputType(FilterByCategoryProblemFunction.Request.class)
                .build();
    }

    @Bean
    @Description("Filtra reportes por nivel de urgencia")
    public ToolCallback filterByUrgencyLevelFunctionCallback(
            FilterByUrgencyLevelFunction function) {
        return FunctionToolCallback.builder("filterByUrgencyLevel", function)
                .description(
                        "Filtra reportes por nivel de urgencia: Urgente, Alta, Media o Baja. Parámetro: urgencyLevel")
                .inputType(FilterByUrgencyLevelFunction.Request.class)
                .build();
    }

    @Bean
    @Description("Filtra reportes por atención del gobierno")
    public ToolCallback filterByGovernmentAttentionFunctionCallback(
            FilterByGovernmentAttentionFunction function) {
        return FunctionToolCallback.builder("filterByGovernmentAttention", function)
                .description(
                        "Filtra reportes según si han recibido atención del gobierno (true) o no (false). Parámetro: hasAttention")
                .inputType(FilterByGovernmentAttentionFunction.Request.class)
                .build();
    }

    @Bean
    @Description("Filtra reportes por fecha")
    public ToolCallback filterByReportDateFunctionCallback(FilterByReportDateFunction function) {
        return FunctionToolCallback.builder("filterByReportDate", function)
                .description(
                        "Filtra reportes por rango de fechas. Parámetros: startDate, endDate (formato: yyyy-MM-dd)")
                .inputType(FilterByReportDateFunction.Request.class)
                .build();
    }

    @Bean
    @Description("Filtra reportes por zona")
    public ToolCallback filterByZoneFunctionCallback(FilterByZoneFunction function) {
        return FunctionToolCallback.builder("filterByZone", function)
                .description(
                        "Filtra reportes por zona: Rural (0) o Urbana (1). Parámetro: isUrban (0 o 1)")
                .inputType(FilterByZoneFunction.Request.class)
                .build();
    }

    @Bean
    @Description("Búsqueda semántica en reportes")
    public ToolCallback semanticSearchFunctionCallback(SemanticSearchFunction function) {
        return FunctionToolCallback.builder("semanticSearch", function)
                .description(
                        "Búsqueda semántica en reportes usando IA. Encuentra reportes similares conceptualmente. Parámetros: query (consulta en lenguaje natural), topK (número de resultados, opcional)")
                .inputType(SemanticSearchFunction.Request.class)
                .build();
    }

    @Bean
    @Description("Genera reportes PDF")
    public ToolCallback generateReportFunctionCallback(GenerateReportFunction function) {
        return FunctionToolCallback.builder("generateReport", function)
                .description(
                        "Genera un reporte PDF con análisis de reportes ciudadanos. Parámetros: analysisType, filters (opcional)")
                .inputType(GenerateReportFunction.Request.class)
                .build();
    }
}
