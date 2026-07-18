package org.bamof.scheduler;

import java.util.List;

import org.bamof.config.SimulationConfig;
import org.bamof.model.Algorithm;
import org.bamof.model.Task;
import org.bamof.model.Vm;
import org.bamof.model.VmType;

public interface Scheduler {
    Algorithm algorithm();

    void schedule(List<Task> readyTasks, List<Vm> vms, List<VmType> vmTypes, int slot, SimulationConfig config);
}
