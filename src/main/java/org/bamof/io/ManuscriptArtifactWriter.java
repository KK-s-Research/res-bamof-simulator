package org.bamof.io;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.bamof.model.Algorithm;
import org.bamof.stats.SummaryRow;
import org.bamof.stats.WilcoxonRow;

public final class ManuscriptArtifactWriter {
    private static final DecimalFormat VALUE = new DecimalFormat("0.###");
    private static final DecimalFormat PERCENT = new DecimalFormat("0.##");
    private static final DecimalFormat P_VALUE = new DecimalFormat("0.####");

    private ManuscriptArtifactWriter() {
    }

    public static List<Path> writeAll(Path manuscriptDir, List<SummaryRow> summaries, List<WilcoxonRow> wilcoxonRows)
            throws IOException {
        Files.createDirectories(manuscriptDir);
        Path tablesDir = manuscriptDir.resolve("tables");
        Path textDir = manuscriptDir.resolve("text");
        Files.createDirectories(tablesDir);
        Files.createDirectories(textDir);

        List<Path> written = new ArrayList<>();
        written.addAll(writeStaticTables(tablesDir));
        written.addAll(writeStatisticalTable(tablesDir, wilcoxonRows));
        written.addAll(writeRunningText(textDir, summaries, wilcoxonRows));
        try (Stream<Path> files = Files.walk(manuscriptDir)) {
            return files.filter(Files::isRegularFile)
                    .sorted()
                    .toList();
        }
    }

    private static List<Path> writeStaticTables(Path tablesDir) throws IOException {
        List<Path> written = new ArrayList<>();
        written.add(writeTable(tablesDir, "table_i_notation", "Notation and Symbols Used in the BAMOF Framework",
                List.of("Symbol", "Description"),
                List.of(
                        row("$I$", "Set of microservice tasks"),
                        row("$V$", "Set of available virtual machines"),
                        row("$V_b$", "Set of burstable virtual machines, where $V_b \\subseteq V$"),
                        row("$T$", "Set of discrete scheduling time slots"),
                        row("$T^{-}$", "Set of transition slots, $T^{-}=\\{1,2,\\ldots,T-1\\}$"),
                        row("$N$", "Total number of microservice tasks"),
                        row("$K$", "Total number of virtual machines"),
                        row("$a_i$", "Arrival time slot of task $i$"),
                        row("$W_i$", "Computational workload of task $i$ in MI"),
                        row("$D_i$", "Absolute deadline slot of task $i$"),
                        row("$P_v$", "Peak processing capacity of VM $v$ in MIPS"),
                        row("$b_v$", "Baseline CPU fraction of VM $v$"),
                        row("$r_v$", "Credit regeneration rate of burstable VM $v$"),
                        row("$C_v^{max}$", "Maximum CPU-credit capacity of burstable VM $v$"),
                        row("$C_{v,t}$", "CPU-credit balance of burstable VM $v$ at slot $t$"),
                        row("$\\kappa_v$", "Operating cost of VM $v$ per scheduling slot"),
                        row("$x_{i,v}$", "Binary task-placement variable"),
                        row("$y_{v,t}$", "Binary VM-activation variable"),
                        row("$f_{i,v,t}$", "Fraction of CPU capacity allocated to task $i$"),
                        row("$u_{v,t}$", "Total CPU utilization of burstable VM $v$"),
                        row("$o_{v,t}$", "CPU-credit consumption in slot $t$"))));

        written.add(writeTable(tablesDir, "table_ii_simulation_environment", "Simulation Environment",
                List.of("Parameter", "Value"),
                List.of(
                        row("Programming language", "Java 17"),
                        row("Build system", "Maven"),
                        row("Simulation type", "Discrete-event simulation"),
                        row("Scheduling horizon", "1440 slots"),
                        row("Slot duration, $\\Delta t$", "1 minute"),
                        row("Default simulation duration", "24 hours"),
                        row("Initial VM pool size", "30 VMs"),
                        row("Maximum VM pool size", "1000 VMs"),
                        row("Prediction horizon, $H$", "5 slots"),
                        row("Independent repetitions", "30 runs"),
                        row("Confidence interval", "95\\%"))));

        written.add(writeTable(tablesDir, "table_iii_vm_configuration", "Virtual Machine Configuration Used in the Simulation",
                List.of("VM Type", "Peak Capacity (MIPS)", "Baseline CPU Fraction", "Credit Capacity", "Credit Regeneration Rate", "Cost/hr (\\$)"),
                List.of(
                        row("T3.nano", "1000", "0.05", "144", "0.25", "0.0052"),
                        row("T3.micro", "1200", "0.10", "288", "0.50", "0.0104"),
                        row("T3.small", "1600", "0.20", "576", "1.00", "0.0208"),
                        row("T3.medium", "2200", "0.40", "1152", "2.00", "0.0416"),
                        row("M5.large", "2600", "1.00", "--", "--", "0.0960"))));

        written.add(writeTable(tablesDir, "table_iv_compared_algorithms", "Compared Algorithms and Evaluation Roles",
                List.of("Method", "Category", "Role in Evaluation"),
                List.of(
                        row("EDF", "Deadline-aware scheduling", "Prioritizes tasks by non-decreasing deadlines without VM cost or CPU-credit modeling."),
                        row("HPA", "Reactive autoscaling", "Represents threshold-based CPU autoscaling inspired by Kubernetes HPA."),
                        row("AmRP", "Bursty-workload-aware autoscaling", "Represents proactive-reactive autoscaling with burst detection."),
                        row("CDAScaler", "Predictive microservice autoscaling", "Represents prediction-based container and CPU allocation."),
                        row("CEMA", "Multi-layer autoscaling", "Represents container-level and VM-level scaling with consolidation."),
                        row("CEDULE", "Burstable-instance scheduling", "Represents credit-aware burstable-instance scheduling."),
                        row("BADP", "Proposed method", "Uses deadline-first ordering with current credit-aware VM selection."),
                        row("BADP+", "Proposed predictive method", "Extends BADP using predicted future credit availability."))));

        written.add(writeTable(tablesDir, "table_v_experiment_design", "Experiment Design for Comparative Evaluation",
                List.of("Experiment", "Independent Variable", "Measured Metric", "Compared Algorithms"),
                List.of(
                        row("Cost comparison", "Number of tasks: 500, 1000, 2000, 5000, 10000", "Total infrastructure cost", "EDF, HPA, AmRP, CDAScaler, CEMA, CEDULE, BADP, BADP+"),
                        row("Deadline satisfaction", "Workload intensity: 0.5, 0.8, 1.0, 1.3, 1.6", "DSR (\\%)", "EDF, HPA, AmRP, CDAScaler, CEMA, CEDULE, BADP, BADP+"),
                        row("Credit utilization", "Workload intensity: 0.5, 0.8, 1.0, 1.3, 1.6", "CUE (\\%)", "CEDULE, BADP, BADP+"),
                        row("Active VM count", "Time and workload size", "Number of active VMs", "HPA, CEMA, BADP, BADP+"),
                        row("Burst sensitivity", "Burst factor: 0.2, 0.5, 0.8, 1.1, 1.5", "Cost and DSR", "AmRP, CEMA, BADP, BADP+"),
                        row("Credit regeneration sensitivity", "Multiplier: 0.5, 0.75, 1.0, 1.25, 1.5, 2.0", "Cost and CUE", "CEDULE, BADP, BADP+"),
                        row("Runtime scalability", "Number of tasks: 500, 1000, 2000, 5000, 10000", "Scheduler runtime", "EDF, HPA, AmRP, CDAScaler, CEMA, CEDULE, BADP, BADP+"),
                        row("Prediction stress", "Burst factor: 1.2, 1.8, 2.4, 3.0", "DSR, credit-starvation events, fallback VM active slots", "BADP, BADP+"))));

        written.add(writeTable(tablesDir, "table_vi_default_parameters", "Default Simulation Parameters",
                List.of("Parameter", "Value"),
                List.of(
                        row("Scheduling horizon", "1440 slots"),
                        row("Slot duration, $\\Delta t$", "1 minute"),
                        row("Initial VM count", "30"),
                        row("Maximum VM count", "1000"),
                        row("Prediction horizon, $H$", "5 slots"),
                        row("Deadline slack factor, $\\eta$", "0.0045"),
                        row("Baseline arrival rate", "1.0"),
                        row("Default burst factor", "0.8"),
                        row("Default credit regeneration multiplier", "1.0"),
                        row("Initial credit fraction", "0.35"),
                        row("Predictive credit safety threshold", "0.10"),
                        row("Cost weight, $\\alpha$", "0.40"),
                        row("Credit weight, $\\beta$", "0.30"),
                        row("Deadline weight, $\\gamma$", "0.30"),
                        row("HPA queue threshold", "4 slots"),
                        row("Burst detection threshold", "1.50"))));

        written.add(writeTable(tablesDir, "table_vii_comparative_summary", "Summary of Comparative Evaluation Dimensions",
                List.of("Method", "Cost-Aware", "Deadline-Aware", "Burst-Workload Aware", "Credit-Aware", "Predictive", "VM-Level Control"),
                List.of(
                        row("EDF", "No", "Yes", "No", "No", "No", "No"),
                        row("HPA", "No", "No", "Partial", "No", "No", "Partial"),
                        row("AmRP", "Partial", "No", "Yes", "No", "Yes", "No"),
                        row("CDAScaler", "Yes", "Partial", "Partial", "No", "Yes", "No"),
                        row("CEMA", "Yes", "Partial", "Partial", "No", "Yes", "Yes"),
                        row("CEDULE", "Yes", "No", "Partial", "Yes", "Partial", "Yes"),
                        row("BADP", "Yes", "Yes", "Yes", "Yes", "No", "Yes"),
                        row("BADP+", "Yes", "Yes", "Yes", "Yes", "Yes", "Yes"))));
        return written;
    }

    private static List<Path> writeStatisticalTable(Path tablesDir, List<WilcoxonRow> rows) throws IOException {
        List<String[]> tableRows = new ArrayList<>();
        for (Algorithm baseline : List.of(Algorithm.EDF, Algorithm.HPA, Algorithm.AMRP, Algorithm.CDASCALER,
                Algorithm.CEMA, Algorithm.CEDULE, Algorithm.BADP)) {
            double costP = medianP(rows, baseline, "cost");
            double dsrP = medianP(rows, baseline, "dsr");
            tableRows.add(row("BADP+ vs " + baseline.name(), formatP(costP), formatP(dsrP),
                    interpretation(costP, dsrP)));
        }
        return List.of(writeTable(tablesDir, "table_viii_statistical_significance",
                "Statistical Significance Test Results",
                List.of("Comparison", "Cost p-value", "DSR p-value", "Interpretation"),
                tableRows));
    }

    private static List<Path> writeRunningText(Path textDir, List<SummaryRow> summaries, List<WilcoxonRow> wilcoxonRows)
            throws IOException {
        List<TextValue> values = new ArrayList<>();
        Map<Key, SummaryRow> byKey = summaries.stream().collect(Collectors.toMap(
                row -> new Key(row.experiment(), row.algorithm(), row.independentVariable()),
                Function.identity(), (a, b) -> a, LinkedHashMap::new));

        addCostValues(values, byKey, "cost_comparison");
        addDsrValues(values, byKey);
        addCueValues(values, byKey);
        addActiveVmValues(values, byKey);
        addBurstValues(values, byKey);
        addCreditRegenValues(values, byKey);
        addRuntimeValues(values, byKey);
        addPredictionStressValues(values, byKey);
        addStatsValues(values, wilcoxonRows);

        Path csv = textDir.resolve("placeholder_values.csv");
        CsvWriter.write(csv, "key,value,description", values, TextValue::toCsvRow);

        Path md = textDir.resolve("running_text_placeholders.md");
        Files.writeString(md, runningTextMarkdown(values));

        Path tex = textDir.resolve("running_text_macros.tex");
        Files.writeString(tex, runningTextMacros(values));
        return List.of(csv, md, tex);
    }

    private static void addCostValues(List<TextValue> values, Map<Key, SummaryRow> byKey, String experiment) {
        double x = maxVariable(byKey, experiment);
        SummaryRow badpPlus = byKey.get(new Key(experiment, Algorithm.BADP_PLUS, x));
        if (badpPlus == null) {
            return;
        }
        for (Algorithm baseline : List.of(Algorithm.EDF, Algorithm.HPA, Algorithm.CDASCALER, Algorithm.CEMA,
                Algorithm.CEDULE, Algorithm.BADP)) {
            SummaryRow base = byKey.get(new Key(experiment, baseline, x));
            if (base != null) {
                double reduction = percentChange(base.meanCost(), badpPlus.meanCost());
                values.add(new TextValue("cost_reduction_vs_" + baseline.name().toLowerCase(), PERCENT.format(reduction) + "\\%",
                        "Infrastructure cost reduction of BADP+ relative to " + baseline + " at " + VALUE.format(x) + " tasks."));
            }
        }
    }

    private static void addDsrValues(List<TextValue> values, Map<Key, SummaryRow> byKey) {
        double x = maxVariable(byKey, "deadline_satisfaction");
        SummaryRow badpPlus = byKey.get(new Key("deadline_satisfaction", Algorithm.BADP_PLUS, x));
        if (badpPlus == null) {
            return;
        }
        values.add(new TextValue("badp_plus_dsr_high_intensity", PERCENT.format(badpPlus.meanDsr()) + "\\%",
                "BADP+ DSR at the highest evaluated workload intensity."));
        for (Algorithm baseline : List.of(Algorithm.HPA, Algorithm.AMRP, Algorithm.CEMA, Algorithm.CEDULE, Algorithm.BADP)) {
            SummaryRow base = byKey.get(new Key("deadline_satisfaction", baseline, x));
            if (base != null) {
                values.add(new TextValue("dsr_gain_vs_" + baseline.name().toLowerCase(),
                        PERCENT.format(badpPlus.meanDsr() - base.meanDsr()) + " percentage points",
                        "DSR gain of BADP+ relative to " + baseline + " at workload intensity " + VALUE.format(x) + "."));
            }
        }
    }

    private static void addCueValues(List<TextValue> values, Map<Key, SummaryRow> byKey) {
        double x = maxVariable(byKey, "credit_utilization");
        SummaryRow badpPlus = byKey.get(new Key("credit_utilization", Algorithm.BADP_PLUS, x));
        SummaryRow cedule = byKey.get(new Key("credit_utilization", Algorithm.CEDULE, x));
        if (badpPlus != null) {
            values.add(new TextValue("badp_plus_cue_high_intensity", PERCENT.format(badpPlus.meanCue()) + "\\%",
                    "BADP+ CUE at the highest evaluated workload intensity."));
        }
        if (badpPlus != null && cedule != null) {
            values.add(new TextValue("cue_difference_vs_cedule", PERCENT.format(badpPlus.meanCue() - cedule.meanCue()) + " percentage points",
                    "CUE difference of BADP+ relative to CEDULE at workload intensity " + VALUE.format(x) + "."));
        }
    }

    private static void addActiveVmValues(List<TextValue> values, Map<Key, SummaryRow> byKey) {
        double x = maxVariable(byKey, "active_vm_count");
        SummaryRow badpPlus = byKey.get(new Key("active_vm_count", Algorithm.BADP_PLUS, x));
        if (badpPlus == null) {
            return;
        }
        values.add(new TextValue("badp_plus_average_active_vms", VALUE.format(badpPlus.meanAvm()),
                "Average active VM count for BADP+ in the active-VM experiment."));
        for (Algorithm baseline : List.of(Algorithm.HPA, Algorithm.CEMA, Algorithm.BADP)) {
            SummaryRow base = byKey.get(new Key("active_vm_count", baseline, x));
            if (base != null) {
                values.add(new TextValue("active_vm_reduction_vs_" + baseline.name().toLowerCase(),
                        PERCENT.format(percentChange(base.meanAvm(), badpPlus.meanAvm())) + "\\%",
                        "Average active VM reduction of BADP+ relative to " + baseline + "."));
            }
        }
    }

    private static void addBurstValues(List<TextValue> values, Map<Key, SummaryRow> byKey) {
        double x = maxVariable(byKey, "burst_sensitivity");
        SummaryRow badpPlus = byKey.get(new Key("burst_sensitivity", Algorithm.BADP_PLUS, x));
        if (badpPlus == null) {
            return;
        }
        values.add(new TextValue("badp_plus_burst_cost_high", "\\$" + VALUE.format(badpPlus.meanCost()),
                "BADP+ cost at the highest burst factor."));
        values.add(new TextValue("badp_plus_burst_dsr_high", PERCENT.format(badpPlus.meanDsr()) + "\\%",
                "BADP+ DSR at the highest burst factor."));
    }

    private static void addCreditRegenValues(List<TextValue> values, Map<Key, SummaryRow> byKey) {
        double x = maxVariable(byKey, "credit_regeneration_sensitivity");
        SummaryRow badpPlus = byKey.get(new Key("credit_regeneration_sensitivity", Algorithm.BADP_PLUS, x));
        if (badpPlus != null) {
            values.add(new TextValue("badp_plus_credit_regen_cost_high", "\\$" + VALUE.format(badpPlus.meanCost()),
                    "BADP+ cost at the highest credit regeneration multiplier."));
            values.add(new TextValue("badp_plus_credit_regen_cue_high", PERCENT.format(badpPlus.meanCue()) + "\\%",
                    "BADP+ CUE at the highest credit regeneration multiplier."));
        }
    }

    private static void addRuntimeValues(List<TextValue> values, Map<Key, SummaryRow> byKey) {
        double x = maxVariable(byKey, "runtime_scalability");
        SummaryRow badpPlus = byKey.get(new Key("runtime_scalability", Algorithm.BADP_PLUS, x));
        if (badpPlus == null) {
            return;
        }
        values.add(new TextValue("badp_plus_runtime_max_tasks", VALUE.format(badpPlus.meanRuntimeMs()) + " ms",
                "BADP+ scheduler runtime at the largest task count."));
        SummaryRow badp = byKey.get(new Key("runtime_scalability", Algorithm.BADP, x));
        if (badp != null) {
            values.add(new TextValue("runtime_overhead_vs_badp",
                    PERCENT.format(percentIncrease(badp.meanRuntimeMs(), badpPlus.meanRuntimeMs())) + "\\%",
                    "Runtime overhead of BADP+ relative to BADP at the largest task count."));
        }
    }

    private static void addPredictionStressValues(List<TextValue> values, Map<Key, SummaryRow> byKey) {
        double x = maxVariable(byKey, "prediction_stress");
        SummaryRow badp = byKey.get(new Key("prediction_stress", Algorithm.BADP, x));
        SummaryRow badpPlus = byKey.get(new Key("prediction_stress", Algorithm.BADP_PLUS, x));
        if (badpPlus == null) {
            return;
        }
        values.add(new TextValue("badp_plus_prediction_stress_dsr_high", PERCENT.format(badpPlus.meanDsr()) + "\\%",
                "BADP+ DSR at the highest prediction-stress burst factor."));
        values.add(new TextValue("badp_plus_prediction_stress_starvation_high",
                VALUE.format(badpPlus.meanCreditStarvationEvents()),
                "Mean BADP+ credit-starvation events at the highest prediction-stress burst factor."));
        values.add(new TextValue("badp_plus_prediction_stress_fallback_high",
                VALUE.format(badpPlus.meanFallbackActiveVmSlots()),
                "Mean BADP+ fallback VM active slots at the highest prediction-stress burst factor."));
        if (badp != null) {
            values.add(new TextValue("prediction_stress_starvation_reduction_vs_badp",
                    PERCENT.format(percentChange(badp.meanCreditStarvationEvents(), badpPlus.meanCreditStarvationEvents())) + "\\%",
                    "Credit-starvation reduction of BADP+ relative to BADP at the highest prediction-stress burst factor."));
            values.add(new TextValue("prediction_stress_dsr_gain_vs_badp",
                    PERCENT.format(badpPlus.meanDsr() - badp.meanDsr()) + " percentage points",
                    "DSR gain of BADP+ relative to BADP at the highest prediction-stress burst factor."));
        }
    }

    private static void addStatsValues(List<TextValue> values, List<WilcoxonRow> rows) {
        for (Algorithm baseline : List.of(Algorithm.EDF, Algorithm.HPA, Algorithm.AMRP, Algorithm.CDASCALER,
                Algorithm.CEMA, Algorithm.CEDULE, Algorithm.BADP)) {
            values.add(new TextValue("median_cost_p_vs_" + baseline.name().toLowerCase(),
                    formatP(medianP(rows, baseline, "cost")),
                    "Median Wilcoxon cost p-value for BADP+ vs " + baseline + "."));
            values.add(new TextValue("median_dsr_p_vs_" + baseline.name().toLowerCase(),
                    formatP(medianP(rows, baseline, "dsr")),
                    "Median Wilcoxon DSR p-value for BADP+ vs " + baseline + "."));
        }
    }

    private static String runningTextMarkdown(List<TextValue> values) {
        StringBuilder sb = new StringBuilder();
        sb.append("# Running Text Placeholder Values\n\n");
        sb.append("Use these values to replace narrative placeholders in the Results and Discussion section.\n\n");
        for (TextValue value : values) {
            sb.append("- `").append(value.key()).append("`: ").append(value.value())
                    .append(" - ").append(value.description()).append('\n');
        }
        return sb.toString();
    }

    private static String runningTextMacros(List<TextValue> values) {
        StringBuilder sb = new StringBuilder();
        sb.append("% Auto-generated manuscript macros.\n");
        for (TextValue value : values) {
            String macro = value.key().replace("_", "");
            sb.append("\\newcommand{\\").append(macro).append("}{").append(value.value()).append("}\n");
        }
        return sb.toString();
    }

    private static Path writeTable(Path tablesDir, String name, String caption, List<String> headers, List<String[]> rows)
            throws IOException {
        Path csv = tablesDir.resolve(name + ".csv");
        CsvWriter.write(csv, String.join(",", headers), rows, row -> csvRow(row));

        Path tex = tablesDir.resolve(name + ".tex");
        Files.writeString(tex, latexTable(caption, headers, rows));
        return tex;
    }

    private static String latexTable(String caption, List<String> headers, List<String[]> rows) {
        StringBuilder sb = new StringBuilder();
        sb.append("\\begin{table*}[t]\n");
        sb.append("\\centering\n");
        sb.append("\\caption{").append(caption).append("}\n");
        sb.append("\\begin{tabular}{").append("l".repeat(headers.size())).append("}\n");
        sb.append("\\hline\n");
        sb.append(headers.stream().map(ManuscriptArtifactWriter::latexCell).collect(Collectors.joining(" & "))).append(" \\\\\n");
        sb.append("\\hline\n");
        for (String[] row : rows) {
            sb.append(List.of(row).stream().map(ManuscriptArtifactWriter::latexCell).collect(Collectors.joining(" & "))).append(" \\\\\n");
        }
        sb.append("\\hline\n");
        sb.append("\\end{tabular}\n");
        sb.append("\\end{table*}\n");
        return sb.toString();
    }

    private static String latexCell(String cell) {
        return Objects.toString(cell, "");
    }

    private static String csvRow(String[] row) {
        return List.of(row).stream().map(ManuscriptArtifactWriter::csvCell).collect(Collectors.joining(","));
    }

    private static String csvCell(String cell) {
        String value = Objects.toString(cell, "");
        if (value.contains(",") || value.contains("\"")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }

    private static String[] row(String... cells) {
        return cells;
    }

    private static double maxVariable(Map<Key, SummaryRow> rows, String experiment) {
        return rows.keySet().stream()
                .filter(key -> experiment.equals(key.experiment()))
                .mapToDouble(Key::variable)
                .max()
                .orElse(0.0);
    }

    private static double percentChange(double baseline, double candidate) {
        if (Math.abs(baseline) < 1.0e-12) {
            return 0.0;
        }
        return (baseline - candidate) * 100.0 / baseline;
    }

    private static double percentIncrease(double baseline, double candidate) {
        if (Math.abs(baseline) < 1.0e-12) {
            return 0.0;
        }
        return (candidate - baseline) * 100.0 / baseline;
    }

    private static double medianP(List<WilcoxonRow> rows, Algorithm baseline, String metric) {
        List<Double> pValues = rows.stream()
                .filter(row -> row.baseline() == baseline)
                .filter(row -> metric.equals(row.metric()))
                .map(WilcoxonRow::pValue)
                .sorted()
                .toList();
        if (pValues.isEmpty()) {
            return Double.NaN;
        }
        int mid = pValues.size() / 2;
        if (pValues.size() % 2 == 0) {
            return (pValues.get(mid - 1) + pValues.get(mid)) / 2.0;
        }
        return pValues.get(mid);
    }

    private static boolean significant(double p) {
        return !Double.isNaN(p) && p < 0.05;
    }

    private static String interpretation(double costP, double dsrP) {
        boolean cost = significant(costP);
        boolean dsr = significant(dsrP);
        if (cost && dsr) {
            return "Significant";
        }
        if (!cost && !dsr) {
            return "Not significant";
        }
        return "Mixed";
    }

    private static String formatP(double p) {
        if (Double.isNaN(p)) {
            return "--";
        }
        return p < 0.0001 ? "<0.0001" : P_VALUE.format(p);
    }

    private record Key(String experiment, Algorithm algorithm, double variable) {
    }

    private record TextValue(String key, String value, String description) {
        String toCsvRow() {
            return csvCell(key) + "," + csvCell(value) + "," + csvCell(description);
        }
    }
}
