package avilaos.model.enumeration;

public enum ProcessType {
    CPU_BOUND("CPU Bound"),
    IO_BOUND("I/O Bound"),
    PRODUCER("Productor"),
    CONSUMER("Consumidor");

    private final String displayName;

    ProcessType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public boolean isProducerConsumer() {
        return this == PRODUCER || this == CONSUMER;
    }

    public boolean requiresBuffer() {
        return isProducerConsumer();
    }
}