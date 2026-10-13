package avilaos.core.hardware;

import avilaos.model.enumeration.ExecutionMode;
import avilaos.model.pcb.PCB;

public class CPU {
    private PCB currentProcess;
    private ExecutionMode mode;
    private int instructionsExecutedThisCycle;
    private final int computerId;

    public CPU(int computerId) {
        this.computerId = computerId;
        this.currentProcess = null;
        this.mode = ExecutionMode.USER;
        this.instructionsExecutedThisCycle = 0;
    }

    public boolean isIdle() {
        return currentProcess == null;
    }

    public PCB getCurrentProcess() {
        return currentProcess;
    }

    public ExecutionMode getMode() {
        return mode;
    }

    public void setMode(ExecutionMode mode) {
        this.mode = mode;
    }

    public int getInstructionsExecutedThisCycle() {
        return instructionsExecutedThisCycle;
    }

    public void resetCycleCounter() {
        this.instructionsExecutedThisCycle = 0;
    }

    public void executeCycle() {
        if (currentProcess == null) return;

        instructionsExecutedThisCycle = 0;
        mode = currentProcess.getMode();

        boolean continueExecution = currentProcess.getRemainingTime() > 0 && !currentProcess.isFinished();

        if (continueExecution) {
            currentProcess.decrementRemainingTime();
            currentProcess.incrementProgramCounter();
            instructionsExecutedThisCycle = 1;
        }

        if (currentProcess.isFinished() || currentProcess.getRemainingTime() <= 0) {
            currentProcess.setState(avilaos.model.enumeration.ProcessState.TERMINATED);
        }
    }

    public void contextSwitch(PCB nextProcess) {
        this.currentProcess = nextProcess;
        if (nextProcess != null) {
            nextProcess.setState(avilaos.model.enumeration.ProcessState.RUNNING);
            nextProcess.resetQuantumUsed();
        }
    }

    public void interrupt() {
        if (currentProcess != null) {
            currentProcess.setState(avilaos.model.enumeration.ProcessState.READY);
            currentProcess = null;
        }
    }

    public int getComputerId() {
        return computerId;
    }
}