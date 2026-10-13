package avilaos.core.process;

import avilaos.model.pcb.PCB;
import avilaos.model.enumeration.ProcessType;
import avilaos.model.enumeration.ExecutionMode;

public class Process {
    private final PCB pcb;
    private int instructionCount;
    private int currentInstruction;

    public Process(PCB pcb, int instructionCount) {
        this.pcb = pcb;
        this.instructionCount = instructionCount;
        this.currentInstruction = 0;
    }

    public PCB getPCB() {
        return pcb;
    }

    public boolean executeInstruction() {
        if (pcb.isFinished()) return false;

        pcb.setMode(ExecutionMode.USER);
        pcb.decrementRemainingTime();
        pcb.incrementProgramCounter();
        currentInstruction++;

        return !pcb.isFinished() && currentInstruction < instructionCount;
    }

    public void executeKernelInstruction() {
        pcb.setMode(ExecutionMode.KERNEL);
        pcb.incrementProgramCounter();
    }

    public boolean hasInstructionsLeft() {
        return currentInstruction < instructionCount && !pcb.isFinished();
    }

    public int getRemainingInstructions() {
        return Math.max(0, instructionCount - currentInstruction);
    }

    public ProcessType getType() {
        return pcb.getType();
    }
}