package avilaos.kernel.sync;

import avilaos.model.pcb.PCB;
import avilaos.util.datastructures.LinkedList;
import avilaos.util.datastructures.List;
import avilaos.model.enumeration.BlockReason;

import java.util.function.Consumer;

public class SyncManager {
    private final List<Buffer> buffers;
    private final Consumer<PCB> onUnblockCallback;
    private int networkLatencyCycles;

    public SyncManager(Consumer<PCB> onUnblockCallback) {
        this.buffers = new LinkedList<>();
        this.onUnblockCallback = onUnblockCallback;
        this.networkLatencyCycles = 3; // default
    }

    public void setNetworkLatencyCycles(int cycles) {
        this.networkLatencyCycles = Math.max(0, cycles);
    }

    public int getNetworkLatencyCycles() {
        return networkLatencyCycles;
    }

    public Buffer createBuffer(String id, int capacity, int hostComputerId) {
        Buffer buffer = new Buffer(id, capacity, hostComputerId, onUnblockCallback, networkLatencyCycles);
        buffers.add(buffer);
        return buffer;
    }

    public Buffer getBuffer(String id) {
        for (Buffer buffer : buffers) {
            if (buffer.getId().equals(id)) {
                return buffer;
            }
        }
        return null;
    }

    public List<Buffer> getAllBuffers() {
        return buffers;
    }

    public void removeBuffer(String id) {
        Buffer buffer = getBuffer(id);
        if (buffer != null) {
            buffers.remove(buffer);
        }
    }

    public int getBufferCount() {
        return buffers.size();
    }
}