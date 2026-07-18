package org.bamof.model;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Queue;

public final class Vm {
    private final int id;
    private final VmType type;
    private final Queue<Task> queue = new ArrayDeque<>();
    private double credits;
    private boolean active;
    private double lastSlotCreditConsumption;
    private double totalCreditConsumption;
    private double totalCreditBudget;
    private int activeSlots;
    private int creditStarvationEvents;

    public Vm(int id, VmType type) {
        this(id, type, 1.0);
    }

    public Vm(int id, VmType type, double initialCreditFraction) {
        this.id = id;
        this.type = type;
        double boundedFraction = Math.max(0.0, Math.min(1.0, initialCreditFraction));
        this.credits = type.burstable() ? type.creditCapacity() * boundedFraction : 0.0;
        this.totalCreditBudget = type.burstable() ? this.credits : 0.0;
    }

    public int id() {
        return id;
    }

    public VmType type() {
        return type;
    }

    public double credits() {
        return credits;
    }

    public boolean active() {
        return active;
    }

    public boolean idle() {
        return queue.isEmpty();
    }

    public int queueSize() {
        return queue.size();
    }

    public double queuedWorkloadMi() {
        return queue.stream().mapToDouble(Task::remainingMi).sum();
    }

    public double estimatedFinishSlotFor(Task task, int currentSlot) {
        double workAhead = queuedWorkloadMi() + task.remainingMi();
        return currentSlot + Math.ceil(workAhead / type.peakMips());
    }

    public double creditAwareEstimatedFinishSlotFor(Task task, int currentSlot, int maxLookaheadSlots) {
        double workAhead = queuedWorkloadMi() + task.remainingMi();
        if (!type.burstable()) {
            return currentSlot + Math.ceil(workAhead / type.peakMips());
        }

        double simulatedCredits = credits;
        int slots = 0;
        int lookahead = Math.max(1, maxLookaheadSlots);
        while (workAhead > 1.0e-9 && slots < lookahead) {
            double utilization = Math.min(1.0, type.baselineFraction() + simulatedCredits / type.peakMips());
            double processed = type.peakMips() * utilization;
            double excess = Math.max(0.0, utilization - type.baselineFraction());
            double consumed = Math.min(simulatedCredits, excess * type.peakMips());
            workAhead -= processed;
            simulatedCredits = Math.max(0.0, simulatedCredits - consumed);
            simulatedCredits = Math.min(type.creditCapacity(), simulatedCredits + type.creditRegenerationPerSlot());
            slots++;
        }
        if (workAhead <= 1.0e-9) {
            return currentSlot + slots;
        }

        double baselineCapacity = Math.max(1.0e-9, type.peakMips() * type.baselineFraction());
        return currentSlot + slots + Math.ceil(workAhead / baselineCapacity);
    }

    public double projectedUtilizationWith(Task task) {
        double oneSlotWork = Math.min(type.peakMips(), queuedWorkloadMi() + task.remainingMi());
        return Math.max(0.0, Math.min(1.0, oneSlotWork / type.peakMips()));
    }

    public double immediateCreditNeedWith(Task task) {
        if (!type.burstable()) {
            return 0.0;
        }
        double utilization = projectedUtilizationWith(task);
        double excess = Math.max(0.0, utilization - type.baselineFraction());
        return excess * type.peakMips();
    }

    public double taskImmediateCreditNeed(Task task) {
        if (!type.burstable()) {
            return 0.0;
        }
        double oneSlotWork = Math.min(type.peakMips(), task.remainingMi());
        double utilization = Math.max(0.0, Math.min(1.0, oneSlotWork / type.peakMips()));
        double excess = Math.max(0.0, utilization - type.baselineFraction());
        return excess * type.peakMips();
    }

    public void enqueue(Task task, int slot) {
        task.assignTo(id, slot);
        queue.add(task);
        active = true;
    }

    public SlotExecution executeSlot(int slot) {
        lastSlotCreditConsumption = 0.0;
        if (queue.isEmpty()) {
            active = false;
            regenerate(false);
            return new SlotExecution(0.0, 0.0, List.of());
        }

        active = true;
        activeSlots++;
        double utilization = feasibleUtilization();
        double availableMi = type.peakMips() * utilization;
        if (type.burstable() && utilization < 0.999999 && queuedWorkloadMi() > availableMi + 1.0e-9) {
            creditStarvationEvents++;
        }
        double processed = 0.0;
        List<Task> completed = new ArrayList<>();

        while (availableMi > 1.0e-9 && !queue.isEmpty()) {
            Task task = queue.peek();
            double amount = Math.min(task.remainingMi(), availableMi);
            task.process(amount);
            processed += amount;
            availableMi -= amount;
            if (task.remainingMi() <= 1.0e-9) {
                task.completeAt(slot + 1);
                completed.add(queue.remove());
            }
        }

        if (type.burstable()) {
            double excess = Math.max(0.0, utilization - type.baselineFraction());
            lastSlotCreditConsumption = Math.min(credits, excess * type.peakMips());
            totalCreditConsumption += lastSlotCreditConsumption;
        }
        regenerate(true);
        active = !queue.isEmpty();
        return new SlotExecution(processed, lastSlotCreditConsumption, completed);
    }

    private double feasibleUtilization() {
        if (!type.burstable()) {
            return 1.0;
        }
        double burstCapacityFraction = credits / type.peakMips();
        return Math.min(1.0, type.baselineFraction() + burstCapacityFraction);
    }

    private void regenerate(boolean wasActive) {
        if (!type.burstable()) {
            return;
        }
        if (wasActive) {
            credits = Math.max(0.0, credits - lastSlotCreditConsumption);
            totalCreditBudget += type.creditRegenerationPerSlot();
            credits = Math.min(type.creditCapacity(), credits + type.creditRegenerationPerSlot());
        }
    }

    public void sortQueueByDeadline() {
        List<Task> tasks = new ArrayList<>(queue);
        tasks.sort(Comparator.comparingInt(Task::deadlineSlot));
        queue.clear();
        queue.addAll(tasks);
    }

    public double lastSlotCreditConsumption() {
        return lastSlotCreditConsumption;
    }

    public double totalCreditConsumption() {
        return totalCreditConsumption;
    }

    public double totalCreditBudget() {
        return totalCreditBudget;
    }

    public int activeSlots() {
        return activeSlots;
    }

    public int creditStarvationEvents() {
        return creditStarvationEvents;
    }

    public record SlotExecution(double processedMi, double consumedCredits, List<Task> completedTasks) {
    }
}
