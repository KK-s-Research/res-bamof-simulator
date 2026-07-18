package org.bamof.sim;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.bamof.config.SimulationConfig;
import org.bamof.model.RunResult;
import org.bamof.model.Task;
import org.bamof.model.TimeSeriesPoint;
import org.bamof.model.Vm;
import org.bamof.model.VmType;
import org.bamof.scheduler.Scheduler;

public final class SimulationEngine {
    public RunResult run(String experiment, double independentVariable, Scheduler scheduler,
                         List<Task> workload, SimulationConfig config) {
        List<Task> tasks = workload.stream().map(Task::copy).toList();
        List<VmType> vmTypes = VmType.defaults(config.creditRegenerationMultiplier);
        List<Vm> vms = createInitialPool(vmTypes, config);
        RunResult result = new RunResult();
        result.experiment = experiment;
        result.algorithm = scheduler.algorithm();
        result.seed = config.seed;
        result.independentVariable = independentVariable;
        result.submittedTasks = tasks.size();

        double totalCost = 0.0;
        long schedulerNanos = 0L;
        long schedulerCalls = 0L;
        int activeVmSlotSum = 0;

        for (int slot = 0; slot < config.horizonSlots; slot++) {
            List<Task> ready = readyTasks(tasks, slot);
            if (!ready.isEmpty()) {
                long start = System.nanoTime();
                scheduler.schedule(ready, vms, vmTypes, slot, config);
                schedulerNanos += System.nanoTime() - start;
                schedulerCalls++;
            }

            int activeAtStart = 0;
            for (Vm vm : vms) {
                if (!vm.idle()) {
                    activeAtStart++;
                    totalCost += vm.type().costPerSlot();
                }
            }
            activeVmSlotSum += activeAtStart;
            result.timeSeries.add(new TimeSeriesPoint(experiment, scheduler.algorithm(), config.seed, slot, activeAtStart));

            for (Vm vm : vms) {
                vm.sortQueueByDeadline();
                vm.executeSlot(slot);
            }
        }

        Metrics metrics = Metrics.from(tasks, vms, totalCost, activeVmSlotSum, config.horizonSlots,
                schedulerNanos, schedulerCalls);
        metrics.applyTo(result);
        return result;
    }

    private static List<Vm> createInitialPool(List<VmType> vmTypes, SimulationConfig config) {
        List<Vm> vms = new ArrayList<>(config.initialVmCount);
        for (int i = 0; i < config.initialVmCount; i++) {
            VmType type = vmTypes.get(i % vmTypes.size());
            vms.add(new Vm(i + 1, type, config.initialCreditFraction));
        }
        return vms;
    }

    private static List<Task> readyTasks(List<Task> tasks, int slot) {
        return tasks.stream()
                .filter(task -> task.arrivalSlot() <= slot)
                .filter(task -> !task.isAssigned())
                .filter(task -> !task.isCompleted())
                .filter(task -> slot <= task.deadlineSlot())
                .sorted(Comparator.comparingInt(Task::deadlineSlot))
                .collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
    }
}
