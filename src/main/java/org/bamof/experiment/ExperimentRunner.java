package org.bamof.experiment;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;

import org.bamof.config.SimulationConfig;
import org.bamof.io.CsvWriter;
import org.bamof.io.FigureWriter;
import org.bamof.io.ManuscriptArtifactWriter;
import org.bamof.model.Algorithm;
import org.bamof.model.RunResult;
import org.bamof.model.Task;
import org.bamof.model.TimeSeriesPoint;
import org.bamof.sim.SchedulerFactory;
import org.bamof.sim.SimulationEngine;
import org.bamof.stats.Aggregator;
import org.bamof.stats.SummaryRow;
import org.bamof.stats.WilcoxonRow;
import org.bamof.stats.WilcoxonSignedRank;
import org.bamof.workload.SyntheticBurstyWorkloadGenerator;
import org.bamof.workload.TraceInspiredWorkloadGenerator;
import org.bamof.workload.WorkloadGenerator;

public final class ExperimentRunner {
    private static final List<Algorithm> ALL_ALGORITHMS = List.of(
            Algorithm.EDF,
            Algorithm.HPA,
            Algorithm.AMRP,
            Algorithm.CDASCALER,
            Algorithm.CEMA,
            Algorithm.CEDULE,
            Algorithm.BADP,
            Algorithm.BADP_PLUS);

    private final SimulationEngine engine = new SimulationEngine();

    public List<RunResult> run(boolean full) throws IOException {
        List<RunResult> results = new ArrayList<>();
        SimulationConfig base = new SimulationConfig();
        base.repetitions = full ? 30 : 3;
        if (!full) {
            base.horizonSlots = 240;
            base.initialVmCount = 12;
            base.maxVmCount = 120;
            base.initialCreditFraction = 0.45;
        }

        log("Configuration: repetitions=" + base.repetitions
                + ", horizonSlots=" + base.horizonSlots
                + ", initialVmCount=" + base.initialVmCount
                + ", maxVmCount=" + base.maxVmCount);
        runCostComparison(results, base, full);
        runDeadlineSatisfaction(results, base, full);
        runCreditUtilization(results, base, full);
        runActiveVmCount(results, base, full);
        runBurstSensitivity(results, base, full);
        runCreditRegenerationSensitivity(results, base, full);
        runRuntimeScalability(results, base, full);
        runPredictionStress(results, base, full);

        writeOutputs(results, full);
        return results;
    }

    private void runCostComparison(List<RunResult> results, SimulationConfig base, boolean full) {
        int[] taskCounts = full ? new int[] {500, 1000, 2000, 5000, 10000} : new int[] {100, 300};
        for (int taskCount : taskCounts) {
            runScenario(results, "cost_comparison", taskCount, base, ALL_ALGORITHMS,
                    (config, ignored) -> config.taskCount = taskCount);
        }
    }

    private void runDeadlineSatisfaction(List<RunResult> results, SimulationConfig base, boolean full) {
        double[] intensities = full ? new double[] {0.5, 0.8, 1.0, 1.3, 1.6} : new double[] {0.8, 1.3};
        for (double intensity : intensities) {
            runScenario(results, "deadline_satisfaction", intensity, base, ALL_ALGORITHMS,
                    (config, ignored) -> {
                        config.taskCount = full ? 3000 : 250;
                        config.workloadIntensity = intensity;
                    });
        }
    }

    private void runCreditUtilization(List<RunResult> results, SimulationConfig base, boolean full) {
        double[] intensities = full ? new double[] {0.5, 0.8, 1.0, 1.3, 1.6} : new double[] {0.8, 1.3};
        List<Algorithm> algorithms = List.of(Algorithm.CEDULE, Algorithm.BADP, Algorithm.BADP_PLUS);
        for (double intensity : intensities) {
            runScenario(results, "credit_utilization", intensity, base, algorithms,
                    (config, ignored) -> {
                        config.taskCount = full ? 3000 : 250;
                        config.workloadIntensity = intensity;
                        config.initialCreditFraction = full ? 0.25 : 0.45;
                    });
        }
    }

    private void runActiveVmCount(List<RunResult> results, SimulationConfig base, boolean full) {
        List<Algorithm> algorithms = List.of(Algorithm.HPA, Algorithm.CEMA, Algorithm.BADP, Algorithm.BADP_PLUS);
        runScenario(results, "active_vm_count", full ? 2000 : 250, base, algorithms,
                (config, ignored) -> {
                    config.taskCount = full ? 3000 : 250;
                    config.traceInspired = true;
                });
    }

    private void runBurstSensitivity(List<RunResult> results, SimulationConfig base, boolean full) {
        double[] burstFactors = full ? new double[] {0.2, 0.5, 0.8, 1.1, 1.5} : new double[] {0.5, 1.1};
        List<Algorithm> algorithms = List.of(Algorithm.AMRP, Algorithm.CEMA, Algorithm.BADP, Algorithm.BADP_PLUS);
        for (double burstFactor : burstFactors) {
            runScenario(results, "burst_sensitivity", burstFactor, base, algorithms,
                    (config, ignored) -> {
                        config.taskCount = full ? 3500 : 250;
                        config.burstFactor = burstFactor;
                        config.workloadIntensity = full ? 1.25 : 1.0;
                        config.initialCreditFraction = full ? 0.25 : 0.45;
                    });
        }
    }

    private void runCreditRegenerationSensitivity(List<RunResult> results, SimulationConfig base, boolean full) {
        double[] multipliers = full ? new double[] {0.5, 0.75, 1.0, 1.25, 1.5, 2.0} : new double[] {0.75, 1.5};
        List<Algorithm> algorithms = List.of(Algorithm.CEDULE, Algorithm.BADP, Algorithm.BADP_PLUS);
        for (double multiplier : multipliers) {
            runScenario(results, "credit_regeneration_sensitivity", multiplier, base, algorithms,
                    (config, ignored) -> {
                        config.taskCount = full ? 3500 : 250;
                        config.workloadIntensity = full ? 1.35 : 1.0;
                        config.burstFactor = full ? 1.1 : 0.8;
                        config.initialCreditFraction = full ? 0.18 : 0.45;
                        config.creditRegenerationMultiplier = multiplier;
                    });
        }
    }

    private void runRuntimeScalability(List<RunResult> results, SimulationConfig base, boolean full) {
        int[] taskCounts = full ? new int[] {500, 1000, 2000, 5000, 10000} : new int[] {100, 300};
        for (int taskCount : taskCounts) {
            runScenario(results, "runtime_scalability", taskCount, base, ALL_ALGORITHMS,
                    (config, ignored) -> config.taskCount = taskCount);
        }
    }

    private void runPredictionStress(List<RunResult> results, SimulationConfig base, boolean full) {
        double[] burstFactors = full ? new double[] {1.2, 1.8, 2.4, 3.0} : new double[] {1.8, 2.6};
        List<Algorithm> algorithms = List.of(Algorithm.BADP, Algorithm.BADP_PLUS);
        for (double burstFactor : burstFactors) {
            runScenario(results, "prediction_stress", burstFactor, base, algorithms,
                    (config, ignored) -> {
                        config.taskCount = full ? 6500 : 700;
                        config.workloadIntensity = full ? 2.20 : 1.80;
                        config.burstFactor = burstFactor;
                        config.initialCreditFraction = 0.55;
                        config.creditRegenerationMultiplier = full ? 0.25 : 0.30;
                        config.deadlineSlackFactor = full ? 0.0024 : 0.0027;
                        config.predictionHorizon = full ? 18 : 12;
                        config.predictedCreditSafetyThreshold = full ? 0.22 : 0.18;
                    });
        }
    }

    private void runScenario(List<RunResult> results, String experiment, double variable, SimulationConfig base,
                             List<Algorithm> algorithms, BiConsumer<SimulationConfig, Double> customizer) {
        log("");
        log("Experiment: " + experiment
                + ", variable=" + variable
                + ", algorithms=" + algorithms
                + ", repetitions=" + base.repetitions);
        for (int repetition = 0; repetition < base.repetitions; repetition++) {
            SimulationConfig config = base.copy();
            config.seed = 10_000L + repetition;
            customizer.accept(config, variable);
            WorkloadGenerator generator = config.traceInspired
                    ? new TraceInspiredWorkloadGenerator()
                    : new SyntheticBurstyWorkloadGenerator();
            List<Task> workload = generator.generate(config);
            log("  Repetition " + (repetition + 1) + "/" + base.repetitions
                    + ": seed=" + config.seed
                    + ", tasks=" + workload.size()
                    + ", workload=" + (config.traceInspired ? "trace-inspired" : "synthetic-bursty"));
            for (Algorithm algorithm : algorithms) {
                log("    Running " + algorithm + "...");
                RunResult result = engine.run(experiment, variable,
                        SchedulerFactory.create(algorithm), workload, config);
                results.add(result);
                log(String.format("      done: cost=%.6f, DSR=%.2f%%, ACT=%.2f, CUE=%.2f%%, AVM=%.2f, starvation=%.0f, fallbackSlots=%.0f, runtime=%.4f ms",
                        result.totalCost,
                        result.deadlineSatisfactionRatio,
                        result.averageCompletionTime,
                        result.creditUtilizationEfficiency,
                        result.averageActiveVms,
                        result.creditStarvationEvents,
                        result.fallbackActiveVmSlots,
                        result.schedulerRuntimeMs));
            }
        }
    }

    private void writeOutputs(List<RunResult> results, boolean full) throws IOException {
        Path root = Path.of("results", full ? "manuscript-run" : "quick-run");
        log("");
        log("Writing CSV outputs under " + root.toAbsolutePath());
        results.sort(Comparator.comparing((RunResult r) -> r.experiment)
                .thenComparingDouble(r -> r.independentVariable)
                .thenComparing(r -> r.algorithm.name())
                .thenComparingLong(r -> r.seed));

        CsvWriter.write(root.resolve("raw").resolve("runs.csv"),
                new RunResult().csvHeader(), results, RunResult::toCsvRow);
        log("  wrote raw/runs.csv (" + results.size() + " rows)");

        List<SummaryRow> summaries = Aggregator.summarize(results);
        CsvWriter.write(root.resolve("aggregate").resolve("summary.csv"),
                SummaryRow.csvHeader(), summaries, SummaryRow::toCsvRow);
        log("  wrote aggregate/summary.csv (" + summaries.size() + " rows)");

        List<TimeSeriesPoint> timeSeries = results.stream()
                .flatMap(result -> result.timeSeries.stream())
                .toList();
        CsvWriter.write(root.resolve("timeseries").resolve("active_vms.csv"),
                TimeSeriesPoint.csvHeader(), timeSeries, TimeSeriesPoint::toCsvRow);
        log("  wrote timeseries/active_vms.csv (" + timeSeries.size() + " rows)");

        List<WilcoxonRow> wilcoxonRows = wilcoxon(results);
        CsvWriter.write(root.resolve("stats").resolve("wilcoxon.csv"),
                WilcoxonRow.csvHeader(), wilcoxonRows, WilcoxonRow::toCsvRow);
        log("  wrote stats/wilcoxon.csv (" + wilcoxonRows.size() + " rows)");

        List<Path> figures = FigureWriter.writeAll(root.resolve("figures"), summaries, results);
        log("  wrote " + figures.size() + " figure PNG files:");
        for (Path figure : figures) {
            log("    " + root.relativize(figure));
        }

        List<Path> manuscriptArtifacts = ManuscriptArtifactWriter.writeAll(root.resolve("manuscript"), summaries, wilcoxonRows);
        log("  wrote " + manuscriptArtifacts.size() + " manuscript table/text artifacts:");
        for (Path artifact : manuscriptArtifacts) {
            log("    " + root.relativize(artifact));
        }
    }

    private List<WilcoxonRow> wilcoxon(List<RunResult> results) {
        Map<String, List<RunResult>> grouped = new HashMap<>();
        for (RunResult result : results) {
            grouped.computeIfAbsent(result.experiment + "|" + result.independentVariable, ignored -> new ArrayList<>())
                    .add(result);
        }
        List<WilcoxonRow> rows = new ArrayList<>();
        for (Map.Entry<String, List<RunResult>> entry : grouped.entrySet()) {
            String[] parts = entry.getKey().split("\\|");
            String experiment = parts[0];
            double variable = Double.parseDouble(parts[1]);
            for (Algorithm baseline : ALL_ALGORITHMS) {
                if (baseline == Algorithm.BADP_PLUS) {
                    continue;
                }
                addWilcoxon(rows, experiment, variable, baseline, entry.getValue(), "cost");
                addWilcoxon(rows, experiment, variable, baseline, entry.getValue(), "dsr");
            }
        }
        rows.sort(Comparator.comparing(WilcoxonRow::experiment)
                .thenComparingDouble(WilcoxonRow::independentVariable)
                .thenComparing(WilcoxonRow::metric)
                .thenComparing(row -> row.baseline().name()));
        return rows;
    }

    private void addWilcoxon(List<WilcoxonRow> rows, String experiment, double variable, Algorithm baseline,
                             List<RunResult> values, String metric) {
        List<RunResult> badpPlus = values.stream()
                .filter(result -> result.algorithm == Algorithm.BADP_PLUS)
                .sorted(Comparator.comparingLong(result -> result.seed))
                .toList();
        List<RunResult> base = values.stream()
                .filter(result -> result.algorithm == baseline)
                .sorted(Comparator.comparingLong(result -> result.seed))
                .toList();
        if (badpPlus.isEmpty() || base.isEmpty() || badpPlus.size() != base.size()) {
            return;
        }
        double[] a = new double[badpPlus.size()];
        double[] b = new double[base.size()];
        for (int i = 0; i < a.length; i++) {
            if ("cost".equals(metric)) {
                a[i] = badpPlus.get(i).totalCost;
                b[i] = base.get(i).totalCost;
            } else {
                a[i] = badpPlus.get(i).deadlineSatisfactionRatio;
                b[i] = base.get(i).deadlineSatisfactionRatio;
            }
        }
        WilcoxonSignedRank.TestResult test = WilcoxonSignedRank.test(a, b);
        rows.add(new WilcoxonRow(experiment, variable, metric, baseline, test.n(), test.statistic(), test.pValue()));
    }

    public static String algorithms() {
        return Arrays.toString(ALL_ALGORITHMS.toArray());
    }

    private void log(String message) {
        System.out.println(message);
    }
}
