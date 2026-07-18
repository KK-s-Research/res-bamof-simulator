package org.bamof.workload;

import java.util.List;

import org.bamof.config.SimulationConfig;
import org.bamof.model.Task;

public interface WorkloadGenerator {
    List<Task> generate(SimulationConfig config);
}
