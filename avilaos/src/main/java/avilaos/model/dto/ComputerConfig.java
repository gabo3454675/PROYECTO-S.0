package avilaos.model.dto;

import avilaos.model.enumeration.SchedulingPolicyType;

public record ComputerConfig(
    int id,
    long ramSizeMB,
    SchedulingPolicyType initialPolicy,
    int quantum
) {
    private static final long MAX_RAM_MB = Long.MAX_VALUE / (1024L * 1024L);

    public ComputerConfig {
        if (id < 0) throw new IllegalArgumentException("ID >= 0");
        if (ramSizeMB <= 0) throw new IllegalArgumentException("RAM > 0");
        if (ramSizeMB > MAX_RAM_MB) throw new IllegalArgumentException("RAM excede el límite representable");
        if (initialPolicy == null) throw new IllegalArgumentException("Policy requerida");
        if (quantum <= 0) throw new IllegalArgumentException("Quantum > 0");
    }
}