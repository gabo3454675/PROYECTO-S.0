package avilaos.model.interfaces;

import avilaos.util.datastructures.Queue;
import avilaos.model.pcb.PCB;
import avilaos.model.enumeration.SchedulingPolicyType;

public interface SchedulingPolicy {
    PCB selectNext(Queue<PCB> readyQueue);

    void reorder(Queue<PCB> readyQueue);

    default void onQuantumExpired() {
    }

    SchedulingPolicyType getType();

    default String getDescription() {
        return getType().getDisplayName();
    }
}