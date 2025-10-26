package com.senasoft.comunidataapi.csv.service.processing;

import com.senasoft.comunidataapi.csv.entity.CitizenReport;
import java.util.List;

/**
 * Servicio para procesamiento de reportes ciudadanos con IBM Granite.
 *
 * <p>Procesa reportes en batches de 50, validando categorías y detectando sesgos usando IBM
 * Granite 3.3-8B-Instruct.
 */
public interface GraniteProcessingService {

    /**
     * Procesa una lista de reportes en batches paralelos.
     *
     * @param reports Lista de reportes normalizados
     * @param batchId ID del batch para tracking
     * @return Lista de reportes procesados y validados por IA
     */
    List<CitizenReport> processReportsInBatches(List<CitizenReport> reports, String batchId);

    /**
     * Procesa un batch individual de hasta 50 reportes.
     *
     * @param reportBatch Batch de reportes (máximo 50)
     * @return Batch procesado con validaciones de IA
     */
    List<CitizenReport> processSingleBatch(List<CitizenReport> reportBatch);

    /**
     * Valida un reporte individual con IBM Granite.
     *
     * @param report Reporte a validar
     * @return Reporte validado con información de sesgo y categoría
     */
    CitizenReport validateReport(CitizenReport report);
}
