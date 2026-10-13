package avilaos.model.enumeration;

public enum ProcessState {
    NEW("Nuevo"),
    READY("Listo"),
    RUNNING("Ejecución"),
    BLOCKED("Bloqueado"),
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
}