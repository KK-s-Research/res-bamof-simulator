package org.bamof.stats;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class WilcoxonSignedRank {
    private WilcoxonSignedRank() {
    }

    public static TestResult test(double[] a, double[] b) {
        List<Diff> diffs = new ArrayList<>();
        for (int i = 0; i < Math.min(a.length, b.length); i++) {
            double d = a[i] - b[i];
            if (Math.abs(d) > 1.0e-12) {
                diffs.add(new Diff(Math.abs(d), Math.signum(d), 0.0));
            }
        }
        if (diffs.isEmpty()) {
            return new TestResult(0, 0.0, 1.0);
        }
        diffs.sort(Comparator.comparingDouble(Diff::absolute));
        for (int i = 0; i < diffs.size(); i++) {
            diffs.set(i, new Diff(diffs.get(i).absolute(), diffs.get(i).sign(), i + 1.0));
        }
        double wPlus = 0.0;
        double wMinus = 0.0;
        for (Diff diff : diffs) {
            if (diff.sign() > 0) {
                wPlus += diff.rank();
            } else {
                wMinus += diff.rank();
            }
        }
        double statistic = Math.min(wPlus, wMinus);
        int n = diffs.size();
        double mean = n * (n + 1) / 4.0;
        double variance = n * (n + 1) * (2 * n + 1) / 24.0;
        double z = variance <= 0.0 ? 0.0 : (statistic - mean) / Math.sqrt(variance);
        double p = 2.0 * normalCdf(-Math.abs(z));
        return new TestResult(n, statistic, p);
    }

    private static double normalCdf(double x) {
        return 0.5 * (1.0 + erf(x / Math.sqrt(2.0)));
    }

    private static double erf(double x) {
        double sign = Math.signum(x);
        x = Math.abs(x);
        double t = 1.0 / (1.0 + 0.3275911 * x);
        double y = 1.0 - (((((1.061405429 * t - 1.453152027) * t) + 1.421413741) * t
                - 0.284496736) * t + 0.254829592) * t * Math.exp(-x * x);
        return sign * y;
    }

    public record TestResult(int n, double statistic, double pValue) {
    }

    private record Diff(double absolute, double sign, double rank) {
    }
}
