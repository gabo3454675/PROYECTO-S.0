package avilaos.kernel.disk;

import avilaos.model.disk.DiskRequest;
import avilaos.model.disk.DiskSelection;
import avilaos.model.enumeration.DiskPolicyType;
import avilaos.model.enumeration.HeadDirection;
import avilaos.model.enumeration.SeekMode;
import avilaos.model.interfaces.DiskSchedulingPolicy;
import avilaos.util.datastructures.List;

/**
 * SCAN (ascensor): el cabezal atiende las solicitudes en su sentido de
 * movimiento; cuando no quedan, llega hasta el extremo del disco,
 * invierte el sentido y sigue atendiendo.
 */
public class SCANDiskPolicy implements DiskSchedulingPolicy {

    @Override
    public DiskSelection selectNext(List<DiskRequest> pending, int headPosition,
                                    HeadDirection direction, int totalBlocks) {
        DiskRequest ahead = DiskPolicySupport.nearestAhead(pending, headPosition, direction);
        if (ahead != null) {
            return new DiskSelection(ahead, SeekMode.DIRECT, direction);
        }
        HeadDirection reversed = direction.opposite();
        DiskRequest behind = DiskPolicySupport.nearestAhead(pending, headPosition, reversed);
        return new DiskSelection(behind, SeekMode.SWEEP_AND_REVERSE, reversed);
    }

    @Override
    public DiskPolicyType getType() {
        return DiskPolicyType.SCAN;
    }
}
