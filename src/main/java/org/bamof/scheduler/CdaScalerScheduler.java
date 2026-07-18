package org.bamof.scheduler;

import java.util.Comparator;
import java.util.List;

import org.bamof.config.SimulationConfig;
import org.bamof.model.Algorithm;
import org.bamof.model.Task;
import org.bamof.model.Vm;
import org.bamof.model.VmType;

public final class CdaScalerScheduler extends SchedulerSupport {
    @Override
    public Algorithm algorithm() {
        return Algorithm.CDASCALER;
    }

    @Override
    public void schedule(List<Task> readyTasks, List<Vm> vms, List<VmType> vmTypes, int slot, SimulationConfig config) {
        double demand = readyTasks.stream().mapToDouble(Task::remainingMi).sum();
        double capacity = vms.stream().mapToDouble(vm -> vm.type().peakMips()).sum();
        while (demand > capacity * 0.72 && vms.size() < config.maxVmCount && !readyTasks.isEmpty()) {
            Vm vm = provision(vms, vmTypes, readyTasks.get(0), slot, config, false);
            if (vm == null) {
                break;
            }
            capacity += vm.type().peakMips();
        }
        readyTasks.sort(Comparator.comparingDouble(Task::workloadMi).reversed());
        for (Task task : readyTasks) {
            Vm vm = cheapestFeasibleVm(vms, task, slot, false);
            if (vm == null) {
                vm = provision(vms, vmTypes, task, slot, config, false);
            }
            assign(task, vm, slot);
        }
    }
}
