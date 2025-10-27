package com.senasoft.comunidataapi.csv.service.normalization;

import com.senasoft.comunidataapi.csv.dto.request.RawCsvRowDTO;
import com.senasoft.comunidataapi.csv.entity.CitizenReport;
import com.senasoft.comunidataapi.csv.enums.ProblemCategory;
import com.senasoft.comunidataapi.csv.enums.ProcessingStatus;
import com.senasoft.comunidataapi.csv.enums.UrgencyLevel;
import com.senasoft.comunidataapi.csv.enums.Zone;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.Normalizer;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Implementación del servicio de normalización de CSV.
 *
 * <p>Normaliza datos según las reglas: - Edad: rango 0-120, convierte strings a int - Ciudad:
 * capitaliza y normaliza acentos - Fecha: convierte a ISO format (yyyy-MM-dd) - Comentario: trim,
 * limpia caracteres especiales excesivos - Booleanos: convierte "0"|"1" y "Sí"|"No" a boolean
 */
@Slf4j
@Service
public class CsvNormalizationServiceImpl implements CsvNormalizationService {

    private static final DateTimeFormatter[] DATE_FORMATTERS = {
        DateTimeFormatter.ofPattern("yyyy-MM-dd"),
        DateTimeFormatter.ofPattern("dd/MM/yyyy"),
        DateTimeFormatter.ofPattern("MM/dd/yyyy"),
        DateTimeFormatter.ofPattern("yyyy/MM/dd")
    };

    @Override
    public List<CitizenReport> parseAndNormalizeCsv(InputStream inputStream, String batchId) {
        List<CitizenReport> reports = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
            String headerLine = reader.readLine(); // Skip header
            if (headerLine == null) {
                log.warn("CSV file is empty for batch {}", batchId);
                return reports;
            }

            String line;
            int batchIndex = 0;
            while ((line = reader.readLine()) != null) {
                try {
                    RawCsvRowDTO rawRow = parseCsvLine(line);
                    CitizenReport normalizedReport = normalizeRow(rawRow, batchId, batchIndex++);
                    reports.add(normalizedReport);
                } catch (Exception e) {
                    log.error("Error parsing CSV line at index {}: {}", batchIndex, line, e);
                }
            }
        } catch (IOException e) {
            log.error("Error reading CSV file for batch {}", batchId, e);
            throw new RuntimeException("Failed to parse CSV file", e);
        }

        log.info("Parsed and normalized {} reports from CSV for batch {}", reports.size(), batchId);
        return reports;
    }

    @Override
    public CitizenReport normalizeRow(RawCsvRowDTO rawRow, String batchId, Integer batchIndex) {
        return CitizenReport.builder()
                .age(normalizeEdad(rawRow.getAge()))
                .city(normalizeCiudad(rawRow.getCity()))
                .comment(normalizeComentario(rawRow.getComment()))
                .originalComment(rawRow.getComment()) // Guardar original para auditoría
                .categoryProblem(ProblemCategory.fromString(rawRow.getCategoryProblem()))
                .originalCategory(rawRow.getCategoryProblem())
                .urgencyLevel(UrgencyLevel.fromString(rawRow.getUrgencyLevel()))
                .reportDate(normalizeFecha(rawRow.getDateReport()))
                .governmentPreAttention(normalizeBoolean(rawRow.getGovernmentPreAttention()))
                .area(normalizeZona(rawRow.getRuralArea()))
                .processingStatus(ProcessingStatus.PENDIENTE)
                .importDate(LocalDateTime.now())
                .batchId(batchId)
                .batchIndex(batchIndex)
                .biasDetected(false) // Se actualizará después del procesamiento IA
                .build();
    }

    // ==================== Métodos de Normalización ====================

    /**
     * Normaliza edad: - Valida rango 0-120 - Convierte string a int - Retorna null si está vacío o
     * inválido
     */
    private Integer normalizeEdad(String edad) {
        if (edad == null || edad.trim().isEmpty()) {
            return null;
        }

        try {
            int edadInt = Integer.parseInt(edad.trim());
            if (edadInt < 0 || edadInt > 120) {
                log.warn("Edad fuera de rango: {}", edadInt);
                return null;
            }
            return edadInt;
        } catch (NumberFormatException e) {
            log.warn("Error parsing edad: {}", edad);
            return null;
        }
    }

    /**
     * Normaliza ciudad: - Capitaliza correctamente: "manizales" → "Manizales" - Normaliza acentos
     * si es necesario - Trim espacios
     */
    private String normalizeCiudad(String ciudad) {
        if (ciudad == null || ciudad.trim().isEmpty()) {
            return null;
        }

        String normalized = ciudad.trim();

        // Capitalizar cada palabra
        String[] words = normalized.split("\\s+");
        StringBuilder result = new StringBuilder();
        for (String word : words) {
            if (!word.isEmpty()) {
                result.append(Character.toUpperCase(word.charAt(0)))
                        .append(word.substring(1).toLowerCase())
                        .append(" ");
            }
        }

        return result.toString().trim();
    }

    /**
     * Normaliza comentario: - Trim espacios - Elimina caracteres especiales excesivos (###, @@@) -
     * Normaliza múltiples espacios a uno solo - Mantiene puntuación básica (. , ! ?)
     */
    private String normalizeComentario(String comentario) {
        if (comentario == null || comentario.trim().isEmpty()) {
            return null;
        }

        String normalized = comentario.trim();

        // Eliminar caracteres especiales repetitivos (###, @@@, etc.)
        normalized = normalized.replaceAll("([#@*_=+\\-]{2,})", "");

        // Normalizar múltiples espacios a uno solo
        normalized = normalized.replaceAll("\\s+", " ");

        // Normalizar múltiples puntos/exclamaciones/interrogaciones
        normalized = normalized.replaceAll("\\.{2,}", ".");
        normalized = normalized.replaceAll("!{2,}", "!");
        normalized = normalized.replaceAll("\\?{2,}", "?");

        return normalized.trim();
    }

    /**
     * Normaliza fecha: - Convierte a formato ISO: "11/08/2023" → "2023-08-11" - Valida que sea
     * fecha válida - Soporta múltiples formatos de entrada
     */
    private LocalDate normalizeFecha(String fecha) {
        if (fecha == null || fecha.trim().isEmpty()) {
            return null;
        }

        String trimmedFecha = fecha.trim();

        // Intentar parsear con diferentes formatos
        for (DateTimeFormatter formatter : DATE_FORMATTERS) {
            try {
                return LocalDate.parse(trimmedFecha, formatter);
            } catch (DateTimeParseException e) {
                // Intentar siguiente formato
            }
        }

        log.warn("Could not parse fecha: {}", fecha);
        return null;
    }

    /**
     * Normaliza booleanos: - "0" | "1" → false | true - "Sí" | "No" → true | false - "true" |
     * "false" → boolean
     */
    private Boolean normalizeBoolean(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }

        String normalized = value.trim().toLowerCase();

        return switch (normalized) {
            case "1", "true", "sí", "si", "yes", "y" -> true;
            case "0", "false", "no", "n" -> false;
            default -> null;
        };
    }

    /** Normaliza zona: - Convierte "Zona rural" a enum Zone - "0" | "1" → RURAL | URBANA */
    private Zone normalizeZona(String zonaRural) {
        if (zonaRural == null || zonaRural.trim().isEmpty()) {
            return null;
        }

        // Primero intentar parsear como boolean
        Boolean isRural = normalizeBoolean(zonaRural);
        if (isRural != null) {
            return Zone.fromBoolean(isRural);
        }

        // Si no, intentar parsear como string
        return Zone.fromString(zonaRural);
    }

    // ==================== Helper Methods ====================

    /**
     * Parsea una línea del CSV en un objeto RawCsvRowDTO.
     *
     * <p>Estructura esperada: ID, Nombre, Edad, Género, Ciudad, Comentario, Categoría del problema,
     * Nivel de urgencia, Fecha del reporte, Acceso a internet, Atención previa del gobierno, Zona
     * rural
     */
    private RawCsvRowDTO parseCsvLine(String line) {
        // Split considerando que puede haber comas dentro de campos entre comillas
        String[] fields = line.split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)", -1);

        // Limpiar comillas de los campos
        for (int i = 0; i < fields.length; i++) {
            fields[i] = fields[i].trim().replaceAll("^\"|\"$", "");
        }

        return RawCsvRowDTO.builder()
                .id(getField(fields, 0))
                .name(getField(fields, 1))
                .age(getField(fields, 2))
                .gender(getField(fields, 3))
                .city(getField(fields, 4))
                .comment(getField(fields, 5))
                .categoryProblem(getField(fields, 6))
                .urgencyLevel(getField(fields, 7))
                .dateReport(getField(fields, 8))
                .internetAccess(getField(fields, 9))
                .governmentPreAttention(getField(fields, 10))
                .ruralArea(getField(fields, 11))
                .build();
    }

    private String getField(String[] fields, int index) {
        return index < fields.length ? fields[index] : null;
    }

    /** Normaliza texto removiendo acentos (útil para búsquedas). */
    private String removeAccents(String text) {
        if (text == null) return null;
        return Normalizer.normalize(text, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
    }
}
