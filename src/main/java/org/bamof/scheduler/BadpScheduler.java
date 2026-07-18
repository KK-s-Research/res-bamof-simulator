package org.bamof.scheduler;

import org.bamof.config.SimulationConfig;
import org.bamof.model.Algorithm;
import org.bamof.model.Task;
import org.bamof.model.Vm;

public final class BadpScheduler extends AbstractBurstAwareScheduler {
    @Override
    public Algorithm algorithm() {
        return Algorithm.BADP;
    }

    @Override
    protected double creditScore(Task task, Vm vm, SimulationConfig config) {
        return currentCreditRatio(vm);
    }
}
