package org.bamof.sim;

import java.util.List;

import org.bamof.model.RunResult;
import org.bamof.model.Task;
import org.bamof.model.Vm;

public final class Metrics {
    private final int completedTasks;
    private final int deadlineMetTasks;
    private final double totalCost;
    private final double deadlineSatisfactionRatio;
    private final double averageCompletionTime;
    private final double creditUtilizationEfficiency;
    private final double averageActiveVms;
    private final double creditStarvationEvents;
    private final double fallbackActiveVmSlots;
    private final double schedulerRuntimeMs;

    private Metrics(int completedTasks, int deadlineMetTasks, double totalCost,
                    double deadlineSatisfactionRatio, double averageCompletionTime,
                    double creditUtilizationEfficiency, double averageActiveVms,
                    double creditStarvationEvents, double fallbackActiveVmSlots,
                    double schedulerRuntimeMs) {
        this.completedTasks = completedTasks;
        this.deadlineMetTasks = deadlineMetTasks;
        this.totalCost = totalCost;
        this.deadlineSatisfactionRatio = deadlineSatisfactionRatio;
        this.averageCompletionTime = averageCompletionTime;
        this.creditUtilizationEfficiency = creditUtilizationEfficiency;
        this.averageActiveVms = averageActiveVms;
        this.creditStarvationEvents = creditStarvationEvents;
        this.fallbackActiveVmSlots = fallbackActiveVmSlots;
        this.schedulerRuntimeMs = schedulerRuntimeMs;
    }

    public static Metrics from(List<Task> tasks, List<Vm> vms, double totalCost, int activeVmSlotSum,
                               int horizonSlots, long schedulerNanos, long schedulerCalls) {
        int completed = 0;
        int deadlineMet = 0;
        double completionTimeSum = 0.0;
        for (Task task : tasks) {
            if (task.isCompleted()) {
                completed++;
                completionTimeSum += task.completionSlot() - task.arrivalSlot();
                if (task.metDeadline()) {
                    deadlineMet++;
                }
            }
        }

        double consumedCredits = vms.stream().mapToDouble(Vm::totalCreditConsumption).sum();
        double creditBudget = vms.stream().mapToDouble(Vm::totalCreditBudget).sum();
        int starvationEvents = vms.stream().mapToInt(Vm::creditStarvationEvents).sum();
        int fallbackSlots = vms.stream()
                .filter(vm -> !vm.type().burstable())
                .mapToInt(Vm::activeSlots)
                .sum();
        double dsr = tasks.isEmpty() ? 0.0 : deadlineMet * 100.0 / tasks.size();
        double act = completed == 0 ? 0.0 : completionTimeSum / completed;
        double cue = creditBudget <= 0.0 ? 0.0 : consumedCredits * 100.0 / creditBudget;
        double avm = horizonSlots == 0 ? 0.0 : activeVmSlotSum / (double) horizonSlots;
        double runtimeMs = schedulerCalls == 0 ? 0.0 : schedulerNanos / 1_000_000.0 / schedulerCalls;
        return new Metrics(completed, deadlineMet, totalCost, dsr, act, cue, avm,
                starvationEvents, fallbackSlots, runtimeMs);
    }

    public void applyTo(RunResult result) {
        result.completedTasks = completedTasks;
        result.deadlineMetTasks = deadlineMetTasks;
        result.totalCost = totalCost;
        result.deadlineSatisfactionRatio = deadlineSatisfactionRatio;
        result.averageCompletionTime = averageCompletionTime;
        result.creditUtilizationEfficiency = creditUtilizationEfficiency;
        result.averageActiveVms = averageActiveVms;
        result.creditStarvationEvents = creditStarvationEvents;
        result.fallbackActiveVmSlots = fallbackActiveVmSlots;
        result.schedulerRuntimeMs = schedulerRuntimeMs;
    }
}
