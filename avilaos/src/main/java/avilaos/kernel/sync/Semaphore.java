package avilaos.kernel.sync;

import avilaos.model.pcb.PCB;
import avilaos.util.datastructures.Queue;
import avilaos.util.datastructures.LinkedQueue;

import java.util.function.Consumer;

public class Semaphore {
    private int value;
    private final Queue<PCB> waitingQueue;
    private final Object lock = new Object();
    private Consumer<PCB> onUnblockCallback;

    public Semaphore(int initialValue) {
        this.value = initialValue;
        this.waitingQueue = new LinkedQueue<>();
    }

    public void setOnUnblockCallback(Consumer<PCB> callback) {
        this.onUnblockCallback = callback;
    }

    public void wait(PCB pcb) {
        synchronized (lock) {
            value--;
            if (value < 0) {
                pcb.setState(avilaos.model.enumeration.ProcessState.BLOCKED);
                pcb.setBlockReason(avilaos.model.enumeration.BlockReason.SEMAPHORE_WAIT);
                waitingQueue.enqueue(pcb);
            }
        }
    }

    public void signal() {
        synchronized (lock) {
            value++;
            if (value <= 0 && !waitingQueue.isEmpty()) {
                PCB pcb = waitingQueue.dequeue();
                pcb.setState(avilaos.model.enumeration.ProcessState.READY);
                pcb.setBlockReason(null);
                if (onUnblockCallback != null) {
                    onUnblockCallback.accept(pcb);
                }
            }
        }
    }

    public int getValue() {
        synchronized (lock) {
            return value;
        }
    }

    public Queue<PCB> getWaitingQueue() {
        return waitingQueue;
    }

    public int getWaitingCount() {
        synchronized (lock) {
            return waitingQueue.size();
        }
    }

    public void setValue(int value) {
        synchronized (lock) {
            this.value = value;
        }
    }
}