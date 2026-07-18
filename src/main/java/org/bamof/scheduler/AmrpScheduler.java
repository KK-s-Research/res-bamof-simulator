package org.bamof.scheduler;

import java.util.Comparator;
import java.util.List;

import org.bamof.config.SimulationConfig;
import org.bamof.model.Algorithm;
import org.bamof.model.Task;
import org.bamof.model.Vm;
import org.bamof.model.VmType;

public final class AmrpScheduler extends SchedulerSupport {
    private double movingReadyCount = 1.0;

    @Override
    public Algorithm algorithm() {
        return Algorithm.AMRP;
    }

    @Override
    public void schedule(List<Task> readyTasks, List<Vm> vms, List<VmType> vmTypes, int slot, SimulationConfig config) {
        boolean burst = readyTasks.size() > Math.max(2.0, movingReadyCount * config.burstDetectionThreshold);
        movingReadyCount = 0.85 * movingReadyCount + 0.15 * readyTasks.size();
        if (burst) {
            int additions = Math.max(1, (int) Math.ceil(readyTasks.size() * 0.08));
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
