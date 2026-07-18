package org.bamof.scheduler;

import java.util.Comparator;
import java.util.List;

import org.bamof.config.SimulationConfig;
import org.bamof.model.Algorithm;
import org.bamof.model.Task;
import org.bamof.model.Vm;
import org.bamof.model.VmType;

public final class CeduleScheduler extends SchedulerSupport {
    @Override
    public Algorithm algorithm() {
        return Algorithm.CEDULE;
    }

    @Override
    public void schedule(List<Task> readyTasks, List<Vm> vms, List<VmType> vmTypes, int slot, SimulationConfig config) {
        readyTasks.sort(Comparator.comparingInt(Task::arrivalSlot).thenComparingInt(Task::deadlineSlot));
        for (Task task : readyTasks) {
            Vm vm = vms.stream()
                    .filter(candidate -> canPlace(task, candidate, slot, true))
                    .max(Comparator.comparingDouble(this::creditPreference)
                            .thenComparing(candidate -> -candidate.type().costPerSlot()))
                    .orElse(null);
            if (vm == null) {
                vm = provision(vms, vmTypes, task, slot, config, true);
            }
            assign(task, vm, slot);
        }
    }

    private double creditPreference(Vm vm) {
        if (!vm.type().burstable()) {
            return 0.65;
        }
        return vm.credits() / Math.max(EPS, vm.type().creditCapacity());
    }
}
