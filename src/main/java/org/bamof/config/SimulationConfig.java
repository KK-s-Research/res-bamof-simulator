package org.bamof.config;

public final class SimulationConfig {
    public int horizonSlots = 1440;
    public int slotDurationMinutes = 1;
    public int initialVmCount = 30;
    public int maxVmCount = 1000;
    public int predictionHorizon = 5;
    public int repetitions = 30;

    public int taskCount = 1000;
    public double workloadIntensity = 1.0;
    public double burstFactor = 0.8;
    public double creditRegenerationMultiplier = 1.0;
    public double initialCreditFraction = 0.35;
    public double deadlineSlackFactor = 0.0045;
    public double predictedCreditSafetyThreshold = 0.10;

    public double costWeight = 0.40;
    public double creditWeight = 0.30;
    public double deadlineWeight = 0.30;
    public int hpaQueueThresholdSlots = 4;
    public double burstDetectionThreshold = 1.50;
    public long seed = 1L;
    public boolean traceInspired = false;

    public SimulationConfig copy() {
        SimulationConfig c = new SimulationConfig();
        c.horizonSlots = horizonSlots;
        c.slotDurationMinutes = slotDurationMinutes;
        c.initialVmCount = initialVmCount;
        c.maxVmCount = maxVmCount;
        c.predictionHorizon = predictionHorizon;
        c.repetitions = repetitions;
        c.taskCount = taskCount;
        c.workloadIntensity = workloadIntensity;
        c.burstFactor = burstFactor;
        c.creditRegenerationMultiplier = creditRegenerationMultiplier;
        c.initialCreditFraction = initialCreditFraction;
        c.deadlineSlackFactor = deadlineSlackFactor;
        c.predictedCreditSafetyThreshold = predictedCreditSafetyThreshold;
        c.costWeight = costWeight;
        c.creditWeight = creditWeight;
        c.deadlineWeight = deadlineWeight;
        c.hpaQueueThresholdSlots = hpaQueueThresholdSlots;
        c.burstDetectionThreshold = burstDetectionThreshold;
        c.seed = seed;
        c.traceInspired = traceInspired;
        return c;
    }
}
