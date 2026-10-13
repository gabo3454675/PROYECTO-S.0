package avilaos.model.dto;

import avilaos.model.enumeration.ProcessType;
import avilaos.model.enumeration.SchedulingPolicyType;

public record ProcessConfig(
    String name,
    int instructions,
    long memoryRequired,
    int priority,
    long deadline,
    ProcessType type,
    int computerId,
    String bufferId,
    int produceConsumeInterval,
    int elementsRequired
) {
    public ProcessConfig {
        if (name == null || name.isBlank()) throw new IllegalArgumentException("Nombre requerido");
        if (instructions <= 0) throw new IllegalArgumentException("Instrucciones > 0");
        if (memoryRequired <= 0) throw new IllegalArgumentException("Memoria > 0");
        if (priority < 0) throw new IllegalArgumentException("Prioridad >= 0");
        if (deadline < 0) throw new IllegalArgumentException("Deadline >= 0");
        if (type == null) throw new IllegalArgumentException("Tipo requerido");
        if (computerId < 0) throw new IllegalArgumentException("Computer ID >= 0");
        if (type == ProcessType.PRODUCER || type == ProcessType.CONSUMER) {
            if (bufferId == null || bufferId.isBlank()) throw new IllegalArgumentException("Buffer ID requerido para productor/consumidor");
            if (produceConsumeInterval <= 0) throw new IllegalArgumentException("Intervalo > 0");
            if (elementsRequired <= 0) throw new IllegalArgumentException("Elementos requeridos > 0");
        }
    }
}