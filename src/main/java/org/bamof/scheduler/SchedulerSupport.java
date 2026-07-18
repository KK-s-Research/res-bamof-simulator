package org.bamof.scheduler;

import java.util.Comparator;
import java.util.List;

import org.bamof.config.SimulationConfig;
import org.bamof.model.Task;
import org.bamof.model.Vm;
import org.bamof.model.VmType;

abstract class SchedulerSupport implements Scheduler {
    protected static final double EPS = 1.0e-9;

    protected boolean canPlace(Task task, Vm vm, int slot, boolean checkCredits) {
        if (task.isAssigned()) {
            return false;
        }
        double finish = vm.estimatedFinishSlotFor(task, slot);
        if (finish > task.deadlineSlot()) {
            return false;
        }
        return !checkCredits || vm.taskImmediateCreditNeed(task) <= vm.credits() + EPS;
    }

    protected Vm provision(List<Vm> vms, List<VmType> vmTypes, Task task, int slot, SimulationConfig config,
                           boolean preferBurstable) {
        if (vms.size() >= config.maxVmCount) {
            return null;
        }
        VmType selected = vmTypes.stream()
                .filter(type -> !preferBurstable || type.burstable())
                .filter(type -> slot + Math.ceil(task.remainingMi() / type.peakMips()) <= task.deadlineSlot())
                .filter(type -> hasInitialCreditFor(type, task, config))
                .min(Comparator.comparingDouble(VmType::costPerSlot))
                .orElseGet(() -> vmTypes.stream()
                        .filter(type -> slot + Math.ceil(task.remainingMi() / type.peakMips()) <= task.deadlineSlot())
                        .filter(type -> hasInitialCreditFor(type, task, config))
                        .min(Comparator.comparingDouble(VmType::costPerSlot))
                        .orElse(null));
        if (selected == null) {
            return null;
        }
        Vm vm = new Vm(nextVmId(vms), selected, config.initialCreditFraction);
        vms.add(vm);
        return vm;
    }

    private boolean hasInitialCreditFor(VmType type, Task task, SimulationConfig config) {
        if (!type.burstable()) {
            return true;
        }
        double utilization = Math.max(0.0, Math.min(1.0, task.remainingMi() / type.peakMips()));
        double need = Math.max(0.0, utilization - type.baselineFraction()) * type.peakMips();
        return need <= type.creditCapacity() * config.initialCreditFraction + EPS;
    }

    protected int nextVmId(List<Vm> vms) {
        return vms.stream().mapToInt(Vm::id).max().orElse(0) + 1;
    }

    protected Vm leastLoadedVm(List<Vm> vms) {
        return vms.stream()
                .min(Comparator.comparingDouble(Vm::queuedWorkloadMi)
                        .thenComparingDouble(vm -> vm.type().costPerSlot()))
                .orElse(null);
    }

    protected Vm cheapestFeasibleVm(List<Vm> vms, Task task, int slot, boolean checkCredits) {
        return vms.stream()
                .filter(vm -> canPlace(task, vm, slot, checkCredits))
                .min(Comparator.comparingDouble((Vm vm) -> vm.type().costPerSlot())
                        .thenComparingDouble(vm -> vm.estimatedFinishSlotFor(task, slot)))
                .orElse(null);
    }

    protected void assign(Task task, Vm vm, int slot) {
        if (vm != null && !task.isAssigned()) {
            vm.enqueue(task, slot);
        }
    }
}
