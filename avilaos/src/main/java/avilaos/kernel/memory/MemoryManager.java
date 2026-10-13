package avilaos.kernel.memory;

import avilaos.core.hardware.RAM;
import avilaos.model.pcb.PCB;
import avilaos.util.datastructures.Queue;
import avilaos.util.datastructures.LinkedQueue;

public class MemoryManager {
    private final RAM ram;
    private final Queue<PCB> newQueue;

    public MemoryManager(RAM ram) {
        this.ram = ram;
        this.newQueue = new LinkedQueue<>();
    }

    public boolean admitProcess(PCB pcb) {
        if (ram.canAllocate(pcb.getMemoryRequired())) {
            ram.allocate(pcb.getMemoryRequired());
            pcb.setState(avilaos.model.enumeration.ProcessState.READY);
            return true;
        } else {
            addToNewQueue(pcb);
            return false;
        }
    }

    public void addToNewQueue(PCB pcb) {
        pcb.setState(avilaos.model.enumeration.ProcessState.NEW);
        newQueue.enqueue(pcb);
    }

    public void releaseProcess(PCB pcb) {
        ram.free(pcb.getMemoryRequired());
        pcb.setState(avilaos.model.enumeration.ProcessState.TERMINATED);
    }

    public void checkNewQueue(avilaos.kernel.scheduler.Scheduler scheduler) {
        int size = newQueue.size();
        for (int i = 0; i < size; i++) {
            PCB pcb = newQueue.peek();
            if (ram.canAllocate(pcb.getMemoryRequired())) {
                newQueue.dequeue();
                ram.allocate(pcb.getMemoryRequired());
                pcb.setState(avilaos.model.enumeration.ProcessState.READY);
                scheduler.addToReady(pcb);
            } else {
                newQueue.enqueue(newQueue.dequeue());
            }
        }
    }

    public Queue<PCB> getNewQueue() {
        return newQueue;
    }

    public RAM getRam() {
        return ram;
    }
}