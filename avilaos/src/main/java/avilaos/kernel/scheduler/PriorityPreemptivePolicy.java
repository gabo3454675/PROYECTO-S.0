package avilaos.kernel.scheduler;

import avilaos.model.interfaces.SchedulingPolicy;
import avilaos.model.pcb.PCB;
import avilaos.util.datastructures.Queue;
import avilaos.util.datastructures.PriorityQueue;
import avilaos.model.enumeration.SchedulingPolicyType;
import java.util.Comparator;

public class PriorityPreemptivePolicy implements SchedulingPolicy {
    private final PriorityQueue<PCB> priorityQueue;

    public PriorityPreemptivePolicy() {
        this.priorityQueue = new PriorityQueue<>(Comparator.comparingInt(PCB::getPriority)
                .thenComparingLong(PCB::getId));
    }

    @Override
    public PCB selectNext(Queue<PCB> readyQueue) {
        rebuildQueue(readyQueue);
        return priorityQueue.peek();
    }

    @Override
    public void reorder(Queue<PCB> readyQueue) {
        rebuildQueue(readyQueue);
    }

    private void rebuildQueue(Queue<PCB> readyQueue) {
        priorityQueue.clear();
        for (PCB pcb : readyQueue) {
            priorityQueue.enqueue(pcb);
        }
    }

    public boolean shouldPreempt(PCB running, PCB incoming) {
        return incoming.getPriority() < running.getPriority();
    }

    @Override
    public SchedulingPolicyType getType() {
        return SchedulingPolicyType.PRIORITY_PREEMPTIVE;
    }
}