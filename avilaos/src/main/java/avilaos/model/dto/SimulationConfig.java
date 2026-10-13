package avilaos.model.dto;

public record SimulationConfig(
    int cycleDurationMs,
    int networkLatencyCycles,
    ComputerConfig[] computers,
    ProcessConfig[] processes,
    BufferConfig[] buffers
) {
    public SimulationConfig {
        if (cycleDurationMs <= 0) throw new IllegalArgumentException("Cycle duration > 0");
        if (networkLatencyCycles < 0) throw new IllegalArgumentException("Latencia >= 0");
        if (computers == null || computers.length == 0) throw new IllegalArgumentException("Al menos 1 computador");
        if (processes == null) throw new IllegalArgumentException("Lista procesos no null");
        if (buffers == null) throw new IllegalArgumentException("Lista buffers no null");
    }
}
