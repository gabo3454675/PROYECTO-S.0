package avilaos.kernel.scheduler;

import avilaos.model.interfaces.SchedulingPolicy;
import avilaos.model.pcb.PCB;
import avilaos.util.datastructures.Queue;
import avilaos.model.enumeration.SchedulingPolicyType;

public class RoundRobinPolicy implements SchedulingPolicy {
    private final int quantum;
    private int currentQuantum;

    public RoundRobinPolicy(int quantum) {
        this.quantum = quantum;
        this.currentQuantum = 0;
    }

    @Override
    public PCB selectNext(Queue<PCB> readyQueue) {
        return readyQueue.peek();
    }

    @Override
    public void reorder(Queue<PCB> readyQueue) {
    }

    @Override
    public void onQuantumExpired() {
        currentQuantum = 0;
    }

    public int getQuantum() {
        return quantum;
    }

    public int getCurrentQuantum() {
        return currentQuantum;
    }

    public void incrementQuantum() {
        currentQuantum++;
    }

    public boolean isQuantumExpired() {
        return currentQuantum >= quantum;
    }

    @Override
    public SchedulingPolicyType getType() {
        return SchedulingPolicyType.ROUND_ROBIN;
    }
}