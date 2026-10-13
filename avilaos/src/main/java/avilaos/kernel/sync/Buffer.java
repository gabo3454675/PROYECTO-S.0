package avilaos.kernel.sync;

import avilaos.model.pcb.PCB;
import avilaos.util.datastructures.Queue;
import avilaos.util.datastructures.LinkedQueue;

import java.util.function.Consumer;

public class Buffer {
    private final String id;
    private final int capacity;
    private final int hostComputerId;
    private final Queue<Object> elements;
    private final Semaphore semMutex;
    private final Semaphore semEmpty;
    private final Semaphore semFull;
    private final long memoryUsed;
    private final int networkLatencyCycles;

    public Buffer(String id, int capacity, int hostComputerId, Consumer<PCB> onUnblockCallback, int networkLatencyCycles) {
        this.id = id;
        this.capacity = capacity;
        this.hostComputerId = hostComputerId;
        this.elements = new LinkedQueue<>();
        this.semMutex = new Semaphore(1);
        this.semEmpty = new Semaphore(capacity);
        this.semFull = new Semaphore(0);
        this.memoryUsed = (long) capacity * 1024;
        this.networkLatencyCycles = Math.max(0, networkLatencyCycles);

        // Set unblock callback on all semaphores
        semMutex.setOnUnblockCallback(onUnblockCallback);
        semEmpty.setOnUnblockCallback(onUnblockCallback);
        semFull.setOnUnblockCallback(onUnblockCallback);
    }

    // Backward compatibility constructor
    public Buffer(String id, int capacity, int hostComputerId, Consumer<PCB> onUnblockCallback) {
        this(id, capacity, hostComputerId, onUnblockCallback, 3);
    }

    // Backward compatibility constructor
    public Buffer(String id, int capacity, int hostComputerId) {
        this(id, capacity, hostComputerId, null, 3);
    }

    public boolean put(PCB producer, Object item) throws InterruptedException {
        boolean isRemote = producer.getComputerId() != hostComputerId;

        if (isRemote) {
            producer.setState(avilaos.model.enumeration.ProcessState.BLOCKED);
            producer.setBlockReason(avilaos.model.enumeration.BlockReason.NETWORK_LATENCY);
            producer.setNetworkLatencyRemaining(networkLatencyCycles);
        }

        semEmpty.wait(producer);
        if (producer.getState() == avilaos.model.enumeration.ProcessState.BLOCKED &&
            producer.getBlockReason() == avilaos.model.enumeration.BlockReason.SEMAPHORE_WAIT) {
            return false;
        }

        semMutex.wait(producer);
        if (producer.getState() == avilaos.model.enumeration.ProcessState.BLOCKED &&
            producer.getBlockReason() == avilaos.model.enumeration.BlockReason.SEMAPHORE_WAIT) {
            semEmpty.signal();
            return false;
        }

        elements.enqueue(item);
        semMutex.signal();
        semFull.signal();

        return true;
    }

    public Object get(PCB consumer) throws InterruptedException {
        boolean isRemote = consumer.getComputerId() != hostComputerId;

        if (isRemote) {
            consumer.setState(avilaos.model.enumeration.ProcessState.BLOCKED);
            consumer.setBlockReason(avilaos.model.enumeration.BlockReason.NETWORK_LATENCY);
            consumer.setNetworkLatencyRemaining(networkLatencyCycles);
        }

        semFull.wait(consumer);
        if (consumer.getState() == avilaos.model.enumeration.ProcessState.BLOCKED &&
            consumer.getBlockReason() == avilaos.model.enumeration.BlockReason.SEMAPHORE_WAIT) {
            return null;
        }

        semMutex.wait(consumer);
        if (consumer.getState() == avilaos.model.enumeration.ProcessState.BLOCKED &&
            consumer.getBlockReason() == avilaos.model.enumeration.BlockReason.SEMAPHORE_WAIT) {
            semFull.signal();
            return null;
        }

        Object item = elements.dequeue();
        semMutex.signal();
        semEmpty.signal();

        return item;
    }

    public String getId() {
        return id;
    }

    public int getCapacity() {
        return capacity;
    }

    public int getHostComputerId() {
        return hostComputerId;
    }

    public int getCurrentSize() {
        return elements.size();
    }

    public Semaphore getSemMutex() {
        return semMutex;
    }

    public Semaphore getSemEmpty() {
        return semEmpty;
    }

    public Semaphore getSemFull() {
        return semFull;
    }

    public long getMemoryUsed() {
        return memoryUsed;
    }

    public boolean isFull() {
        return elements.size() >= capacity;
    }

    public boolean isEmpty() {
        return elements.isEmpty();
    }

    @Override
    public String toString() {
        return String.format("Buffer{id='%s', size=%d/%d, host=%d, empty=%d, full=%d, mutex=%d}",
                id, elements.size(), capacity, hostComputerId,
                semEmpty.getValue(), semFull.getValue(), semMutex.getValue());
    }
}