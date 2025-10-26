package com.senasoft.comunidataapi.chat.service.report;

import com.itextpdf.text.*;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;
import com.senasoft.comunidataapi.csv.entity.CitizenReport;
import java.io.File;
import java.io.FileOutputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

/**
 * Servicio de generación de reportes PDF dinámicos para ComuniData.
 *
 * <p>A diferencia de reportes estáticos, este servicio usa GPT-5 para generar análisis dinámicos
 * basados en los datos de reportes ciudadanos.
 */
@Slf4j
@Service
public class CitizenReportGenerationService {

    private final ChatModel chatModel;
    private final CitizenChartGenerationService chartService;

    @Value("${app.reports.storage.path:./reports}")
    private String reportsStoragePath;

    // Constructor manual para especificar @Qualifier en ChatModel
    public CitizenReportGenerationService(
            @Qualifier("openAiChatModel") ChatModel chatModel,
            CitizenChartGenerationService chartService) {
        this.chatModel = chatModel;
        this.chartService = chartService;
    }

    // Colores para ComuniData
    private static final BaseColor PRIMARY_COLOR = new BaseColor(41, 128, 185);
    private static final BaseColor SECONDARY_COLOR = new BaseColor(52, 152, 219);
    private static final BaseColor HEADER_BG = new BaseColor(236, 240, 241);

    /**
     * Genera un reporte PDF dinámico basado en datos de reportes ciudadanos.
     *
     * @param reports Lista de reportes ciudadanos
     * @param analysisType Tipo de análisis (por_categoria, por_ciudad, general, etc.)
     * @param userPrompt Prompt original del usuario para contexto
     * @return ResponseEntity con el archivo PDF
     */
    public ResponseEntity<Resource> generateDynamicPdfReport(
            List<CitizenReport> reports, String analysisType, String userPrompt) {

        try {
            // Crear directorio si no existe
            createDirectoryIfNotExists(reportsStoragePath);

            // Generar nombre de archivo
            String fileName =
                    String.format(
                            "reporte_comunidata_%s_%s.pdf",
                            analysisType, LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")));
            String filePath = reportsStoragePath + File.separator + fileName;

            // Generar análisis dinámico con GPT-5
            String aiAnalysis = generateAiAnalysis(reports, analysisType, userPrompt);

            // Generar métricas básicas
            Map<String, Object> metrics = calculateMetrics(reports);

            // Crear PDF
            Document document = new Document(PageSize.A4, 50, 50, 50, 50);
            PdfWriter.getInstance(document, new FileOutputStream(filePath));
            document.open();

            // Contenido del PDF
            addHeader(document);
            addExecutiveSummary(document, aiAnalysis);
            addMetricsSection(document, metrics);
            addChartsSection(document, reports); // ← NUEVA SECCIÓN DE GRÁFICOS
            addReportsTable(document, reports);
            addFooter(document);

            document.close();

            log.info("PDF report generated successfully: {}", fileName);

            // Retornar archivo
            File pdfFile = new File(filePath);
            Resource resource = new FileSystemResource(pdfFile);

            return ResponseEntity.ok()
                    .header(
                            HttpHeaders.CONTENT_DISPOSITION,
                            "attachment; filename=\"" + fileName + "\"")
                    .contentType(MediaType.APPLICATION_PDF)
                    .contentLength(pdfFile.length())
                    .body(resource);

        } catch (Exception e) {
            log.error("Error generating dynamic PDF report", e);
            throw new RuntimeException("Error generando reporte PDF: " + e.getMessage());
        }
    }

    // ==================== Análisis con IA ====================

    /**
     * Genera análisis dinámico usando GPT-5.
     */
    private String generateAiAnalysis(
            List<CitizenReport> reports, String analysisType, String userPrompt) {

        StringBuilder promptBuilder = new StringBuilder();

        promptBuilder.append("Eres un analista experto en datos de reportes ciudadanos. ");
        promptBuilder.append(
                        "Genera un análisis ejecutivo profesional y conciso basado en los siguientes datos:\n\n");

        // Agregar contexto del usuario
        promptBuilder.append("SOLICITUD DEL USUARIO: ").append(userPrompt).append("\n\n");

        // Agregar resumen de datos
        promptBuilder.append("DATOS DISPONIBLES:\n");
        promptBuilder.append("- Total de reportes: ").append(reports.size()).append("\n");

        // Distribución por categoría
        Map<String, Long> byCategory =
                reports.stream()
                        .filter(r -> r.getCategoriaProblema() != null)
                        .collect(
                                Collectors.groupingBy(
                                        r -> r.getCategoriaProblema().getDisplayName(),
                                        Collectors.counting()));
        promptBuilder.append("- Distribución por categoría:\n");
        byCategory.forEach((cat, count) -> promptBuilder.append("  * ").append(cat).append(": ").append(count).append("\n"));

        // Distribución por ciudad (top 5)
        Map<String, Long> byCity =
                reports.stream()
                        .filter(r -> r.getCiudad() != null)
                        .collect(Collectors.groupingBy(CitizenReport::getCiudad, Collectors.counting()));
        promptBuilder.append("- Ciudades con más reportes (top 5):\n");
        byCity.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(5)
                .forEach(
                        entry ->
                                promptBuilder
                                        .append("  * ")
                                        .append(entry.getKey())
                                        .append(": ")
                                        .append(entry.getValue())
                                        .append("\n"));

        // Nivel de urgencia
        Map<String, Long> byUrgency =
                reports.stream()
                        .filter(r -> r.getNivelUrgencia() != null)
                        .collect(
                                Collectors.groupingBy(
                                        r -> r.getNivelUrgencia().getDisplayName(),
                                        Collectors.counting()));
        promptBuilder.append("- Distribución por urgencia:\n");
        byUrgency.forEach(
                (urgency, count) ->
                        promptBuilder
                                .append("  * ")
                                .append(urgency)
                                .append(": ")
                                .append(count)
                                .append("\n"));

        promptBuilder.append(
                        "\nGENERA UN ANÁLISIS EJECUTIVO que incluya:\n");
        promptBuilder.append("1. Hallazgos principales (2-3 puntos clave)\n");
        promptBuilder.append("2. Áreas de mayor preocupación\n");
        promptBuilder.append("3. Recomendaciones de acción prioritaria\n");
        promptBuilder.append(
                "\nFormato: Texto profesional, máximo 300 palabras, sin bullets ni markdown.\n");

        // Llamar a GPT-5
        Prompt prompt = new Prompt(promptBuilder.toString());
        String analysis = chatModel.call(prompt).getResult().getOutput().getText();

        return analysis;
    }

    // ==================== Cálculo de Métricas ====================

    private Map<String, Object> calculateMetrics(List<CitizenReport> reports) {
        return Map.of(
                "total",
                reports.size(),
                "porCategoria",
                reports.stream()
                        .filter(r -> r.getCategoriaProblema() != null)
                        .collect(
                                Collectors.groupingBy(
                                        r -> r.getCategoriaProblema().getDisplayName(),
                                        Collectors.counting())),
                "porUrgencia",
                reports.stream()
                        .filter(r -> r.getNivelUrgencia() != null)
                        .collect(
                                Collectors.groupingBy(
                                        r -> r.getNivelUrgencia().getDisplayName(),
                                        Collectors.counting())),
                "sinAtencionGobierno",
                reports.stream()
                        .filter(
                                r ->
                                        r.getAtencionPreviaGobierno() != null
                                                && !r.getAtencionPreviaGobierno())
                        .count(),
                "conSesgos",
                reports.stream()
                        .filter(r -> Boolean.TRUE.equals(r.getSesgoDetectado()))
                        .count());
    }

    // ==================== Generación de PDF ====================

    private void addHeader(Document document) throws DocumentException {
        // Título principal
        Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 24, PRIMARY_COLOR);
        Paragraph title = new Paragraph("ComuniData - Análisis de Reportes Ciudadanos", titleFont);
        title.setAlignment(Element.ALIGN_CENTER);
        title.setSpacingAfter(10);
        document.add(title);

        // Subtítulo
        Font subtitleFont = FontFactory.getFont(FontFactory.HELVETICA, 12, BaseColor.GRAY);
        Paragraph subtitle =
                new Paragraph(
                        "Generado el "
                                + LocalDateTime.now()
                                        .format(
                                                DateTimeFormatter.ofPattern(
                                                        "dd/MM/yyyy HH:mm:ss")),
                        subtitleFont);
        subtitle.setAlignment(Element.ALIGN_CENTER);
        subtitle.setSpacingAfter(20);
        document.add(subtitle);

        // Línea separadora
        document.add(new Paragraph(" "));
    }

    private void addExecutiveSummary(Document document, String aiAnalysis)
            throws DocumentException {
        Font sectionFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16, PRIMARY_COLOR);
        Paragraph sectionTitle = new Paragraph("Resumen Ejecutivo", sectionFont);
        sectionTitle.setSpacingBefore(15);
        sectionTitle.setSpacingAfter(10);
        document.add(sectionTitle);

        Font bodyFont = FontFactory.getFont(FontFactory.HELVETICA, 11, BaseColor.DARK_GRAY);
        Paragraph analysis = new Paragraph(aiAnalysis, bodyFont);
        analysis.setAlignment(Element.ALIGN_JUSTIFIED);
        analysis.setSpacingAfter(20);
        document.add(analysis);
    }

    @SuppressWarnings("unchecked")
    private void addMetricsSection(Document document, Map<String, Object> metrics)
            throws DocumentException {
        Font sectionFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16, PRIMARY_COLOR);
        Paragraph sectionTitle = new Paragraph("Métricas Clave", sectionFont);
        sectionTitle.setSpacingBefore(15);
        sectionTitle.setSpacingAfter(10);
        document.add(sectionTitle);

        // Tabla de métricas principales
        PdfPTable table = new PdfPTable(2);
        table.setWidthPercentage(100);
        table.setSpacingAfter(20);

        // Headers
        addCellToTable(table, "Métrica", true);
        addCellToTable(table, "Valor", true);

        // Datos
        addCellToTable(table, "Total de Reportes", false);
        addCellToTable(table, String.valueOf(metrics.get("total")), false);

        addCellToTable(table, "Sin Atención del Gobierno", false);
        addCellToTable(table, String.valueOf(metrics.get("sinAtencionGobierno")), false);

        addCellToTable(table, "Reportes con Sesgos Detectados", false);
        addCellToTable(table, String.valueOf(metrics.get("conSesgos")), false);

        document.add(table);

        // Distribución por categoría
        Map<String, Long> porCategoria = (Map<String, Long>) metrics.get("porCategoria");
        if (!porCategoria.isEmpty()) {
            Font subSectionFont =
                    FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12, SECONDARY_COLOR);
            Paragraph subTitle = new Paragraph("Distribución por Categoría", subSectionFont);
            subTitle.setSpacingBefore(10);
            subTitle.setSpacingAfter(5);
            document.add(subTitle);

            PdfPTable catTable = new PdfPTable(2);
            catTable.setWidthPercentage(100);
            catTable.setSpacingAfter(15);

            addCellToTable(catTable, "Categoría", true);
            addCellToTable(catTable, "Cantidad", true);

            porCategoria.forEach(
                    (cat, count) -> {
                        addCellToTable(catTable, cat, false);
                        addCellToTable(catTable, String.valueOf(count), false);
                    });

            document.add(catTable);
        }
    }

    /**
     * Agrega sección de gráficos visuales al reporte.
     */
    private void addChartsSection(Document document, List<CitizenReport> reports)
            throws DocumentException {
        Font sectionFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16, PRIMARY_COLOR);
        Paragraph sectionTitle = new Paragraph("Análisis Visual", sectionFont);
        sectionTitle.setSpacingBefore(20);
        sectionTitle.setSpacingAfter(15);
        document.add(sectionTitle);

        try {
            // Gráfico 1: Distribución por Categoría
            byte[] categoryChart = chartService.generateCategoryDistributionChart(reports);
            Image categoryImage = Image.getInstance(categoryChart);
            categoryImage.scaleToFit(500, 300);
            categoryImage.setAlignment(Element.ALIGN_CENTER);
            categoryImage.setSpacingAfter(20);
            document.add(categoryImage);

            // Gráfico 2: Nivel de Urgencia (Pie Chart)
            byte[] urgencyChart = chartService.generateUrgencyLevelPieChart(reports);
            Image urgencyImage = Image.getInstance(urgencyChart);
            urgencyImage.scaleToFit(500, 300);
            urgencyImage.setAlignment(Element.ALIGN_CENTER);
            urgencyImage.setSpacingAfter(20);
            document.add(urgencyImage);

            // Salto de página para los siguientes gráficos
            document.newPage();

            // Encabezado de segunda página
            Font pageFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14, PRIMARY_COLOR);
            Paragraph pageTitle = new Paragraph("Análisis Visual (continuación)", pageFont);
            pageTitle.setSpacingAfter(15);
            document.add(pageTitle);

            // Gráfico 3: Top Ciudades
            byte[] citiesChart = chartService.generateTopCitiesChart(reports);
            Image citiesImage = Image.getInstance(citiesChart);
            citiesImage.scaleToFit(500, 300);
            citiesImage.setAlignment(Element.ALIGN_CENTER);
            citiesImage.setSpacingAfter(20);
            document.add(citiesImage);

            // Gráfico 4: Atención del Gobierno
            byte[] govChart = chartService.generateGovernmentAttentionChart(reports);
            Image govImage = Image.getInstance(govChart);
            govImage.scaleToFit(500, 300);
            govImage.setAlignment(Element.ALIGN_CENTER);
            govImage.setSpacingAfter(20);
            document.add(govImage);

            // Gráfico 5: Rural vs Urbana
            byte[] zoneChart = chartService.generateRuralUrbanComparisonChart(reports);
            Image zoneImage = Image.getInstance(zoneChart);
            zoneImage.scaleToFit(500, 300);
            zoneImage.setAlignment(Element.ALIGN_CENTER);
            zoneImage.setSpacingAfter(20);
            document.add(zoneImage);

            // Si hay suficientes datos de fechas, agregar tendencia temporal
            long reportsWithDates =
                    reports.stream().filter(r -> r.getFechaReporte() != null).count();
            if (reportsWithDates > 5) {
                byte[] trendChart = chartService.generateTimeTrendChart(reports);
                Image trendImage = Image.getInstance(trendChart);
                trendImage.scaleToFit(500, 300);
                trendImage.setAlignment(Element.ALIGN_CENTER);
                trendImage.setSpacingAfter(20);
                document.add(trendImage);
            }

            // Nueva página para la tabla de detalles
            document.newPage();

        } catch (Exception e) {
            log.error("Error adding charts to PDF", e);
            // Continuar sin gráficos si hay error
            Font errorFont = FontFactory.getFont(FontFactory.HELVETICA, 10, BaseColor.RED);
            Paragraph errorMsg =
                    new Paragraph(
                            "Los gráficos no pudieron ser generados. Continuando con el reporte...",
                            errorFont);
            errorMsg.setSpacingAfter(20);
            document.add(errorMsg);
        }
    }

    private void addReportsTable(Document document, List<CitizenReport> reports)
            throws DocumentException {
        Font sectionFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16, PRIMARY_COLOR);
        Paragraph sectionTitle = new Paragraph("Detalle de Reportes (Top 20)", sectionFont);
        sectionTitle.setSpacingBefore(15);
        sectionTitle.setSpacingAfter(10);
        document.add(sectionTitle);

        PdfPTable table = new PdfPTable(5);
        table.setWidthPercentage(100);
        table.setWidths(new float[] {2, 2, 4, 2, 1.5f});

        // Headers
        addCellToTable(table, "Ciudad", true);
        addCellToTable(table, "Categoría", true);
        addCellToTable(table, "Comentario", true);
        addCellToTable(table, "Urgencia", true);
        addCellToTable(table, "Zona", true);

        // Datos (top 20)
        reports.stream()
                .limit(20)
                .forEach(
                        report -> {
                            addCellToTable(table, report.getCiudad(), false);
                            addCellToTable(
                                    table,
                                    report.getCategoriaProblema() != null
                                            ? report.getCategoriaProblema().getDisplayName()
                                            : "N/A",
                                    false);
                            String comentario = report.getComentario();
                            if (comentario != null && comentario.length() > 80) {
                                comentario = comentario.substring(0, 77) + "...";
                            }
                            addCellToTable(table, comentario, false);
                            addCellToTable(
                                    table,
                                    report.getNivelUrgencia() != null
                                            ? report.getNivelUrgencia().getDisplayName()
                                            : "N/A",
                                    false);
                            addCellToTable(
                                    table,
                                    report.getZona() != null
                                            ? report.getZona().getDisplayName()
                                            : "N/A",
                                    false);
                        });

        document.add(table);
    }

    private void addFooter(Document document) throws DocumentException {
        document.add(new Paragraph(" "));
        document.add(new Paragraph(" "));

        Font footerFont = FontFactory.getFont(FontFactory.HELVETICA, 9, BaseColor.GRAY);
        Paragraph footer =
                new Paragraph(
                        "Generado con Claude Code - ComuniData v1.0 | Datos anonimizados según Ley 1581/2012",
                        footerFont);
        footer.setAlignment(Element.ALIGN_CENTER);
        document.add(footer);
    }

    private void addCellToTable(PdfPTable table, String content, boolean isHeader) {
        PdfPCell cell = new PdfPCell(new Phrase(content != null ? content : "N/A"));
        if (isHeader) {
            cell.setBackgroundColor(HEADER_BG);
            cell.setHorizontalAlignment(Element.ALIGN_CENTER);
            cell.setPadding(8);
        } else {
            cell.setPadding(5);
        }
        table.addCell(cell);
    }

    private void createDirectoryIfNotExists(String directoryPath) {
        File directory = new File(directoryPath);
        if (!directory.exists()) {
            boolean created = directory.mkdirs();
            if (created) {
                log.info("Created reports directory: {}", directoryPath);
            }
        }
    }
}
