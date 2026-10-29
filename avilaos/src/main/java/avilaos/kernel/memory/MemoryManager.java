package avilaos.kernel.memory;

import avilaos.core.hardware.RAM;
import avilaos.model.pcb.PCB;
import avilaos.util.datastructures.AssociativeList;
import avilaos.util.datastructures.Queue;
import avilaos.util.datastructures.LinkedQueue;

public class MemoryManager {
    private static final long BYTES_PER_MB = 1024L * 1024L;

    private final RAM ram;
    private final Queue<PCB> newQueue;
    private final AssociativeList<Long, PCB> allocatedProcesses;

    public MemoryManager(RAM ram) {
        this.ram = ram;
        this.newQueue = new LinkedQueue<>();
        this.allocatedProcesses = new AssociativeList<>();
    }

    public boolean admitProcess(PCB pcb) {
        long memoryBytes = getMemoryBytes(pcb);
        if (memoryBytes > ram.getTotalSize()) {
            throw new IllegalArgumentException("El proceso " + pcb.getName()
                    + " requiere más memoria que la RAM total disponible.");
        }
        if (ram.canAllocate(memoryBytes)) {
            ram.allocate(memoryBytes);
            allocatedProcesses.put(pcb.getId(), pcb);
            pcb.setState(avilaos.model.enumeration.ProcessState.READY);
            return true;
        } else {
            addToNewQueue(pcb);
            return false;
        }
    }

    public void addToNewQueue(PCB pcb) {
        if (getMemoryBytes(pcb) > ram.getTotalSize()) {
            throw new IllegalArgumentException("El proceso " + pcb.getName()
                    + " requiere más memoria que la RAM total disponible.");
        }
        pcb.setState(avilaos.model.enumeration.ProcessState.NEW);
        newQueue.enqueue(pcb);
    }

    public void releaseProcess(PCB pcb) {
        if (allocatedProcesses.remove(pcb.getId()) != null) {
            ram.free(getMemoryBytes(pcb));
        }
        pcb.setState(avilaos.model.enumeration.ProcessState.TERMINATED);
    }

    public void checkNewQueue(avilaos.kernel.scheduler.Scheduler scheduler) {
        int size = newQueue.size();
        for (int i = 0; i < size; i++) {
            PCB pcb = newQueue.peek();
            long memoryBytes = getMemoryBytes(pcb);
            if (ram.canAllocate(memoryBytes)) {
                newQueue.dequeue();
                ram.allocate(memoryBytes);
                allocatedProcesses.put(pcb.getId(), pcb);
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

    private long getMemoryBytes(PCB pcb) {
        if (pcb.getMemoryRequired() <= 0) {
            throw new IllegalArgumentException("La memoria del proceso debe ser positiva.");
        }
        try {
            return Math.multiplyExact(pcb.getMemoryRequired(), BYTES_PER_MB);
        } catch (ArithmeticException ex) {
            throw new IllegalArgumentException("La memoria solicitada por el proceso "
                    + pcb.getName() + " excede el límite representable.", ex);
        }
    }
}