package avilaos.model.pcb;

import avilaos.model.enumeration.ProcessState;
import avilaos.model.enumeration.ProcessType;
import avilaos.model.enumeration.ExecutionMode;
import avilaos.model.enumeration.BlockReason;
import avilaos.util.IDGenerator;

public final class PCB {
    private final long id;
    private final String name;
    private final int computerId;
    private ProcessState state;
    private final ProcessType type;
    private final int priority;
    private final long memoryRequired;
    private long deadline;
    private long remainingTime;
    private int programCounter;
    private ExecutionMode mode;
    private BlockReason blockReason;
    private int quantumUsed;
    private int networkLatencyRemaining;

    public PCB(String name, int computerId, ProcessType type, int priority,
               long memoryRequired, long deadline, long remainingTime) {
        this.id = IDGenerator.getNextId();
        this.name = name;
        this.computerId = computerId;
        this.state = ProcessState.NEW;
        this.type = type;
        this.priority = priority;
        this.memoryRequired = memoryRequired;
        this.deadline = deadline;
        this.remainingTime = remainingTime;
        this.programCounter = 0;
        this.mode = ExecutionMode.USER;
        this.blockReason = null;
        this.quantumUsed = 0;
        this.networkLatencyRemaining = 0;
    }

    public long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public int getComputerId() {
        return computerId;
    }

    public ProcessState getState() {
        return state;
    }

    public void setState(ProcessState state) {
        this.state = state;
    }

    public ProcessType getType() {
        return type;
    }

    public int getPriority() {
        return priority;
    }

    public long getMemoryRequired() {
        return memoryRequired;
    }

    public long getDeadline() {
        return deadline;
    }

    public void setDeadline(long deadline) {
        this.deadline = deadline;
    }

    public long getRemainingTime() {
        return remainingTime;
    }

    public void setRemainingTime(long remainingTime) {
        this.remainingTime = remainingTime;
    }

    public void decrementRemainingTime() {
        if (remainingTime > 0) remainingTime--;
    }

    public int getProgramCounter() {
        return programCounter;
    }

    public void incrementProgramCounter() {
        programCounter++;
    }

    public void setProgramCounter(int pc) {
        this.programCounter = pc;
    }

    public ExecutionMode getMode() {
        return mode;
    }

    public void setMode(ExecutionMode mode) {
        this.mode = mode;
    }

    public BlockReason getBlockReason() {
        return blockReason;
    }

    public void setBlockReason(BlockReason blockReason) {
        this.blockReason = blockReason;
    }

    public int getQuantumUsed() {
        return quantumUsed;
    }

    public void incrementQuantumUsed() {
        quantumUsed++;
    }

    public void resetQuantumUsed() {
        quantumUsed = 0;
    }

    public int getNetworkLatencyRemaining() {
        return networkLatencyRemaining;
    }

    public void setNetworkLatencyRemaining(int cycles) {
        this.networkLatencyRemaining = cycles;
    }

    public void decrementNetworkLatency() {
        if (networkLatencyRemaining > 0) networkLatencyRemaining--;
    }

    public boolean isDeadlineReached(long currentCycle) {
        return deadline > 0 && currentCycle >= deadline;
    }

    public boolean isFinished() {
        return remainingTime <= 0;
    }

    @Override
    public String toString() {
        return String.format("PCB{id=%d, name='%s', state=%s, pc=%d, mode=%s, remTime=%d, deadline=%d}",
                id, name, state, programCounter, mode, remainingTime, deadline);
    }
}