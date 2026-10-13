package avilaos.model.enumeration;

public enum ExecutionMode {
    USER("Usuario"),
    KERNEL("Sistema Operativo");

    private final String displayName;

    ExecutionMode(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}