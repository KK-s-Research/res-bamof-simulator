package org.bamof.scheduler;

import java.util.Comparator;
import java.util.List;

import org.bamof.config.SimulationConfig;
import org.bamof.model.Task;
import org.bamof.model.Vm;
import org.bamof.model.VmType;

abstract class AbstractBurstAwareScheduler extends SchedulerSupport {
    @Override
    public void schedule(List<Task> readyTasks, List<Vm> vms, List<VmType> vmTypes, int slot, SimulationConfig config) {
        readyTasks.sort(Comparator.comparingInt(Task::deadlineSlot).thenComparingDouble(Task::workloadMi));
        for (Task task : readyTasks) {
            if (task.isAssigned()) {
                continue;
            }
            Vm selected = selectBestVm(task, vms, slot, config);
            if (selected == null) {
                selected = provisionVm(vms, vmTypes, task, slot, config);
            }
            assign(task, selected, slot);
        }
    }

    protected Vm provisionVm(List<Vm> vms, List<VmType> vmTypes, Task task, int slot, SimulationConfig config) {
        return provision(vms, vmTypes, task, slot, config, true);
    }

    private Vm selectBestVm(Task task, List<Vm> vms, int slot, SimulationConfig config) {
        double minCost = vms.stream().mapToDouble(vm -> vm.type().costPerSlot()).min().orElse(0.0);
        double maxCost = vms.stream().mapToDouble(vm -> vm.type().costPerSlot()).max().orElse(1.0);
        Vm best = null;
        double bestScore = Double.NEGATIVE_INFINITY;
        for (Vm vm : vms) {
            if (!candidateFeasible(task, vm, slot, config)) {
                continue;
            }
            double score = score(task, vm, slot, config, minCost, maxCost);
            if (score > bestScore) {
                bestScore = score;
                best = vm;
            }
        }
        return best;
    }

    private double score(Task task, Vm vm, int slot, SimulationConfig config, double minCost, double maxCost) {
        double costScore = (maxCost - vm.type().costPerSlot()) / (maxCost - minCost + EPS);
        double creditScore = creditScore(task, vm, config);
        double finish = estimatedFinishSlot(task, vm, slot, config);
        double deadlineSlack = (task.deadlineSlot() - finish)
                / Math.max(EPS, task.deadlineSlot() - task.arrivalSlot() + EPS);
        deadlineSlack = clamp(deadlineSlack, 0.0, 1.0);
        return config.costWeight * costScore
                + config.creditWeight * creditScore
                + config.deadlineWeight * deadlineSlack;
    }

    protected boolean candidateFeasible(Task task, Vm vm, int slot, SimulationConfig config) {
        return canPlace(task, vm, slot, true);
    }

    protected double estimatedFinishSlot(Task task, Vm vm, int slot, SimulationConfig config) {
        return vm.estimatedFinishSlotFor(task, slot);
    }

    protected double currentCreditRatio(Vm vm) {
        if (!vm.type().burstable()) {
            return 1.0;
        }
        return clamp(vm.credits() / Math.max(EPS, vm.type().creditCapacity()), 0.0, 1.0);
    }

    protected double predictedCreditRatio(Task task, Vm vm, SimulationConfig config) {
        if (!vm.type().burstable()) {
            return 1.0;
        }
        double horizon = Math.max(1, config.predictionHorizon);
        double averageUtilization = Math.min(1.0,
                (vm.queuedWorkloadMi() + task.remainingMi()) / (vm.type().peakMips() * horizon));
        double predictedConsumption = Math.max(0.0, averageUtilization - vm.type().baselineFraction())
                * vm.type().peakMips() * horizon;
        double predicted = Math.min(vm.type().creditCapacity(),
                vm.credits() + vm.type().creditRegenerationPerSlot() * config.predictionHorizon - predictedConsumption);
        return clamp(predicted / Math.max(EPS, vm.type().creditCapacity()), 0.0, 1.0);
    }

    protected abstract double creditScore(Task task, Vm vm, SimulationConfig config);

    protected static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }
}
