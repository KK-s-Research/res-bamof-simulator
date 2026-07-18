package org.bamof.scheduler;

import java.util.Comparator;
import java.util.List;

import org.bamof.config.SimulationConfig;
import org.bamof.model.Algorithm;
import org.bamof.model.Task;
import org.bamof.model.Vm;
import org.bamof.model.VmType;

public final class EdfScheduler extends SchedulerSupport {
    @Override
    public Algorithm algorithm() {
        return Algorithm.EDF;
    }

    @Override
    public void schedule(List<Task> readyTasks, List<Vm> vms, List<VmType> vmTypes, int slot, SimulationConfig config) {
        readyTasks.sort(Comparator.comparingInt(Task::deadlineSlot));
        for (Task task : readyTasks) {
            Vm vm = vms.stream()
                    .filter(candidate -> !task.isAssigned())
                    .filter(candidate -> candidate.estimatedFinishSlotFor(task, slot) <= task.deadlineSlot())
                    .min(Comparator.comparingDouble(candidate -> candidate.estimatedFinishSlotFor(task, slot)))
                    .orElse(null);
            if (vm == null) {
                vm = provision(vms, vmTypes, task, slot, config, false);
            }
            assign(task, vm, slot);
        }
    }
}
