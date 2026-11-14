package avilaos.model.enumeration;

/**
 * Uso de un bloque del disco simulado.
 */
public enum BlockUsage {
    FREE("Libre"),
    FILE("Archivo"),
    SWAP("Proceso suspendido");

    private final String displayName;

    BlockUsage(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
