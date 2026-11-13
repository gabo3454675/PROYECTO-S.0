package avilaos.kernel.disk;

import avilaos.model.disk.DiskRequest;
import avilaos.model.disk.DiskSelection;
import avilaos.model.enumeration.DiskPolicyType;
import avilaos.model.enumeration.HeadDirection;
import avilaos.model.enumeration.SeekMode;
import avilaos.model.interfaces.DiskSchedulingPolicy;
import avilaos.util.datastructures.List;

/**
 * FIFO: atiende las solicitudes en orden de llegada, sin importar
 * dónde esté el cabezal.
 */
public class FIFODiskPolicy implements DiskSchedulingPolicy {

    @Override
    public DiskSelection selectNext(List<DiskRequest> pending, int headPosition,
                                    HeadDirection direction, int totalBlocks) {
        DiskRequest next = pending.getFirst();
        return new DiskSelection(next, SeekMode.DIRECT,
                directionTowards(headPosition, next.getTargetBlock(), direction));
    }

    @Override
    public DiskPolicyType getType() {
        return DiskPolicyType.FIFO;
    }

    /** Sentido en que se mueve el cabezal para ir de {@code from} a {@code to}. */
    static HeadDirection directionTowards(int from, int to, HeadDirection current) {
        if (to > from) return HeadDirection.UP;
        if (to < from) return HeadDirection.DOWN;
        return current;
    }
}
