package com.senasoft.comunidataapi.chat.service.report;

import java.awt.*;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
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

/** Servicio para generar gráficos en reportes usando JFreeChart */
@Service
public class ChatGenerationService {

    private static final Color PRIMARY_COLOR = new Color(41, 128, 185);
    private static final Color SECONDARY_COLOR = new Color(52, 152, 219);
    private static final Color ACCENT_COLOR = new Color(231, 76, 60);
    private static final int CHART_WIDTH = 800;
    private static final int CHART_HEIGHT = 400;

    /** Genera un gráfico de barras para gastos por categoría */
    public byte[] generateCategoryExpenseChart(Map<String, Double> categoryData) {
        try {
            DefaultCategoryDataset dataset = new DefaultCategoryDataset();

            for (Map.Entry<String, Double> entry : categoryData.entrySet()) {
                dataset.addValue(entry.getValue(), "Gastos", entry.getKey());
            }

            JFreeChart chart =
                    ChartFactory.createBarChart(
                            "Gastos por Categoría", "Categorías", "Monto ($)", dataset);

            customizeChart(chart);

            // Personalizar colores de las barras
            CategoryPlot plot = (CategoryPlot) chart.getPlot();
            BarRenderer renderer = (BarRenderer) plot.getRenderer();
            renderer.setSeriesPaint(0, PRIMARY_COLOR);

            return chartToByteArray(chart);
        } catch (Exception e) {
            throw new RuntimeException("Error generando gráfico de barras", e);
        }
    }

    /** Genera un gráfico de línea para tendencias de balance */
    public byte[] generateBalanceTrendChart(List<Map<String, Object>> timeSeriesData) {
        try {
            TimeSeries series = new TimeSeries("Balance");

            for (Map<String, Object> dataPoint : timeSeriesData) {
                LocalDate date = (LocalDate) dataPoint.get("date");
                Double balance = (Double) dataPoint.get("balance");
                series.add(
                        new Day(date.getDayOfMonth(), date.getMonthValue(), date.getYear()),
                        balance);
            }

            TimeSeriesCollection dataset = new TimeSeriesCollection(series);

            JFreeChart chart =
                    ChartFactory.createTimeSeriesChart(
                            "Tendencia del Balance", "Fecha", "Balance ($)", dataset);

            customizeChart(chart);

            return chartToByteArray(chart);
        } catch (Exception e) {
            throw new RuntimeException("Error generando gráfico de tendencias", e);
        }
    }

    /** Genera un gráfico de torta para distribución de presupuesto */
    public byte[] generateBudgetDistributionChart(Map<String, Double> budgetData) {
        try {
            DefaultPieDataset dataset = new DefaultPieDataset();

            for (Map.Entry<String, Double> entry : budgetData.entrySet()) {
                dataset.setValue(entry.getKey(), entry.getValue());
            }

            JFreeChart chart =
                    ChartFactory.createPieChart(
                            "Distribución de Presupuesto", dataset, true, true, false);

            customizeChart(chart);

            // Personalizar colores del gráfico de torta
            PiePlot plot = (PiePlot) chart.getPlot();
            plot.setBackgroundPaint(Color.WHITE);
            plot.setOutlineStroke(new BasicStroke(2.0f));

            return chartToByteArray(chart);
        } catch (Exception e) {
            throw new RuntimeException("Error generando gráfico de torta", e);
        }
    }

    /** Genera un dashboard completo con múltiples gráficos */
    public byte[] generateFinancialDashboard(Map<String, Object> financialData) {
        try {
            // Para el dashboard, generamos múltiples gráficos y los combinamos
            // Por ahora, generamos un gráfico de resumen

            @SuppressWarnings("unchecked")
            Map<String, Double> summaryData = (Map<String, Double>) financialData.get("summary");

            if (summaryData == null) {
                throw new IllegalArgumentException("Datos de resumen no encontrados");
            }

            return generateCategoryExpenseChart(summaryData);
        } catch (Exception e) {
            throw new RuntimeException("Error generando dashboard financiero", e);
        }
    }

    /** Personaliza el estilo general del gráfico */
    private void customizeChart(JFreeChart chart) {
        chart.setBackgroundPaint(Color.WHITE);
        chart.getTitle().setPaint(Color.BLACK);
        chart.getTitle().setFont(new Font("Arial", Font.BOLD, 16));

        // Personalizar el plot
        if (chart.getPlot() instanceof CategoryPlot) {
            CategoryPlot plot = (CategoryPlot) chart.getPlot();
            plot.setBackgroundPaint(Color.WHITE);
            plot.setRangeGridlinePaint(Color.LIGHT_GRAY);
            plot.setDomainGridlinePaint(Color.LIGHT_GRAY);
        }
    }

    /** Convierte un JFreeChart a array de bytes */
    private byte[] chartToByteArray(JFreeChart chart) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ChartUtils.writeChartAsPNG(baos, chart, CHART_WIDTH, CHART_HEIGHT);
        return baos.toByteArray();
    }
}
