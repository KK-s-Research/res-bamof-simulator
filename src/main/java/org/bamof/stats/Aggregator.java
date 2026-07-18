package org.bamof.stats;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.ToDoubleFunction;

import org.bamof.model.Algorithm;
import org.bamof.model.RunResult;

public final class Aggregator {
    private Aggregator() {
    }

    public static List<SummaryRow> summarize(List<RunResult> results) {
        Map<Key, List<RunResult>> groups = new LinkedHashMap<>();
        for (RunResult result : results) {
            groups.computeIfAbsent(new Key(result.experiment, result.algorithm, result.independentVariable),
                    ignored -> new ArrayList<>()).add(result);
        }

        List<SummaryRow> rows = new ArrayList<>();
        for (Map.Entry<Key, List<RunResult>> entry : groups.entrySet()) {
            Key key = entry.getKey();
            List<RunResult> values = entry.getValue();
            rows.add(new SummaryRow(
                    key.experiment,
                    key.algorithm,
                    key.independentVariable,
                    values.size(),
                    mean(values, r -> r.totalCost), ci95(values, r -> r.totalCost),
                    mean(values, r -> r.deadlineSatisfactionRatio), ci95(values, r -> r.deadlineSatisfactionRatio),
                    mean(values, r -> r.averageCompletionTime), ci95(values, r -> r.averageCompletionTime),
                    mean(values, r -> r.creditUtilizationEfficiency), ci95(values, r -> r.creditUtilizationEfficiency),
                    mean(values, r -> r.averageActiveVms), ci95(values, r -> r.averageActiveVms),
                    mean(values, r -> r.creditStarvationEvents), ci95(values, r -> r.creditStarvationEvents),
                    mean(values, r -> r.fallbackActiveVmSlots), ci95(values, r -> r.fallbackActiveVmSlots),
                    mean(values, r -> r.schedulerRuntimeMs), ci95(values, r -> r.schedulerRuntimeMs)));
        }
        rows.sort(Comparator.comparing(SummaryRow::experiment)
                .thenComparing(SummaryRow::independentVariable)
                .thenComparing(row -> row.algorithm().name()));
        return rows;
    }

    private static double mean(List<RunResult> values, ToDoubleFunction<RunResult> metric) {
        return values.stream().mapToDouble(metric).average().orElse(0.0);
    }

    private static double ci95(List<RunResult> values, ToDoubleFunction<RunResult> metric) {
        if (values.size() < 2) {
            return 0.0;
        }
        double mean = mean(values, metric);
        double sumSquares = 0.0;
        for (RunResult value : values) {
            double d = metric.applyAsDouble(value) - mean;
            sumSquares += d * d;
        }
        double sd = Math.sqrt(sumSquares / (values.size() - 1));
        return 1.96 * sd / Math.sqrt(values.size());
    }

    private record Key(String experiment, Algorithm algorithm, double independentVariable) {
    }
}
