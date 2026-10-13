package avilaos.kernel.scheduler;

import avilaos.model.interfaces.SchedulingPolicy;
import avilaos.model.pcb.PCB;
import avilaos.util.datastructures.Queue;
import avilaos.util.datastructures.LinkedQueue;

public class Scheduler {
    private SchedulingPolicy policy;
    private final Queue<PCB> readyQueue;

    public Scheduler(SchedulingPolicy policy) {
        this.policy = policy;
        this.readyQueue = new LinkedQueue<>();
    }

    public void setPolicy(SchedulingPolicy policy) {
        this.policy = policy;
        if (policy != null) {
            policy.reorder(readyQueue);
        }
    }

    public SchedulingPolicy getPolicy() {
        return policy;
    }

    public void addToReady(PCB pcb) {
        pcb.setState(avilaos.model.enumeration.ProcessState.READY);
        readyQueue.enqueue(pcb);
        if (policy != null) {
            policy.reorder(readyQueue);
        }
    }

    public PCB selectNext() {
        if (readyQueue.isEmpty()) return null;
        PCB next = policy.selectNext(readyQueue);
        if (next != null) {
            readyQueue.remove(next);
        }
        return next;
    }

    public void reorder() {
        if (policy != null) {
            policy.reorder(readyQueue);
        }
    }

    public void onQuantumExpired() {
        if (policy != null) {
            policy.onQuantumExpired();
        }
    }

    public Queue<PCB> getReadyQueue() {
        return readyQueue;
    }

    public boolean isReadyEmpty() {
        return readyQueue.isEmpty();
    }

    public int getReadyCount() {
        return readyQueue.size();
    }

    public boolean removeFromReady(PCB pcb) {
        return readyQueue.remove(pcb);
    }
}