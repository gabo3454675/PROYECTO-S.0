package avilaos.model.dto;

import avilaos.model.enumeration.SchedulingPolicyType;

public record ComputerConfig(
    int id,
    long ramSizeMB,
    SchedulingPolicyType initialPolicy,
    int quantum
) {
    public ComputerConfig {
        if (id < 0) throw new IllegalArgumentException("ID >= 0");
        if (ramSizeMB <= 0) throw new IllegalArgumentException("RAM > 0");
        if (initialPolicy == null) throw new IllegalArgumentException("Policy requerida");
        if (quantum <= 0) throw new IllegalArgumentException("Quantum > 0");
    }
}