package org.bamof.scheduler;

import java.util.Comparator;
import java.util.List;

import org.bamof.config.SimulationConfig;
import org.bamof.model.Algorithm;
import org.bamof.model.Task;
import org.bamof.model.Vm;
import org.bamof.model.VmType;

public final class HpaScheduler extends SchedulerSupport {
    @Override
    public Algorithm algorithm() {
        return Algorithm.HPA;
    }

    @Override
    public void schedule(List<Task> readyTasks, List<Vm> vms, List<VmType> vmTypes, int slot, SimulationConfig config) {
        long active = Math.max(1L, vms.stream().filter(vm -> !vm.idle()).count());
        double pressure = readyTasks.size() / (double) active;
        if (pressure > config.hpaQueueThresholdSlots) {
            int additions = (int) Math.ceil(pressure / config.hpaQueueThresholdSlots);
            for (int i = 0; i < additions && vms.size() < config.maxVmCount && !readyTasks.isEmpty(); i++) {
                provision(vms, vmTypes, readyTasks.get(0), slot, config, false);
            }
        }
        readyTasks.sort(Comparator.comparingInt(Task::deadlineSlot));
        for (Task task : readyTasks) {
            Vm vm = cheapestFeasibleVm(vms, task, slot, false);
            if (vm == null) {
                vm = provision(vms, vmTypes, task, slot, config, false);
            }
            assign(task, vm, slot);
        }
    }
}
