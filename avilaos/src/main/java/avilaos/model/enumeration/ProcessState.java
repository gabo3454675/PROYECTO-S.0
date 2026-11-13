package avilaos.model.enumeration;

/**
 * Modelo de siete estados (Stallings, cap. 3): agrega los estados
 * suspendidos para procesos cuya imagen fue llevada al área de swap.
 */
public enum ProcessState {
    NEW("Nuevo"),
    READY("Listo"),
    RUNNING("Ejecución"),
    BLOCKED("Bloqueado"),
    READY_SUSPENDED("Listo/Suspendido"),
    BLOCKED_SUSPENDED("Bloqueado/Suspendido"),
    TERMINATED("Terminado");

    private final String displayName;

    ProcessState(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public boolean isTerminal() {
        return this == TERMINATED;
    }

    public boolean isActive() {
        return this == RUNNING || this == READY;
    }

    /** true si la imagen del proceso está en el área de intercambio, no en RAM. */
    public boolean isSuspended() {
        return this == READY_SUSPENDED || this == BLOCKED_SUSPENDED;
    }
}