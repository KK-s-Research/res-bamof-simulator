package org.bamof.model;

public final class Task {
    private final int id;
    private final int arrivalSlot;
    private final double workloadMi;
    private final int deadlineSlot;
    private double remainingMi;
    private int assignedVmId = -1;
    private int completionSlot = -1;
    private int firstScheduledSlot = -1;

    public Task(int id, int arrivalSlot, double workloadMi, int deadlineSlot) {
        this.id = id;
        this.arrivalSlot = arrivalSlot;
        this.workloadMi = workloadMi;
        this.deadlineSlot = deadlineSlot;
        this.remainingMi = workloadMi;
    }

    public Task copy() {
        return new Task(id, arrivalSlot, workloadMi, deadlineSlot);
    }

    public int id() {
        return id;
    }

    public int arrivalSlot() {
        return arrivalSlot;
    }

    public double workloadMi() {
        return workloadMi;
    }

    public int deadlineSlot() {
        return deadlineSlot;
    }

    public double remainingMi() {
        return remainingMi;
    }

    public void process(double amountMi) {
        remainingMi = Math.max(0.0, remainingMi - amountMi);
    }

    public boolean isCompleted() {
        return completionSlot >= 0;
    }

    public int completionSlot() {
        return completionSlot;
    }

    public void completeAt(int slot) {
        completionSlot = slot;
    }

    public int assignedVmId() {
        return assignedVmId;
    }

    public void assignTo(int vmId, int slot) {
        assignedVmId = vmId;
        if (firstScheduledSlot < 0) {
            firstScheduledSlot = slot;
        }
    }

    public boolean isAssigned() {
        return assignedVmId >= 0;
    }

    public boolean metDeadline() {
        return completionSlot >= 0 && completionSlot <= deadlineSlot;
    }
}
