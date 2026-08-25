package org.bamof.model;

import java.util.Locale;

public record RawWorkloadRow(
        String experiment,
        double independentVariable,
        long seed,
        String workloadType,
        int taskId,
        int arrivalSlot,
        double workloadMi,
        int deadlineSlot,
        int slackSlots) {

    public static String csvHeader() {
        return "experiment,independentVariable,seed,workloadType,taskId,arrivalSlot,workloadMi,deadlineSlot,slackSlots";
    }

    public String toCsvRow() {
        return String.format(Locale.US, "%s,%.6f,%d,%s,%d,%d,%.6f,%d,%d",
                experiment,
                independentVariable,
                seed,
                workloadType,
                taskId,
                arrivalSlot,
                workloadMi,
                deadlineSlot,
                slackSlots);
    }
}