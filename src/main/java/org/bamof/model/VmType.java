package org.bamof.model;

import java.util.ArrayList;
import java.util.List;

public final class VmType {
    private final String name;
    private final double peakMips;
    private final double baselineFraction;
    private final double creditCapacity;
    private final double creditRegenerationPerSlot;
    private final double costPerHour;
    private final boolean burstable;

    public VmType(String name, double peakMips, double baselineFraction, double creditCapacity,
                  double creditRegenerationPerSlot, double costPerHour, boolean burstable) {
        this.name = name;
        this.peakMips = peakMips;
        this.baselineFraction = baselineFraction;
        this.creditCapacity = creditCapacity;
        this.creditRegenerationPerSlot = creditRegenerationPerSlot;
        this.costPerHour = costPerHour;
        this.burstable = burstable;
    }

    public static List<VmType> defaults(double regenMultiplier) {
        List<VmType> types = new ArrayList<>();
        types.add(new VmType("T3.nano", 1000, 0.05, 144, 0.25 * regenMultiplier, 0.0052, true));
        types.add(new VmType("T3.micro", 1200, 0.10, 288, 0.50 * regenMultiplier, 0.0104, true));
        types.add(new VmType("T3.small", 1600, 0.20, 576, 1.00 * regenMultiplier, 0.0208, true));
        types.add(new VmType("T3.medium", 2200, 0.40, 1152, 2.00 * regenMultiplier, 0.0416, true));
        types.add(new VmType("M5.large", 2600, 1.00, 0, 0, 0.0960, false));
        return types;
    }

    public String name() {
        return name;
    }

    public double peakMips() {
        return peakMips;
    }

    public double baselineFraction() {
        return baselineFraction;
    }

    public double creditCapacity() {
        return creditCapacity;
    }

    public double creditRegenerationPerSlot() {
        return creditRegenerationPerSlot;
    }

    public double costPerHour() {
        return costPerHour;
    }

    public double costPerSlot() {
        return costPerHour / 60.0;
    }

    public boolean burstable() {
        return burstable;
    }
}
