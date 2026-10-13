package avilaos.kernel.scheduler;

import avilaos.model.interfaces.SchedulingPolicy;
import avilaos.model.pcb.PCB;
import avilaos.util.datastructures.Queue;
import avilaos.util.datastructures.PriorityQueue;
import avilaos.model.enumeration.SchedulingPolicyType;
import java.util.Comparator;

public class EDFPolicy implements SchedulingPolicy {
    private final PriorityQueue<PCB> deadlineQueue;

    public EDFPolicy() {
        this.deadlineQueue = new PriorityQueue<>(Comparator.comparingLong(PCB::getDeadline)
                .thenComparingLong(PCB::getId));
    }

    @Override
    public PCB selectNext(Queue<PCB> readyQueue) {
        rebuildQueue(readyQueue);
        return deadlineQueue.peek();
    }

    @Override
    public void reorder(Queue<PCB> readyQueue) {
        rebuildQueue(readyQueue);
    }

    private void rebuildQueue(Queue<PCB> readyQueue) {
        deadlineQueue.clear();
        for (PCB pcb : readyQueue) {
            deadlineQueue.enqueue(pcb);
        }
    }

    @Override
    public SchedulingPolicyType getType() {
        return SchedulingPolicyType.EDF;
    }
}