package avilaos.core.disk;

import avilaos.model.enumeration.BlockUsage;
import avilaos.util.datastructures.LinkedList;
import avilaos.util.datastructures.List;

import java.util.concurrent.Semaphore;

/**
 * Mapa de bloques del disco simulado con asignación encadenada:
 * cada archivo (o proceso suspendido) es una lista enlazada de bloques
 * y cada bloque guarda el índice del siguiente.
 *
 * <p>El acceso está protegido por un semáforo binario porque el hilo del
 * reloj (disco) y el hilo de la interfaz (observador) lo comparten.</p>
 */
public class DiskBlockMap {

    public static final int MIN_TOTAL_BLOCKS = 100;
    public static final int MAX_TOTAL_BLOCKS = 500;

    private final DiskBlock[] blocks;
    private final Semaphore mutex = new Semaphore(1, true);
    private int freeCount;

    public DiskBlockMap(int totalBlocks) {
        if (totalBlocks < MIN_TOTAL_BLOCKS || totalBlocks > MAX_TOTAL_BLOCKS) {
            throw new IllegalArgumentException("El disco debe tener entre " + MIN_TOTAL_BLOCKS
                    + " y " + MAX_TOTAL_BLOCKS + " bloques (recibido: " + totalBlocks + ")");
        }
        this.blocks = new DiskBlock[totalBlocks];
        for (int i = 0; i < totalBlocks; i++) {
            blocks[i] = new DiskBlock(i);
        }
        this.freeCount = totalBlocks;
    }

    /**
     * Reserva {@code count} bloques libres y los encadena.
     *
     * @param usage          tipo de ocupante (archivo o swap)
     * @param ownerId        ruta del archivo o id del proceso suspendido
     * @param count          cantidad de bloques (&gt; 0)
     * @param preferredStart bloque desde el que se empieza a buscar; se recorre
     *                       el disco circularmente tomando los bloques libres
     * @return índice del primer bloque, o {@link DiskBlock#NO_NEXT} si no hay
     *         suficientes bloques libres (en ese caso no se modifica nada)
     */
    public int allocate(BlockUsage usage, String ownerId, int count, int preferredStart) {
        if (usage == null || usage == BlockUsage.FREE) {
            throw new IllegalArgumentException("El uso del bloque debe ser FILE o SWAP");
        }
        if (count <= 0) {
            throw new IllegalArgumentException("La cantidad de bloques debe ser mayor que 0");
        }
        checkIndex(preferredStart);
        lock();
        try {
            if (count > freeCount) {
                return DiskBlock.NO_NEXT;
            }
            int first = DiskBlock.NO_NEXT;
            DiskBlock previous = null;
            int assigned = 0;
            for (int step = 0; step < blocks.length && assigned < count; step++) {
                DiskBlock candidate = blocks[(preferredStart + step) % blocks.length];
                if (!candidate.isFree()) {
                    continue;
                }
                candidate.assign(usage, ownerId);
                if (previous == null) {
                    first = candidate.getIndex();
                } else {
                    previous.setNext(candidate.getIndex());
                }
                previous = candidate;
                assigned++;
            }
            freeCount -= assigned;
            return first;
        } finally {
            mutex.release();
        }
    }

    /** Igual que {@link #allocate(BlockUsage, String, int, int)} empezando por el primer bloque libre. */
    public int allocate(BlockUsage usage, String ownerId, int count) {
        return allocate(usage, ownerId, count, 0);
    }

    /**
     * Libera toda la cadena que empieza en {@code firstBlock}.
     *
     * @return cantidad de bloques liberados
     */
    public int free(int firstBlock) {
        checkIndex(firstBlock);
        lock();
        try {
            int released = 0;
            int current = firstBlock;
            while (current != DiskBlock.NO_NEXT && !blocks[current].isFree()) {
                int next = blocks[current].getNext();
                blocks[current].clear();
                released++;
                current = next;
            }
            freeCount += released;
            return released;
        } finally {
            mutex.release();
        }
    }

    /** Actualiza la ruta asociada a todos los bloques de un archivo renombrado. */
    public void renameOwner(int firstBlock, String ownerId) {
        checkIndex(firstBlock);
        lock();
        try {
            int current = firstBlock;
            while (current != DiskBlock.NO_NEXT && !blocks[current].isFree()) {
                blocks[current].setOwnerId(ownerId);
                current = blocks[current].getNext();
            }
        } finally {
            mutex.release();
        }
    }

    /** Índices de la cadena de bloques que empieza en {@code firstBlock}, en orden. */
    public List<Integer> getChain(int firstBlock) {
        checkIndex(firstBlock);
        lock();
        try {
            List<Integer> chain = new LinkedList<>();
            int current = firstBlock;
            while (current != DiskBlock.NO_NEXT && !blocks[current].isFree()) {
                chain.add(current);
                current = blocks[current].getNext();
            }
            return chain;
        } finally {
            mutex.release();
        }
    }

    /** Copia del bloque {@code index} (solo lectura para observadores). */
    public DiskBlock getBlock(int index) {
        checkIndex(index);
        lock();
        try {
            return new DiskBlock(blocks[index]);
        } finally {
            mutex.release();
        }
    }

    /** Copia consistente de todos los bloques para dibujar el mapa en la interfaz. */
    public DiskBlock[] snapshot() {
        lock();
        try {
            DiskBlock[] copy = new DiskBlock[blocks.length];
            for (int i = 0; i < blocks.length; i++) {
                copy[i] = new DiskBlock(blocks[i]);
            }
            return copy;
        } finally {
            mutex.release();
        }
    }

    public int getTotalBlocks() {
        return blocks.length;
    }

    public int getFreeCount() {
        lock();
        try {
            return freeCount;
        } finally {
            mutex.release();
        }
    }

    public int getUsedCount() {
        return blocks.length - getFreeCount();
    }

    private void checkIndex(int index) {
        if (index < 0 || index >= blocks.length) {
            throw new IndexOutOfBoundsException("Bloque fuera de rango: " + index
                    + " (0.." + (blocks.length - 1) + ")");
        }
    }

    private void lock() {
        mutex.acquireUninterruptibly();
    }
}
