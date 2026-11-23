package avilaos.model.disk;

import avilaos.model.enumeration.IOOperationType;

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

    @Override
    public String toString() {
        return processName + " " + operation.getDisplayName() + " @" + targetBlock
                + (targetPath != null ? " (" + targetPath + ")" : "");
    }
}
