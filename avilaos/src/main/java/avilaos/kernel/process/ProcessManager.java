package avilaos.kernel.process;

import avilaos.model.pcb.PCB;
import avilaos.model.enumeration.ProcessType;
import avilaos.util.IDGenerator;
import avilaos.util.datastructures.List;

public class ProcessManager {
    private int lastAssignedComputerIndex = 0;

    // Asignación Manual (con ID de computadora explícito)
    public PCB createProcess(String name, int computerId, ProcessType type,
                             int priority, long memoryRequired, long deadline,
                             long remainingTime) {
        return new PCB(name, computerId, type, priority, memoryRequired, deadline, remainingTime);
    }

    // Asignación Automática (Round-Robin entre la lista de IDs de computadoras activas)
    public PCB createProcessAuto(String name, List<Integer> availableComputerIds, ProcessType type,
                                 int priority, long memoryRequired, long deadline,
                                 long remainingTime) {
        if (availableComputerIds == null || availableComputerIds.isEmpty()) {
            throw new IllegalArgumentException("No hay computadoras disponibles para la asignación automática.");
        }

        // Selección Round Robin de la computadora
        int selectedComputerId = availableComputerIds.get(lastAssignedComputerIndex % availableComputerIds.size());
        lastAssignedComputerIndex++;

        return createProcess(name, selectedComputerId, type, priority, memoryRequired, deadline, remainingTime);
    }

    public void terminateProcess(PCB pcb) {
        pcb.setState(avilaos.model.enumeration.ProcessState.TERMINATED);
    }
}