package org.bamof.workload;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Random;

import org.bamof.config.SimulationConfig;
import org.bamof.model.Task;

public final class TraceInspiredWorkloadGenerator implements WorkloadGenerator {
    private static final double MIN_WORKLOAD_MI = 500.0;
    private static final double MAX_WORKLOAD_MI = 10000.0;

    @Override
    public List<Task> generate(SimulationConfig config) {
        Random random = new Random(config.seed * 31 + 7);
        double[] weights = buildTemporalWeights(config, random);
        double total = 0.0;
        for (double weight : weights) {
            total += weight;
        }

        List<Task> tasks = new ArrayList<>(config.taskCount);
        for (int i = 0; i < config.taskCount; i++) {
            int arrival = sampleWeightedSlot(random, weights, total);
            double workload = Math.max(MIN_WORKLOAD_MI,
                    Math.min(MAX_WORKLOAD_MI, Math.exp(7.85 + 0.82 * random.nextGaussian())
                            * Math.sqrt(Math.max(0.25, config.workloadIntensity))));
            int slack = (int) Math.ceil(config.deadlineSlackFactor * workload + 3 + random.nextInt(22));
            tasks.add(new Task(i, arrival, workload, Math.min(config.horizonSlots, arrival + Math.max(1, slack))));
        }
        tasks.sort(Comparator.comparingInt(Task::arrivalSlot).thenComparingInt(Task::deadlineSlot));
        return tasks;
    }

    private static double[] buildTemporalWeights(SimulationConfig config, Random random) {
        double[] weights = new double[config.horizonSlots];
        for (int slot = 0; slot < weights.length; slot++) {
            double phase = (2.0 * Math.PI * slot) / Math.max(1, config.horizonSlots);
            double diurnal = 1.0 + 0.55 * Math.sin(phase - Math.PI / 2.0) + 0.25 * Math.sin(2.0 * phase);
            weights[slot] = Math.max(0.05, diurnal) * config.workloadIntensity;
        }
        int clusters = Math.max(2, (int) Math.round(3 + 3 * config.burstFactor));
        for (int c = 0; c < clusters; c++) {
            int center = random.nextInt(config.horizonSlots);
            double amplitude = 1.0 + config.burstFactor * (1.0 + random.nextDouble());
            double width = 15.0 + random.nextDouble() * 45.0;
            for (int slot = 0; slot < weights.length; slot++) {
                double distance = slot - center;
                weights[slot] += amplitude * Math.exp(-(distance * distance) / (2.0 * width * width));
            }
        }
        return weights;
    }

    private static int sampleWeightedSlot(Random random, double[] weights, double total) {
        double r = random.nextDouble() * total;
        double cumulative = 0.0;
        for (int i = 0; i < weights.length; i++) {
            cumulative += weights[i];
            if (cumulative >= r) {
                return i;
            }
        }
        return weights.length - 1;
    }
}
