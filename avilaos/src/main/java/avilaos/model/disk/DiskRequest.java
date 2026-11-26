package avilaos.model.disk;

import avilaos.model.enumeration.IOOperationType;
import avilaos.model.enumeration.UserMode;

/**
 * Solicitud de E/S que un proceso deja en la cola única del disco.
 * Es inmutable: el planificador de disco solo decide el orden.
 */
public final class DiskRequest {

    private static long nextId = 1;

    private final long id;
    private final long processId;
    private final String processName;
    private final IOOperationType operation;
    private final String targetPath;
    private final int targetBlock;
    private final long arrivalCycle;
    private final UserMode userMode;
    private final String activeUser;

    /**
     * @param processId    id del PCB que espera la solicitud
     * @param processName  nombre del proceso (para el log y la interfaz)
     * @param operation    operación solicitada
     * @param targetPath   ruta del archivo, o {@code null} en suspender/reactivar
     * @param targetBlock  bloque donde debe posicionarse el cabezal
     * @param arrivalCycle ciclo de reloj en que entró a la cola
     */
    public DiskRequest(long processId, String processName, IOOperationType operation,
                       String targetPath, int targetBlock, long arrivalCycle) {
        this(processId, processName, operation, targetPath, targetBlock, arrivalCycle, UserMode.ADMIN, "admin");
    }

    public DiskRequest(long processId, String processName, IOOperationType operation,
                       String targetPath, int targetBlock, long arrivalCycle,
                       UserMode userMode, String activeUser) {
        if (operation == null) {
            throw new IllegalArgumentException("La solicitud debe indicar una operación");
        }
        if (targetBlock < 0) {
            throw new IllegalArgumentException("El bloque destino no puede ser negativo");
        }
        this.id = nextRequestId();
        this.processId = processId;
        this.processName = processName;
        this.operation = operation;
        this.targetPath = targetPath;
        this.targetBlock = targetBlock;
        this.arrivalCycle = arrivalCycle;
        this.userMode = java.util.Objects.requireNonNull(userMode, "Modo requerido");
        if (activeUser == null || activeUser.isBlank()) throw new IllegalArgumentException("Usuario requerido");
        this.activeUser = activeUser;
    }

    private static synchronized long nextRequestId() {
        return nextId++;
    }

    public long getId() {
        return id;
    }

    public long getProcessId() {
        return processId;
    }

    public String getProcessName() {
        return processName;
    }

    public IOOperationType getOperation() {
        return operation;
    }

    public String getTargetPath() {
        return targetPath;
    }

    public int getTargetBlock() {
        return targetBlock;
    }

    public long getArrivalCycle() {
        return arrivalCycle;
    }

    public UserMode getUserMode() { return userMode; }
    public String getActiveUser() { return activeUser; }

    @Override
    public String toString() {
        return processName + " " + operation.getDisplayName() + " @" + targetBlock
                + (targetPath != null ? " (" + targetPath + ")" : "");
    }
}
