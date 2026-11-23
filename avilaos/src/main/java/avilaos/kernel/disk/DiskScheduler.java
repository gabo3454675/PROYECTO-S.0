package avilaos.kernel.disk;

import avilaos.model.disk.DiskRequest;
import avilaos.model.disk.DiskSelection;
import avilaos.model.enumeration.DiskPolicyType;
import avilaos.model.enumeration.HeadDirection;
import avilaos.model.interfaces.DiskSchedulingPolicy;
import avilaos.util.datastructures.LinkedList;
import avilaos.util.datastructures.List;

import java.util.concurrent.Semaphore;

/**
 * Planificador de disco (parte del sistema operativo). Mantiene la cola
 * única de E/S y delega en la política activa el orden de atención.
 *
 * <p>La cola la comparten el kernel (que encola), el disco (que retira en
 * cada ciclo de reloj) y la interfaz (que la observa), por eso se protege
 * con un semáforo binario. La política puede cambiarse en cualquier
 * momento, incluso con la simulación en marcha.</p>
 */
public class DiskScheduler {

    private final List<DiskRequest> pending = new LinkedList<>();
    private final Semaphore mutex = new Semaphore(1, true);
    private volatile DiskSchedulingPolicy policy;

    public DiskScheduler(DiskPolicyType initialPolicy) {
        setPolicy(initialPolicy);
    }

    public DiskScheduler(DiskSchedulingPolicy initialPolicy) {
        setPolicy(initialPolicy);
    }

    /** Encola una solicitud de E/S al final (orden de llegada). */
    public void submit(DiskRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("La solicitud no puede ser nula");
        }
        mutex.acquireUninterruptibly();
        try {
            pending.add(request);
        } finally {
            mutex.release();
        }
    }

    /**
     * Pide a la política activa la próxima solicitud y la retira de la cola.
     *
     * @return la decisión tomada, o {@code null} si la cola está vacía
     */
    public DiskSelection dispatch(int headPosition, HeadDirection direction, int totalBlocks) {
        mutex.acquireUninterruptibly();
        try {
            if (pending.isEmpty()) {
                return null;
            }
            DiskSelection selection = policy.selectNext(pending, headPosition, direction, totalBlocks);
            pending.remove(selection.request());
            return selection;
        } finally {
            mutex.release();
        }
    }

    /** Retira una solicitud sin atenderla (p. ej. si su proceso termina). */
    public boolean cancel(DiskRequest request) {
        mutex.acquireUninterruptibly();
        try {
            return pending.remove(request);
        } finally {
            mutex.release();
        }
    }

    public void setPolicy(DiskPolicyType type) {
        setPolicy(DiskPolicyFactory.create(type));
    }

    public void setPolicy(DiskSchedulingPolicy newPolicy) {
        if (newPolicy == null) {
            throw new IllegalArgumentException("La política de disco no puede ser nula");
        }
        this.policy = newPolicy;
    }

    public DiskSchedulingPolicy getPolicy() {
        return policy;
    }

    public DiskPolicyType getPolicyType() {
        return policy.getType();
    }

    /** Copia de la cola en orden de llegada, para que la interfaz la muestre. */
    public DiskRequest[] snapshotQueue() {
        mutex.acquireUninterruptibly();
        try {
            return pending.toArray(new DiskRequest[0]);
        } finally {
            mutex.release();
        }
    }

    public int getPendingCount() {
        mutex.acquireUninterruptibly();
        try {
            return pending.size();
        } finally {
            mutex.release();
        }
    }

    public boolean hasPending() {
        return getPendingCount() > 0;
    }
}
