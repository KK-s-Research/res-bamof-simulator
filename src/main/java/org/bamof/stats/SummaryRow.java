package org.bamof.stats;

import org.bamof.model.Algorithm;

public record SummaryRow(
        String experiment,
        Algorithm algorithm,
        double independentVariable,
        int n,
        double meanCost,
        double ciCost,
        double meanDsr,
        double ciDsr,
        double meanAct,
        double ciAct,
        double meanCue,
        double ciCue,
        double meanAvm,
        double ciAvm,
        double meanCreditStarvationEvents,
        double ciCreditStarvationEvents,
        double meanFallbackActiveVmSlots,
        double ciFallbackActiveVmSlots,
        double meanRuntimeMs,
        double ciRuntimeMs) {

    public static String csvHeader() {
        return "experiment,algorithm,independentVariable,n,meanCost,ciCost,meanDsr,ciDsr,meanAct,ciAct,meanCue,ciCue,meanAvm,ciAvm,meanCreditStarvationEvents,ciCreditStarvationEvents,meanFallbackActiveVmSlots,ciFallbackActiveVmSlots,meanRuntimeMs,ciRuntimeMs";
    }

    public String toCsvRow() {
        return String.join(",",
                experiment,
                algorithm.name(),
                Double.toString(independentVariable),
                Integer.toString(n),
                Double.toString(meanCost),
                Double.toString(ciCost),
                Double.toString(meanDsr),
                Double.toString(ciDsr),
                Double.toString(meanAct),
                Double.toString(ciAct),
                Double.toString(meanCue),
                Double.toString(ciCue),
                Double.toString(meanAvm),
                Double.toString(ciAvm),
                Double.toString(meanCreditStarvationEvents),
                Double.toString(ciCreditStarvationEvents),
                Double.toString(meanFallbackActiveVmSlots),
                Double.toString(ciFallbackActiveVmSlots),
                Double.toString(meanRuntimeMs),
                Double.toString(ciRuntimeMs));
    }
}
