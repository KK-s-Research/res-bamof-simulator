package org.bamof.io;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.ToDoubleFunction;

import javax.imageio.ImageIO;

import org.bamof.model.Algorithm;
import org.bamof.model.RunResult;
import org.bamof.model.TimeSeriesPoint;
import org.bamof.stats.SummaryRow;

public final class FigureWriter {
    private static final int WIDTH = 1100;
    private static final int HEIGHT = 720;
    private static final int LEFT = 125;
    private static final int RIGHT = 285;
    private static final int TOP = 55;
    private static final int BOTTOM = 125;
    private static final DecimalFormat NUMBER = new DecimalFormat("0.###");

    private static final Map<Algorithm, Color> COLORS = Map.of(
            Algorithm.EDF, new Color(31, 119, 180),
            Algorithm.HPA, new Color(255, 127, 14),
            Algorithm.AMRP, new Color(44, 160, 44),
            Algorithm.CDASCALER, new Color(214, 39, 40),
            Algorithm.CEMA, new Color(148, 103, 189),
            Algorithm.CEDULE, new Color(140, 86, 75),
            Algorithm.BADP, new Color(23, 190, 207),
            Algorithm.BADP_PLUS, new Color(0, 90, 181));

    private FigureWriter() {
    }

    public static List<Path> writeAll(Path figuresDir, List<SummaryRow> summaries, List<RunResult> results)
            throws IOException {
        Files.createDirectories(figuresDir);
        List<Path> written = new ArrayList<>();

        written.add(writeArchitectureFigure(figuresDir.resolve("fig_bamof_architecture.png")));
        written.add(writeWorkflowFigure(figuresDir.resolve("fig_operational_workflow.png")));
        written.add(writeSummaryFigure(figuresDir.resolve("fig_cost_comparison.png"), summaries,
                "cost_comparison",
                "Number of Tasks", "Total Infrastructure Cost ($)", SummaryRow::meanCost, false));
        written.add(writeSummaryFigure(figuresDir.resolve("fig_deadline_satisfaction.png"), summaries,
                "deadline_satisfaction",
                "Workload Intensity", "DSR (%)", SummaryRow::meanDsr, true));
        written.add(writeSummaryFigure(figuresDir.resolve("fig_credit_utilization.png"), summaries,
                "credit_utilization",
                "Workload Intensity", "CUE (%)", SummaryRow::meanCue, true));
        written.add(writeActiveVmFigure(figuresDir.resolve("fig_active_vm_count.png"), results));
        written.add(writeSummaryFigure(figuresDir.resolve("fig_burst_sensitivity_cost.png"), summaries,
                "burst_sensitivity",
                "Burst Factor", "Total Infrastructure Cost ($)", SummaryRow::meanCost, false));
        written.add(writeSummaryFigure(figuresDir.resolve("fig_burst_sensitivity_dsr.png"), summaries,
                "burst_sensitivity",
                "Burst Factor", "DSR (%)", SummaryRow::meanDsr, true));
        written.add(writeSummaryFigure(figuresDir.resolve("fig_credit_regen_cost.png"), summaries,
                "credit_regeneration_sensitivity",
                "Credit Regeneration Multiplier", "Total Infrastructure Cost ($)", SummaryRow::meanCost, false));
        written.add(writeSummaryFigure(figuresDir.resolve("fig_credit_regen_cue.png"), summaries,
                "credit_regeneration_sensitivity",
                "Credit Regeneration Multiplier", "CUE (%)", SummaryRow::meanCue, true));
        written.add(writeSummaryFigure(figuresDir.resolve("fig_runtime_scalability.png"), summaries,
                "runtime_scalability",
                "Number of Tasks", "Scheduler Runtime (ms)", SummaryRow::meanRuntimeMs, false));
        written.add(writeSummaryFigure(figuresDir.resolve("fig_prediction_stress_dsr.png"), summaries,
                "prediction_stress",
                "Burst Factor", "DSR (%)", SummaryRow::meanDsr, true));
        written.add(writeSummaryFigure(figuresDir.resolve("fig_prediction_stress_starvation.png"), summaries,
                "prediction_stress",
                "Burst Factor", "Credit-Starvation Events", SummaryRow::meanCreditStarvationEvents, false));
        written.add(writeSummaryFigure(figuresDir.resolve("fig_prediction_stress_fallback.png"), summaries,
                "prediction_stress",
                "Burst Factor", "Fallback VM Active Slots", SummaryRow::meanFallbackActiveVmSlots, false));

        return written;
    }

    private static Path writeArchitectureFigure(Path path) throws IOException {
        BufferedImage image = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = image.createGraphics();
        try {
            setupCanvas(g);

            int yTop = 70;
            int boxW = 210;
            int boxH = 78;
            drawBox(g, 80, yTop, boxW, boxH, "Workload Analyzer", "arrivals, workload, demand");
            drawBox(g, 330, yTop, boxW, boxH, "Deadline Manager", "deadline priority");
            drawBox(g, 580, yTop, boxW, boxH, "Credit Monitor", "current CPU credits");

            drawBox(g, 200, 300, boxW, boxH, "Credit Predictor", "future credit state");
            drawBox(g, 465, 300, boxW, boxH, "VM Selection Engine", "cost, credit, slack score");
            drawBox(g, 730, 300, boxW, boxH, "Placement Scheduler", "BADP / BADP+ decision");

            drawBox(g, 200, 500, 265, boxH, "Microservice Tasks", "ready task queue");
            drawBox(g, 590, 500, 265, boxH, "Heterogeneous VM Pool", "burstable and fallback VMs");

            drawArrow(g, 290, yTop + boxH / 2, 330, yTop + boxH / 2);
            drawArrow(g, 540, yTop + boxH / 2, 580, yTop + boxH / 2);
            drawArrow(g, 685, yTop + boxH, 570, 300);
            drawArrow(g, 410, yTop + boxH, 315, 300);
            drawArrow(g, 410, 378, 465, 340);
            drawArrow(g, 675, 339, 730, 339);
            drawArrow(g, 335, 500, 515, 378);
            drawArrow(g, 730, 378, 705, 500);
            drawArrow(g, 590, 539, 465, 539);
        } finally {
            g.dispose();
        }
        ImageIO.write(image, "png", path.toFile());
        return path;
    }

    private static Path writeWorkflowFigure(Path path) throws IOException {
        BufferedImage image = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = image.createGraphics();
        try {
            setupCanvas(g);

            int x = 95;
            int y = 70;
            int boxW = 245;
            int boxH = 72;
            int gapY = 86;

            drawBox(g, x, y, boxW, boxH, "1. Analyze Ready Tasks", "arrival, workload, deadline");
            drawBox(g, x, y + gapY, boxW, boxH, "2. Read VM Credit State", "credit balance and capacity");
            drawBox(g, x, y + 2 * gapY, boxW, boxH, "3. Predict Credits", "BADP+ horizon H");
            drawBox(g, x, y + 3 * gapY, boxW, boxH, "4. Filter Feasible VMs", "capacity, deadline, credits");
            drawBox(g, x, y + 4 * gapY, boxW, boxH, "5. Rank Candidate VMs", "cost + credit + slack");
            drawBox(g, x, y + 5 * gapY, boxW, boxH, "6. Place Tasks", "assign and update queues");

            drawBox(g, 560, y + gapY, 315, boxH, "Update VM Activation", "active slot accounting");
            drawBox(g, 560, y + 2 * gapY, 315, boxH, "Execute Slot", "process queued workload");
            drawBox(g, 560, y + 3 * gapY, 315, boxH, "Update CPU Credits", "consume and regenerate");
            drawBox(g, 560, y + 4 * gapY, 315, boxH, "Collect Metrics", "cost, DSR, ACT, CUE, AVM");
            drawBox(g, 560, y + 5 * gapY, 315, boxH, "Advance Time Slot", "repeat until horizon ends");

            for (int i = 0; i < 5; i++) {
                drawArrow(g, x + boxW / 2, y + i * gapY + boxH, x + boxW / 2, y + (i + 1) * gapY);
            }
            drawArrow(g, x + boxW, y + 5 * gapY + boxH / 2, 560, y + 5 * gapY + boxH / 2);
            for (int i = 5; i > 1; i--) {
                drawArrow(g, 717, y + i * gapY, 717, y + (i - 1) * gapY + boxH);
            }
            drawArrow(g, 875, y + 5 * gapY + boxH / 2, 965, y + 5 * gapY + boxH / 2);
            drawArrow(g, 965, y + 5 * gapY + boxH / 2, 965, y + 40);
            drawArrow(g, 965, y + 40, x + boxW / 2, y + 40);
            drawArrow(g, x + boxW / 2, y + 40, x + boxW / 2, y);
        } finally {
            g.dispose();
        }
        ImageIO.write(image, "png", path.toFile());
        return path;
    }

    private static Path writeSummaryFigure(Path path, List<SummaryRow> summaries, String experiment,
                                           String xLabel, String yLabel,
                                           ToDoubleFunction<SummaryRow> metric, boolean percentAxis)
            throws IOException {
        Map<Algorithm, List<Point>> series = new LinkedHashMap<>();
        summaries.stream()
                .filter(row -> experiment.equals(row.experiment()))
                .sorted(Comparator.comparingDouble(SummaryRow::independentVariable))
                .forEach(row -> series.computeIfAbsent(row.algorithm(), ignored -> new ArrayList<>())
                        .add(new Point(row.independentVariable(), metric.applyAsDouble(row))));
        drawLineChart(path, xLabel, yLabel, series, percentAxis);
        return path;
    }

    private static Path writeActiveVmFigure(Path path, List<RunResult> results) throws IOException {
        Map<Algorithm, Map<Integer, SlotAverage>> grouped = new LinkedHashMap<>();
        for (RunResult result : results) {
            if (!"active_vm_count".equals(result.experiment)) {
                continue;
            }
            Map<Integer, SlotAverage> bySlot = grouped.computeIfAbsent(result.algorithm, ignored -> new LinkedHashMap<>());
            for (TimeSeriesPoint point : result.timeSeries) {
                bySlot.computeIfAbsent(point.slot(), ignored -> new SlotAverage()).add(point.activeVms());
            }
        }

        Map<Algorithm, List<Point>> series = new LinkedHashMap<>();
        for (Map.Entry<Algorithm, Map<Integer, SlotAverage>> entry : grouped.entrySet()) {
            List<Point> points = entry.getValue().entrySet().stream()
                    .sorted(Map.Entry.comparingByKey())
                    .map(slot -> new Point(slot.getKey(), slot.getValue().mean()))
                    .toList();
            series.put(entry.getKey(), points);
        }
        drawLineChart(path, "Time Slot", "Active VMs", series, false);
        return path;
    }

    private static void drawLineChart(Path path, String xLabel, String yLabel,
                                      Map<Algorithm, List<Point>> series, boolean percentAxis) throws IOException {
        BufferedImage image = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = image.createGraphics();
        try {
            setupCanvas(g);

            if (series.isEmpty() || series.values().stream().allMatch(List::isEmpty)) {
                drawNoData(g);
                ImageIO.write(image, "png", path.toFile());
                return;
            }

            Bounds bounds = bounds(series, percentAxis);
            drawAxes(g, bounds, xLabel, yLabel);
            drawSeries(g, series, bounds);
            drawLegend(g, series);
        } finally {
            g.dispose();
        }
        ImageIO.write(image, "png", path.toFile());
    }

    private static void setupCanvas(Graphics2D g) {
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, WIDTH, HEIGHT);
        g.setFont(new Font("SansSerif", Font.PLAIN, 17));
    }

    private static void drawBox(Graphics2D g, int x, int y, int w, int h, String title, String subtitle) {
        g.setColor(new Color(245, 248, 252));
        g.fillRoundRect(x, y, w, h, 12, 12);
        g.setStroke(new BasicStroke(1.6f));
        g.setColor(new Color(76, 105, 140));
        g.drawRoundRect(x, y, w, h, 12, 12);
        g.setColor(new Color(25, 35, 50));
        g.setFont(new Font("SansSerif", Font.BOLD, 17));
        g.drawString(title, x + 16, y + 30);
        g.setFont(new Font("SansSerif", Font.PLAIN, 14));
        g.setColor(new Color(70, 80, 95));
        g.drawString(subtitle, x + 16, y + 54);
    }

    private static void drawArrow(Graphics2D g, int x1, int y1, int x2, int y2) {
        g.setColor(new Color(70, 90, 120));
        g.setStroke(new BasicStroke(1.8f));
        g.drawLine(x1, y1, x2, y2);
        double angle = Math.atan2(y2 - y1, x2 - x1);
        int size = 9;
        int ax1 = x2 - (int) Math.round(size * Math.cos(angle - Math.PI / 6.0));
        int ay1 = y2 - (int) Math.round(size * Math.sin(angle - Math.PI / 6.0));
        int ax2 = x2 - (int) Math.round(size * Math.cos(angle + Math.PI / 6.0));
        int ay2 = y2 - (int) Math.round(size * Math.sin(angle + Math.PI / 6.0));
        g.drawLine(x2, y2, ax1, ay1);
        g.drawLine(x2, y2, ax2, ay2);
    }

    private static Bounds bounds(Map<Algorithm, List<Point>> series, boolean percentAxis) {
        double minX = Double.POSITIVE_INFINITY;
        double maxX = Double.NEGATIVE_INFINITY;
        double maxY = Double.NEGATIVE_INFINITY;
        for (List<Point> points : series.values()) {
            for (Point point : points) {
                minX = Math.min(minX, point.x());
                maxX = Math.max(maxX, point.x());
                maxY = Math.max(maxY, point.y());
            }
        }
        if (Math.abs(maxX - minX) < 1.0e-9) {
            maxX = minX + 1.0;
        }
        double yMax = percentAxis ? 100.0 : Math.max(1.0, maxY * 1.15);
        if (!percentAxis && maxY <= 0.0) {
            yMax = 1.0;
        }
        return new Bounds(minX, maxX, 0.0, yMax);
    }

    private static void drawNoData(Graphics2D g) {
        g.setColor(new Color(80, 80, 80));
        g.setFont(new Font("SansSerif", Font.PLAIN, 18));
        g.drawString("No data available for this figure.", LEFT, TOP + 80);
    }

    private static void drawAxes(Graphics2D g, Bounds bounds, String xLabel, String yLabel) {
        int plotWidth = WIDTH - LEFT - RIGHT;
        int plotHeight = HEIGHT - TOP - BOTTOM;
        int x0 = LEFT;
        int y0 = TOP + plotHeight;

        g.setStroke(new BasicStroke(1.2f));
        g.setColor(new Color(70, 70, 70));
        g.drawLine(x0, y0, x0 + plotWidth, y0);
        g.drawLine(x0, TOP, x0, y0);

        g.setFont(new Font("SansSerif", Font.PLAIN, 16));
        FontMetrics metrics = g.getFontMetrics();
        g.setColor(new Color(225, 225, 225));
        for (int i = 0; i <= 5; i++) {
            double yValue = bounds.minY() + (bounds.maxY() - bounds.minY()) * i / 5.0;
            int y = yToPixel(yValue, bounds);
            g.drawLine(x0, y, x0 + plotWidth, y);
            g.setColor(new Color(70, 70, 70));
            String label = NUMBER.format(yValue);
            g.drawString(label, x0 - metrics.stringWidth(label) - 12, y + 6);
            g.setColor(new Color(225, 225, 225));
        }

        g.setColor(new Color(70, 70, 70));
        for (int i = 0; i <= 5; i++) {
            double xValue = bounds.minX() + (bounds.maxX() - bounds.minX()) * i / 5.0;
            int x = xToPixel(xValue, bounds);
            g.drawLine(x, y0, x, y0 + 5);
            String label = NUMBER.format(xValue);
            g.drawString(label, x - metrics.stringWidth(label) / 2, y0 + 32);
        }

        g.setFont(new Font("SansSerif", Font.BOLD, 20));
        metrics = g.getFontMetrics();
        g.drawString(xLabel, x0 + plotWidth / 2 - metrics.stringWidth(xLabel) / 2, HEIGHT - 42);

        g.rotate(-Math.PI / 2.0);
        g.drawString(yLabel, -(TOP + plotHeight / 2 + metrics.stringWidth(yLabel) / 2), 38);
        g.rotate(Math.PI / 2.0);
    }

    private static void drawSeries(Graphics2D g, Map<Algorithm, List<Point>> series, Bounds bounds) {
        for (Map.Entry<Algorithm, List<Point>> entry : series.entrySet()) {
            List<Point> points = entry.getValue();
            if (points.isEmpty()) {
                continue;
            }
            g.setColor(COLORS.getOrDefault(entry.getKey(), Color.DARK_GRAY));
            g.setStroke(new BasicStroke(entry.getKey() == Algorithm.BADP_PLUS ? 3.2f : 2.2f));
            for (int i = 1; i < points.size(); i++) {
                Point a = points.get(i - 1);
                Point b = points.get(i);
                g.drawLine(xToPixel(a.x(), bounds), yToPixel(a.y(), bounds),
                        xToPixel(b.x(), bounds), yToPixel(b.y(), bounds));
            }
            for (Point point : points) {
                int x = xToPixel(point.x(), bounds);
                int y = yToPixel(point.y(), bounds);
                g.fillOval(x - 4, y - 4, 8, 8);
            }
        }
    }

    private static void drawLegend(Graphics2D g, Map<Algorithm, List<Point>> series) {
        int x = WIDTH - RIGHT + 55;
        int y = TOP + 20;
        g.setFont(new Font("SansSerif", Font.BOLD, 18));
        g.setColor(new Color(40, 40, 40));
        g.drawString("Algorithms", x, y);
        g.setFont(new Font("SansSerif", Font.PLAIN, 16));
        y += 32;
        for (Algorithm algorithm : series.keySet()) {
            if (series.get(algorithm).isEmpty()) {
                continue;
            }
            g.setColor(COLORS.getOrDefault(algorithm, Color.DARK_GRAY));
            g.setStroke(new BasicStroke(3.0f));
            g.drawLine(x, y - 5, x + 28, y - 5);
            g.fillOval(x + 10, y - 9, 8, 8);
            g.setColor(new Color(40, 40, 40));
            g.drawString(displayName(algorithm), x + 40, y);
            y += 29;
        }
    }

    private static String displayName(Algorithm algorithm) {
        return switch (algorithm) {
            case BADP_PLUS -> "BADP+";
            case CDASCALER -> "CDAScaler";
            default -> algorithm.name();
        };
    }

    private static int xToPixel(double value, Bounds bounds) {
        double ratio = (value - bounds.minX()) / Math.max(1.0e-9, bounds.maxX() - bounds.minX());
        return LEFT + (int) Math.round(ratio * (WIDTH - LEFT - RIGHT));
    }

    private static int yToPixel(double value, Bounds bounds) {
        double ratio = (value - bounds.minY()) / Math.max(1.0e-9, bounds.maxY() - bounds.minY());
        return TOP + (HEIGHT - TOP - BOTTOM) - (int) Math.round(ratio * (HEIGHT - TOP - BOTTOM));
    }

    private record Point(double x, double y) {
    }

    private record Bounds(double minX, double maxX, double minY, double maxY) {
    }

    private static final class SlotAverage {
        private double sum;
        private int count;

        void add(double value) {
            sum += value;
            count++;
        }

        double mean() {
            return count == 0 ? 0.0 : sum / count;
        }
    }
}
