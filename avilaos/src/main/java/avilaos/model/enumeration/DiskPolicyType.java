package avilaos.model.enumeration;

/**
 * Políticas de planificación de disco disponibles.
 */
public enum DiskPolicyType {
    FIFO("FIFO (First In First Out)"),
    SSTF("SSTF (Shortest Seek Time First)"),
    SCAN("SCAN (Ascensor)"),
    C_SCAN("C-SCAN (SCAN Circular)");

    private final String displayName;

    DiskPolicyType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    /**
     * Convierte el nombre usado en los JSON ("FIFO", "SSTF", "SCAN", "C-SCAN")
     * a la constante correspondiente. Acepta guion o guion bajo.
     */
    public static DiskPolicyType fromString(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("La política de disco no puede estar vacía");
        }
        String normalized = value.trim().toUpperCase().replace('-', '_');
        try {
            return DiskPolicyType.valueOf(normalized);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Política de disco desconocida: " + value, e);
        }
    }
}
