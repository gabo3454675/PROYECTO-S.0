package avilaos.model.enumeration;

/**
 * Cómo llega el cabezal al bloque de la solicitud elegida.
 */
public enum SeekMode {
    /** Va directo al bloque destino (FIFO, SSTF, o SCAN sin cambio de sentido). */
    DIRECT("Directo"),
    /** Recorre hasta el extremo del disco, invierte el sentido y luego va al destino (SCAN). */
    SWEEP_AND_REVERSE("Hasta el extremo y regresa"),
    /** Recorre hasta el extremo, salta al extremo opuesto sin contar el salto y sigue (C-SCAN). */
    SWEEP_AND_WRAP("Hasta el extremo y salta al inicio");

    private final String displayName;

    SeekMode(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
