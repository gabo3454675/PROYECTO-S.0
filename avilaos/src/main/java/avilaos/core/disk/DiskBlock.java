package avilaos.core.disk;

import avilaos.model.enumeration.BlockUsage;

/**
 * Bloque del disco simulado. Su índice es también su posición física,
 * y en la asignación encadenada guarda el índice del siguiente bloque
 * del mismo archivo (o {@link #NO_NEXT} si es el último).
 */
public class DiskBlock {

    public static final int NO_NEXT = -1;

    private final int index;
    private BlockUsage usage;
    private String ownerId;
    private int next;

    DiskBlock(int index) {
        this.index = index;
        clear();
    }

    /** Copia defensiva para que la interfaz observe sin modificar el disco. */
    DiskBlock(DiskBlock other) {
        this.index = other.index;
        this.usage = other.usage;
        this.ownerId = other.ownerId;
        this.next = other.next;
    }

    void assign(BlockUsage usage, String ownerId) {
        this.usage = usage;
        this.ownerId = ownerId;
        this.next = NO_NEXT;
    }

    void setNext(int next) {
        this.next = next;
    }

    void clear() {
        this.usage = BlockUsage.FREE;
        this.ownerId = null;
        this.next = NO_NEXT;
    }

    public int getIndex() {
        return index;
    }

    public BlockUsage getUsage() {
        return usage;
    }

    /** Ruta del archivo o identificador del proceso suspendido que ocupa el bloque. */
    public String getOwnerId() {
        return ownerId;
    }

    public int getNext() {
        return next;
    }

    public boolean isFree() {
        return usage == BlockUsage.FREE;
    }

    @Override
    public String toString() {
        return "Bloque " + index + " [" + usage.getDisplayName()
                + (ownerId != null ? ": " + ownerId : "")
                + (next != NO_NEXT ? " -> " + next : "") + "]";
    }
}
