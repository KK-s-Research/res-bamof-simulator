package org.bamof.scheduler;

import java.util.Comparator;
import java.util.List;

import org.bamof.config.SimulationConfig;
import org.bamof.model.Algorithm;
import org.bamof.model.Task;
import org.bamof.model.Vm;
import org.bamof.model.VmType;

public final class BadpPlusScheduler extends AbstractBurstAwareScheduler {
    @Override
    public Algorithm algorithm() {
        return Algorithm.BADP_PLUS;
    }

    @Override
    protected double creditScore(Task task, Vm vm, SimulationConfig config) {
        return predictedCreditRatio(task, vm, config);
    }

    @Override
    protected boolean candidateFeasible(Task task, Vm vm, int slot, SimulationConfig config) {
        if (task.isAssigned()) {
            return false;
        }
        if (estimatedFinishSlot(task, vm, slot, config) > task.deadlineSlot()) {
            return false;
        }
        if (vm.immediateCreditNeedWith(task) > vm.credits() + EPS) {
            return false;
        }
        return !vm.type().burstable()
                || predictedCreditRatio(task, vm, config) >= config.predictedCreditSafetyThreshold;
    }

    @Override
    protected double estimatedFinishSlot(Task task, Vm vm, int slot, SimulationConfig config) {
        int lookahead = Math.max(config.predictionHorizon, task.deadlineSlot() - slot);
        return vm.creditAwareEstimatedFinishSlotFor(task, slot, lookahead);
    }

    @Override
    protected Vm provisionVm(List<Vm> vms, List<VmType> vmTypes, Task task, int slot, SimulationConfig config) {
        if (vms.size() >= config.maxVmCount) {
            return null;
        }

        VmType selected = vmTypes.stream()
                .filter(type -> predictiveTypeFeasible(type, task, slot, config))
                .min(Comparator.comparingDouble(VmType::costPerSlot))
                .orElseGet(() -> vmTypes.stream()
                        .filter(type -> !type.burstable())
                        .filter(type -> slot + Math.ceil(task.remainingMi() / type.peakMips()) <= task.deadlineSlot())
                        .min(Comparator.comparingDouble(VmType::costPerSlot))
                        .orElse(null));
        if (selected == null) {
            return null;
        }
        Vm vm = new Vm(nextVmId(vms), selected, config.initialCreditFraction);
        vms.add(vm);
        return vm;
    }

    private boolean predictiveTypeFeasible(VmType type, Task task, int slot, SimulationConfig config) {
        if (!type.burstable()) {
            return slot + Math.ceil(task.remainingMi() / type.peakMips()) <= task.deadlineSlot();
        }
        Vm candidate = new Vm(-1, type, config.initialCreditFraction);
        int lookahead = Math.max(config.predictionHorizon, task.deadlineSlot() - slot);
        return candidate.creditAwareEstimatedFinishSlotFor(task, slot, lookahead) <= task.deadlineSlot()
                && candidate.immediateCreditNeedWith(task) <= candidate.credits() + EPS
                && predictedCreditRatio(task, candidate, config) >= config.predictedCreditSafetyThreshold;
    }
}
