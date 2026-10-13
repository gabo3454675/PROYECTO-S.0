package avilaos.model.enumeration;

public enum SchedulingPolicyType {
    FCFS("FCFS (First Come First Served)"),
    EDF("EDF (Earliest Deadline First)"),
    ROUND_ROBIN("Round Robin"),
    PRIORITY_PREEMPTIVE("Prioridades Apropiativas");

    private final String displayName;

    SchedulingPolicyType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public boolean usesQuantum() {
        return this == ROUND_ROBIN;
    }

    public boolean isPreemptive() {
        return this == ROUND_ROBIN || this == PRIORITY_PREEMPTIVE || this == EDF;
    }
}