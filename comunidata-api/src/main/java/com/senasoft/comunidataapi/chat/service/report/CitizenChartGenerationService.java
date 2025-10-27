package com.senasoft.comunidataapi.chat.service.report;

import com.senasoft.comunidataapi.csv.entity.CitizenReport;
import java.awt.*;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartUtils;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.plot.PiePlot;
import org.jfree.chart.renderer.category.BarRenderer;
import org.jfree.data.category.DefaultCategoryDataset;
import org.jfree.data.general.DefaultPieDataset;
import org.jfree.data.time.Day;
import org.jfree.data.time.TimeSeries;
import org.jfree.data.time.TimeSeriesCollection;
import org.springframework.stereotype.Service;

/**
 * Servicio para generar gráficos de reportes ciudadanos usando JFreeChart.
 *
 * <p>Genera gráficos para incluir en reportes PDF de análisis de ComuniData.
 */
@Slf4j
@Service
public class CitizenChartGenerationService {

    // Colores de ComuniData
    private static final Color PRIMARY_COLOR = new Color(41, 128, 185);
    private static final Color HEALTH_COLOR = new Color(231, 76, 60); // Rojo - Salud
    private static final Color EDUCATION_COLOR = new Color(52, 152, 219); // Azul - Educación
    private static final Color ENVIRONMENT_COLOR =
            new Color(46, 204, 113); // Verde - Medio Ambiente
    private static final Color SECURITY_COLOR = new Color(241, 196, 15); // Amarillo - Seguridad

    private static final int CHART_WIDTH = 800;
    private static final int CHART_HEIGHT = 400;

    /** Genera gráfico de barras con distribución de reportes por categoría. */
    public byte[] generateCategoryDistributionChart(List<CitizenReport> reports) {
        try {
            Map<String, Long> categoryCount =
                    reports.stream()
                            .filter(r -> r.getCategoryProblem() != null)
                            .collect(
                                    Collectors.groupingBy(
                                            r -> r.getCategoryProblem().getDisplayName(),
                                            Collectors.counting()));

            DefaultCategoryDataset dataset = new DefaultCategoryDataset();

            for (Map.Entry<String, Long> entry : categoryCount.entrySet()) {
                dataset.addValue(entry.getValue(), "Reportes", entry.getKey());
            }

            JFreeChart chart =
                    ChartFactory.createBarChart(
                            "Distribución de Reportes por Categoría",
                            "Categorías",
                            "Cantidad de Reportes",
                            dataset);

            customizeChart(chart);

            // Personalizar colores por categoría
            CategoryPlot plot = (CategoryPlot) chart.getPlot();
            BarRenderer renderer = (BarRenderer) plot.getRenderer();
            renderer.setSeriesPaint(0, PRIMARY_COLOR);

            return chartToByteArray(chart);
        } catch (Exception e) {
            log.error("Error generating category distribution chart", e);
            throw new RuntimeException("Error generando gráfico de categorías", e);
        }
    }

    /** Genera gráfico de torta con distribución por nivel de urgencia. */
    public byte[] generateUrgencyLevelPieChart(List<CitizenReport> reports) {
        try {
            Map<String, Long> urgencyCount =
                    reports.stream()
                            .filter(r -> r.getUrgencyLevel() != null)
                            .collect(
                                    Collectors.groupingBy(
                                            r -> r.getUrgencyLevel().getDisplayName(),
                                            Collectors.counting()));

            DefaultPieDataset dataset = new DefaultPieDataset();

            for (Map.Entry<String, Long> entry : urgencyCount.entrySet()) {
                dataset.setValue(entry.getKey(), entry.getValue());
            }

            JFreeChart chart =
                    ChartFactory.createPieChart(
                            "Distribución por Nivel de Urgencia", dataset, true, true, false);

            customizeChart(chart);

            // Personalizar gráfico de torta
            PiePlot plot = (PiePlot) chart.getPlot();
            plot.setBackgroundPaint(Color.WHITE);
            plot.setOutlineStroke(new BasicStroke(2.0f));

            // Colores específicos por urgencia
            plot.setSectionPaint("Urgente", new Color(231, 76, 60)); // Rojo
            plot.setSectionPaint("Alta", new Color(230, 126, 34)); // Naranja
            plot.setSectionPaint("Media", new Color(241, 196, 15)); // Amarillo
            plot.setSectionPaint("Baja", new Color(46, 204, 113)); // Verde

            return chartToByteArray(chart);
        } catch (Exception e) {
            log.error("Error generating urgency level pie chart", e);
            throw new RuntimeException("Error generando gráfico de urgencia", e);
        }
    }

    /** Genera gráfico de barras horizontales con top 10 ciudades con más reportes. */
    public byte[] generateTopCitiesChart(List<CitizenReport> reports) {
        try {
            Map<String, Long> cityCount =
                    reports.stream()
                            .filter(r -> r.getCity() != null)
                            .collect(
                                    Collectors.groupingBy(
                                            CitizenReport::getCity, Collectors.counting()));

            // Top 10 ciudades
            Map<String, Long> topCities =
                    cityCount.entrySet().stream()
                            .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                            .limit(10)
                            .collect(
                                    Collectors.toMap(
                                            Map.Entry::getKey,
                                            Map.Entry::getValue,
                                            (e1, e2) -> e1,
                                            java.util.LinkedHashMap::new));

            DefaultCategoryDataset dataset = new DefaultCategoryDataset();

            for (Map.Entry<String, Long> entry : topCities.entrySet()) {
                dataset.addValue(entry.getValue(), "Reportes", entry.getKey());
            }

            JFreeChart chart =
                    ChartFactory.createBarChart(
                            "Top 10 Ciudades con Más Reportes",
                            "Cantidad de Reportes",
                            "Ciudades",
                            dataset);

            customizeChart(chart);

            CategoryPlot plot = (CategoryPlot) chart.getPlot();
            BarRenderer renderer = (BarRenderer) plot.getRenderer();
            renderer.setSeriesPaint(0, EDUCATION_COLOR);

            return chartToByteArray(chart);
        } catch (Exception e) {
            log.error("Error generating top cities chart", e);
            throw new RuntimeException("Error generando gráfico de ciudades", e);
        }
    }

    /** Genera gráfico de línea con tendencia temporal de reportes. */
    public byte[] generateTimeTrendChart(List<CitizenReport> reports) {
        try {
            Map<LocalDate, Long> dateCount =
                    reports.stream()
                            .filter(r -> r.getReportDate() != null)
                            .collect(
                                    Collectors.groupingBy(
                                            CitizenReport::getReportDate, Collectors.counting()));

            TimeSeries series = new TimeSeries("Reportes Ciudadanos");

            for (Map.Entry<LocalDate, Long> entry : dateCount.entrySet()) {
                LocalDate date = entry.getKey();
                series.add(
                        new Day(date.getDayOfMonth(), date.getMonthValue(), date.getYear()),
                        entry.getValue());
            }

            TimeSeriesCollection dataset = new TimeSeriesCollection(series);

            JFreeChart chart =
                    ChartFactory.createTimeSeriesChart(
                            "Tendencia Temporal de Reportes",
                            "Fecha",
                            "Cantidad de Reportes",
                            dataset);

            customizeChart(chart);

            return chartToByteArray(chart);
        } catch (Exception e) {
            log.error("Error generating time trend chart", e);
            throw new RuntimeException("Error generando gráfico de tendencias", e);
        }
    }

    /** Genera gráfico de barras comparando zona rural vs urbana. */
    public byte[] generateRuralUrbanComparisonChart(List<CitizenReport> reports) {
        try {
            Map<String, Long> zoneCount =
                    reports.stream()
                            .filter(r -> r.getArea() != null)
                            .collect(
                                    Collectors.groupingBy(
                                            r -> r.getArea().getDisplayName(),
                                            Collectors.counting()));

            DefaultCategoryDataset dataset = new DefaultCategoryDataset();

            for (Map.Entry<String, Long> entry : zoneCount.entrySet()) {
                dataset.addValue(entry.getValue(), "Reportes", entry.getKey());
            }

            JFreeChart chart =
                    ChartFactory.createBarChart(
                            "Comparación Rural vs Urbana", "Zona", "Cantidad de Reportes", dataset);

            customizeChart(chart);

            CategoryPlot plot = (CategoryPlot) chart.getPlot();
            BarRenderer renderer = (BarRenderer) plot.getRenderer();
            renderer.setSeriesPaint(0, ENVIRONMENT_COLOR);

            return chartToByteArray(chart);
        } catch (Exception e) {
            log.error("Error generating rural/urban comparison chart", e);
            throw new RuntimeException("Error generando gráfico rural/urbana", e);
        }
    }

    /** Genera gráfico de torta con atención previa del gobierno. */
    public byte[] generateGovernmentAttentionChart(List<CitizenReport> reports) {
        try {
            long withAttention =
                    reports.stream()
                            .filter(
                                    r ->
                                            r.getGovernmentPreAttention() != null
                                                    && r.getGovernmentPreAttention())
                            .count();

            long withoutAttention =
                    reports.stream()
                            .filter(
                                    r ->
                                            r.getGovernmentPreAttention() != null
                                                    && !r.getGovernmentPreAttention())
                            .count();

            DefaultPieDataset dataset = new DefaultPieDataset();
            dataset.setValue("Con Atención", withAttention);
            dataset.setValue("Sin Atención", withoutAttention);

            JFreeChart chart =
                    ChartFactory.createPieChart(
                            "Atención Previa del Gobierno", dataset, true, true, false);

            customizeChart(chart);

            PiePlot plot = (PiePlot) chart.getPlot();
            plot.setBackgroundPaint(Color.WHITE);
            plot.setSectionPaint("Con Atención", new Color(46, 204, 113)); // Verde
            plot.setSectionPaint("Sin Atención", new Color(231, 76, 60)); // Rojo

            return chartToByteArray(chart);
        } catch (Exception e) {
            log.error("Error generating government attention chart", e);
            throw new RuntimeException("Error generando gráfico de atención gubernamental", e);
        }
    }

    // ==================== Helper Methods ====================

    /** Personaliza el estilo general del gráfico. */
    private void customizeChart(JFreeChart chart) {
        chart.setBackgroundPaint(Color.WHITE);
        chart.getTitle().setPaint(Color.BLACK);
        chart.getTitle().setFont(new Font("Arial", Font.BOLD, 16));

        if (chart.getPlot() instanceof CategoryPlot) {
            CategoryPlot plot = (CategoryPlot) chart.getPlot();
            plot.setBackgroundPaint(Color.WHITE);
            plot.setRangeGridlinePaint(Color.LIGHT_GRAY);
            plot.setDomainGridlinePaint(Color.LIGHT_GRAY);
        }
    }

    /** Convierte un JFreeChart a array de bytes. */
    private byte[] chartToByteArray(JFreeChart chart) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ChartUtils.writeChartAsPNG(baos, chart, CHART_WIDTH, CHART_HEIGHT);
        return baos.toByteArray();
    }
}
