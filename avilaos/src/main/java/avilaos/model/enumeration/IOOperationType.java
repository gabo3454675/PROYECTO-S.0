package avilaos.model.enumeration;

/**
 * Operaciones de E/S que un proceso puede solicitar al disco.
 * CREATE/READ/UPDATE/DELETE actúan sobre el sistema de archivos;
 * SUSPEND/RESUME mueven la imagen de un proceso hacia/desde el swap.
 */
public enum IOOperationType {
    CREATE("Crear"),
    READ("Leer"),
    UPDATE("Actualizar"),
    DELETE("Eliminar"),
    SUSPEND("Suspender"),
    RESUME("Reactivar");

    private final String displayName;

    IOOperationType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    /** true para las operaciones CRUD sobre archivos. */
    public boolean isFileOperation() {
        return this == CREATE || this == READ || this == UPDATE || this == DELETE;
    }

    /** true para las operaciones del planificador a mediano plazo. */
    public boolean isSwapOperation() {
        return this == SUSPEND || this == RESUME;
    }

    /** true si la operación altera el sistema de archivos (requiere permisos de escritura). */
    public boolean modifiesFileSystem() {
        return this == CREATE || this == UPDATE || this == DELETE;
    }
}
