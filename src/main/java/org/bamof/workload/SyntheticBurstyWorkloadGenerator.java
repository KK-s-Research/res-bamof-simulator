package org.bamof.workload;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Random;

import org.bamof.config.SimulationConfig;
import org.bamof.model.Task;

public final class SyntheticBurstyWorkloadGenerator implements WorkloadGenerator {
    private static final double MIN_WORKLOAD_MI = 500.0;
    private static final double MAX_WORKLOAD_MI = 10000.0;

    @Override
    public List<Task> generate(SimulationConfig config) {
        Random random = new Random(config.seed);
        List<Task> tasks = new ArrayList<>(config.taskCount);
        int burstClusters = Math.max(3, (int) Math.round(4 + config.burstFactor * 4));
        List<Integer> centers = new ArrayList<>();
        for (int i = 0; i < burstClusters; i++) {
            centers.add(60 + random.nextInt(Math.max(1, config.horizonSlots - 120)));
        }

        for (int i = 0; i < config.taskCount; i++) {
            int arrival = sampleArrival(random, config, centers);
            double workload = sampleBoundedLogNormal(random, config);
            int slack = deadlineSlack(workload, random, config);
            int deadline = Math.min(config.horizonSlots, arrival + Math.max(1, slack));
            tasks.add(new Task(i, arrival, workload, deadline));
        }
        tasks.sort(Comparator.comparingInt(Task::arrivalSlot).thenComparingInt(Task::deadlineSlot));
        return tasks;
    }

    private static int sampleArrival(Random random, SimulationConfig config, List<Integer> centers) {
        double burstProbability = Math.min(0.92, 0.15 + config.burstFactor * 0.48);
        if (random.nextDouble() < burstProbability) {
            int center = centers.get(random.nextInt(centers.size()));
            int width = Math.max(2, (int) Math.round(65.0 / Math.max(0.20, config.burstFactor * config.burstFactor)));
            int slot = center + (int) Math.round(random.nextGaussian() * width);
            return clamp(slot, 0, config.horizonSlots - 1);
        }
        return random.nextInt(config.horizonSlots);
    }

    private static double sampleBoundedLogNormal(Random random, SimulationConfig config) {
        double pressureMultiplier = Math.sqrt(Math.max(0.25, config.workloadIntensity))
                * (0.85 + 0.35 * Math.max(0.2, config.burstFactor));
        double value = Math.exp(8.0 + 0.75 * random.nextGaussian()) * pressureMultiplier;
        return Math.max(MIN_WORKLOAD_MI, Math.min(MAX_WORKLOAD_MI, value));
    }

    private static int deadlineSlack(double workload, Random random, SimulationConfig config) {
        double urgency = Math.max(0.35, 1.0 / Math.max(0.25, config.workloadIntensity));
        double burstTightening = Math.max(0.55, 1.15 - 0.25 * config.burstFactor);
        double base = config.deadlineSlackFactor * workload * urgency * burstTightening;
        double jitter = 2.0 + random.nextInt(18);
        return (int) Math.ceil(base + jitter);
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }
}
