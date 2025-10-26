package com.senasoft.comunidataapi.csv.service.normalization;

import com.senasoft.comunidataapi.csv.dto.request.RawCsvRowDTO;
import com.senasoft.comunidataapi.csv.entity.CitizenReport;
import java.io.InputStream;
import java.util.List;

/**
 * Servicio para normalización de datos CSV de reportes ciudadanos.
 */
public interface CsvNormalizationService {

    /**
     * Parsea y normaliza un archivo CSV completo.
     *
     * @param inputStream Stream del archivo CSV
     * @param batchId ID del batch para tracking
     * @return Lista de reportes ciudadanos normalizados
     */
    List<CitizenReport> parseAndNormalizeCsv(InputStream inputStream, String batchId);

    /**
     * Normaliza una fila individual del CSV.
     *
     * @param rawRow Fila cruda del CSV
     * @param batchId ID del batch
     * @param batchIndex Índice en el batch
     * @return Reporte ciudadano normalizado
     */
    CitizenReport normalizeRow(RawCsvRowDTO rawRow, String batchId, Integer batchIndex);
}
