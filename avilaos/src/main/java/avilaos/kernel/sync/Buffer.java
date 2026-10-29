package avilaos.kernel.sync;

import avilaos.model.enumeration.BlockReason;
import avilaos.model.enumeration.ProcessState;
import avilaos.model.pcb.PCB;
import avilaos.util.datastructures.AssociativeList;
import avilaos.util.datastructures.LinkedQueue;
import avilaos.util.datastructures.Queue;

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

    // Etapas de operación por PCB ID:
    // -1 = Esperando latencia de red
    //  0 = Esperando recurso de conteo (semEmpty o semFull)
    //  1 = Recurso de conteo adquirido, procediendo a mutex y sección crítica
    private final AssociativeList<Long, Integer> activePuts;
    private final AssociativeList<Long, Integer> activeGets;
    private final Object bufferLock = new Object();

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
        this.activePuts = new AssociativeList<>();
        this.activeGets = new AssociativeList<>();

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
        synchronized (bufferLock) {
            boolean isRemote = producer.getComputerId() != hostComputerId;
            Integer stage = activePuts.get(producer.getId());

            // 1. Manejo de latencia de red remota
            if (isRemote && networkLatencyCycles > 0) {
                if (stage == null) {
                    producer.setState(ProcessState.BLOCKED);
                    producer.setBlockReason(BlockReason.NETWORK_LATENCY);
                    producer.setNetworkLatencyRemaining(networkLatencyCycles);
                    activePuts.put(producer.getId(), -1);
                    return false;
                } else if (stage == -1) {
                    if (producer.getBlockReason() == BlockReason.NETWORK_LATENCY || producer.getNetworkLatencyRemaining() > 0) {
                        return false;
                    }
                    stage = 0;
                    activePuts.put(producer.getId(), 0);
                }
            } else if (stage == null) {
                stage = 0;
                activePuts.put(producer.getId(), 0);
            }

            // 2. Esperar espacio libre (semEmpty) si aún no ha adquirido el permiso
            if (stage == 0) {
                semEmpty.wait(producer);
                if (producer.getState() == ProcessState.BLOCKED &&
                    producer.getBlockReason() == BlockReason.SEMAPHORE_WAIT) {
                    activePuts.put(producer.getId(), 1);
                    return false;
                }
                stage = 1;
                activePuts.put(producer.getId(), 1);
            }

            // 3. Adquirir exclusión mutua (semMutex)
            semMutex.wait(producer);
            if (producer.getState() == ProcessState.BLOCKED &&
                producer.getBlockReason() == BlockReason.SEMAPHORE_WAIT) {
                return false;
            }

            // 4. Sección crítica: inserción en el buffer acotado
            elements.enqueue(item);
            activePuts.remove(producer.getId());

            semMutex.signal();
            semFull.signal();

            return true;
        }
    }

    public Object get(PCB consumer) throws InterruptedException {
        synchronized (bufferLock) {
            boolean isRemote = consumer.getComputerId() != hostComputerId;
            Integer stage = activeGets.get(consumer.getId());

            // 1. Manejo de latencia de red remota
            if (isRemote && networkLatencyCycles > 0) {
                if (stage == null) {
                    consumer.setState(ProcessState.BLOCKED);
                    consumer.setBlockReason(BlockReason.NETWORK_LATENCY);
                    consumer.setNetworkLatencyRemaining(networkLatencyCycles);
                    activeGets.put(consumer.getId(), -1);
                    return null;
                } else if (stage == -1) {
                    if (consumer.getBlockReason() == BlockReason.NETWORK_LATENCY || consumer.getNetworkLatencyRemaining() > 0) {
                        return null;
                    }
                    stage = 0;
                    activeGets.put(consumer.getId(), 0);
                }
            } else if (stage == null) {
                stage = 0;
                activeGets.put(consumer.getId(), 0);
            }

            // 2. Esperar elemento disponible (semFull) si aún no ha adquirido el permiso
            if (stage == 0) {
                semFull.wait(consumer);
                if (consumer.getState() == ProcessState.BLOCKED &&
                    consumer.getBlockReason() == BlockReason.SEMAPHORE_WAIT) {
                    activeGets.put(consumer.getId(), 1);
                    return null;
                }
                stage = 1;
                activeGets.put(consumer.getId(), 1);
            }

            // 3. Adquirir exclusión mutua (semMutex)
            semMutex.wait(consumer);
            if (consumer.getState() == ProcessState.BLOCKED &&
                consumer.getBlockReason() == BlockReason.SEMAPHORE_WAIT) {
                return null;
            }

            // 4. Sección crítica: extracción del elemento
            Object item = elements.dequeue();
            activeGets.remove(consumer.getId());

            semMutex.signal();
            semEmpty.signal();

            return item;
        }
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
        synchronized (bufferLock) {
            return elements.size();
        }
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
        synchronized (bufferLock) {
            return elements.size() >= capacity;
        }
    }

    public boolean isEmpty() {
        synchronized (bufferLock) {
            return elements.isEmpty();
        }
    }

    public void cancelProcess(PCB process) {
        synchronized (bufferLock) {
            activePuts.remove(process.getId());
            activeGets.remove(process.getId());
            semEmpty.cancelWait(process);
            semFull.cancelWait(process);
            semMutex.cancelWait(process);
        }
    }

    public int getNetworkLatencyCycles() {
        return networkLatencyCycles;
    }

    @Override
    public String toString() {
        synchronized (bufferLock) {
            return String.format("Buffer{id='%s', size=%d/%d, host=%d, empty=%d, full=%d, mutex=%d}",
                    id, elements.size(), capacity, hostComputerId,
                    semEmpty.getValue(), semFull.getValue(), semMutex.getValue());
        }
    }
}