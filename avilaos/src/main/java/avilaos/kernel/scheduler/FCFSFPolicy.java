package avilaos.kernel.scheduler;

import avilaos.model.interfaces.SchedulingPolicy;
import avilaos.model.pcb.PCB;
import avilaos.util.datastructures.Queue;
import avilaos.model.enumeration.SchedulingPolicyType;

public class FCFSFPolicy implements SchedulingPolicy {
    @Override
    public PCB selectNext(Queue<PCB> readyQueue) {
        return readyQueue.peek();
    }

    @Override
    public void reorder(Queue<PCB> readyQueue) {
    }

    @Override
    public SchedulingPolicyType getType() {
        return SchedulingPolicyType.FCFS;
    }
}