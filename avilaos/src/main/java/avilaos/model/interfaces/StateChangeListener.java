package avilaos.model.interfaces;

import avilaos.model.pcb.PCB;
import avilaos.model.enumeration.ProcessState;

public interface StateChangeListener {
    void onStateChange(PCB pcb, ProcessState oldState, ProcessState newState);

    default void onProcessCreated(PCB pcb) {
    }

    default void onProcessTerminated(PCB pcb) {
    }
}