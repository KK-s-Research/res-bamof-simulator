package org.bamof.stats;

import org.bamof.model.Algorithm;

public record WilcoxonRow(String experiment, double independentVariable, String metric,
                          Algorithm baseline, int n, double statistic, double pValue) {
    public static String csvHeader() {
        return "experiment,independentVariable,metric,baseline,n,statistic,pValue";
    }

    public String toCsvRow() {
        return String.join(",",
                experiment,
                Double.toString(independentVariable),
                metric,
                baseline.name(),
                Integer.toString(n),
                Double.toString(statistic),
                Double.toString(pValue));
    }
}
