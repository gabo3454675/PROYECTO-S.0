package avilaos.model.enumeration;

public enum BlockReason {
    SEMAPHORE_WAIT("Espera semáforo"),
    SEMAPHORE_FULL("Buffer lleno"),
    SEMAPHORE_EMPTY("Buffer vacío"),
    IO_WAIT("Espera I/O"),
    NETWORK_LATENCY("Latencia de red"),
    MEMORY_WAIT("Espera memoria");

    private final String displayName;

    BlockReason(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public boolean isSemaphoreRelated() {
        return this == SEMAPHORE_WAIT || this == SEMAPHORE_FULL || this == SEMAPHORE_EMPTY;
    }
}