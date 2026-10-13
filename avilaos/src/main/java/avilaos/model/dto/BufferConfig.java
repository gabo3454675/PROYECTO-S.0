package avilaos.model.dto;

public record BufferConfig(
    String id,
    int capacity,
    int hostComputerId
) {
    public BufferConfig {
        if (id == null || id.isBlank()) throw new IllegalArgumentException("ID requerido");
        if (capacity <= 0) throw new IllegalArgumentException("Capacidad > 0");
        if (hostComputerId < 0) throw new IllegalArgumentException("Host computer ID >= 0");
    }
}