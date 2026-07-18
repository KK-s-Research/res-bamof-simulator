package org.bamof.model;

public record TimeSeriesPoint(String experiment, Algorithm algorithm, long seed, int slot, int activeVms) {
    public static String csvHeader() {
        return "experiment,algorithm,seed,slot,activeVms";
    }

    public String toCsvRow() {
        return String.join(",",
                experiment,
                algorithm.name(),
                Long.toString(seed),
                Integer.toString(slot),
                Integer.toString(activeVms));
    }
}
