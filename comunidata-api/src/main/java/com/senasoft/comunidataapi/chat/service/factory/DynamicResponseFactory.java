package com.senasoft.comunidataapi.chat.service.factory;

import com.senasoft.comunidataapi.chat.dto.response.CharDataDTO;
import com.senasoft.comunidataapi.chat.dto.response.ai.*;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * Factory para crear respuestas dinámicas basadas en diferentes tipos de datos del sistema
 * ComuniData.
 *
 * <p>Esta clase se encarga de transformar datos en respuestas estructuradas que pueden ser
 * consumidas por el frontend, incluyendo gráficos, reportes y análisis de datos ciudadanos.
 */
@Component
public class DynamicResponseFactory {

    /**
     * Crea una respuesta de texto simple.
     *
     * @param prompt Pregunta o prompt del usuario
     * @param content Contenido de la respuesta
     * @return Respuesta DTO con el texto
     */
    public BaseDynamicResponseDTO createSimpleTextResponse(String prompt, String content) {
        return new SimpleTextResponseDTO(
                "Respuesta general", "Análisis basado en la consulta del usuario", content);
    }

    /**
     * Crea una respuesta con datos de gráfico.
     *
     * @param title Título del gráfico
     * @param analysis Análisis o descripción del gráfico
     * @param chartType Tipo de gráfico (pie, bar, line, etc.)
     * @param chartData Datos para el gráfico
     * @param xAxisLabel Etiqueta del eje X
     * @param yAxisLabel Etiqueta del eje Y
     * @return Respuesta DTO con datos del gráfico
     */
    public BaseDynamicResponseDTO createChartDataResponse(
            String title,
            String analysis,
            String chartType,
            List<CharDataDTO> chartData,
            String xAxisLabel,
            String yAxisLabel) {
        return new ChartDataResponseDTO(
                title, analysis, chartType, chartData, xAxisLabel, yAxisLabel);
    }

    /**
     * Crea una respuesta para descarga de reporte.
     *
     * @param title Título del reporte
     * @param description Descripción del reporte
     * @param downloadUrl URL de descarga del reporte
     * @param fileFormat Formato del archivo (PDF, CSV, etc.)
     * @return Respuesta DTO con información del reporte
     */
    public BaseDynamicResponseDTO createReportDownloadResponse(
            String title, String description, String downloadUrl, String fileFormat) {
        return new ReportDownloadResponseDTO(
                title, // summary
                description, // analysis
                downloadUrl, // reportId (usando URL como ID)
                "reporte_ciudadano." + fileFormat.toLowerCase(), // fileName
                fileFormat, // reportType
                "CITIZEN_ANALYSIS", // reportCategory
                0L, // fileSizeBytes (desconocido por ahora)
                null // expirationDate (sin expiración por defecto)
                );
    }
}
