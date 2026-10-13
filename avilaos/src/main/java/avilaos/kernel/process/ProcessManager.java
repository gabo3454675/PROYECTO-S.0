package avilaos.kernel.process;

import avilaos.model.pcb.PCB;
import avilaos.model.enumeration.ProcessType;
import avilaos.util.IDGenerator;

public class ProcessManager {
    public PCB createProcess(String name, int computerId, ProcessType type,
                             int priority, long memoryRequired, long deadline,
                             long remainingTime) {
        return new PCB(name, computerId, type, priority, memoryRequired, deadline, remainingTime);
    }

    public void terminateProcess(PCB pcb) {
        pcb.setState(avilaos.model.enumeration.ProcessState.TERMINATED);
    }
}