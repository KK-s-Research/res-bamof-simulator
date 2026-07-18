package org.bamof.model;

import java.util.ArrayList;
import java.util.List;

public final class RunResult {
    public String experiment;
    public Algorithm algorithm;
    public long seed;
    public double independentVariable;
    public int submittedTasks;
    public int completedTasks;
    public int deadlineMetTasks;
    public double totalCost;
    public double deadlineSatisfactionRatio;
    public double averageCompletionTime;
    public double creditUtilizationEfficiency;
    public double averageActiveVms;
    public double creditStarvationEvents;
    public double fallbackActiveVmSlots;
    public double schedulerRuntimeMs;
    public final List<TimeSeriesPoint> timeSeries = new ArrayList<>();

    public String csvHeader() {
        return "experiment,algorithm,seed,independentVariable,submittedTasks,completedTasks,deadlineMetTasks,totalCost,dsr,act,cue,avgActiveVms,creditStarvationEvents,fallbackActiveVmSlots,runtimeMs";
    }

    public String toCsvRow() {
        return String.join(",",
                experiment,
                algorithm.name(),
                Long.toString(seed),
                Double.toString(independentVariable),
                Integer.toString(submittedTasks),
                Integer.toString(completedTasks),
                Integer.toString(deadlineMetTasks),
                Double.toString(totalCost),
                Double.toString(deadlineSatisfactionRatio),
                Double.toString(averageCompletionTime),
                Double.toString(creditUtilizationEfficiency),
                Double.toString(averageActiveVms),
                Double.toString(creditStarvationEvents),
                Double.toString(fallbackActiveVmSlots),
                Double.toString(schedulerRuntimeMs));
    }
}
