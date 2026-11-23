package avilaos.model.enumeration;

/**
 * Modo de uso de ÁvilaOS seleccionado desde la interfaz.
 */
public enum UserMode {
    ADMIN("Administrador"),
    USER("Usuario");

    private final String displayName;

    UserMode(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public boolean isAdmin() {
        return this == ADMIN;
    }
}
