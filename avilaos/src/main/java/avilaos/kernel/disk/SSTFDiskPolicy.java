package avilaos.kernel.disk;

import avilaos.model.disk.DiskRequest;
import avilaos.model.disk.DiskSelection;
import avilaos.model.enumeration.DiskPolicyType;
import avilaos.model.enumeration.HeadDirection;
import avilaos.model.enumeration.SeekMode;
import avilaos.model.interfaces.DiskSchedulingPolicy;
import avilaos.util.datastructures.List;

/**
 * SSTF: atiende la solicitud más cercana al cabezal. En caso de empate
 * gana la que llegó primero.
 */
public class SSTFDiskPolicy implements DiskSchedulingPolicy {

    @Override
    public DiskSelection selectNext(List<DiskRequest> pending, int headPosition,
                                    HeadDirection direction, int totalBlocks) {
        DiskRequest best = null;
        int bestDistance = Integer.MAX_VALUE;
        for (DiskRequest request : pending) {
            int distance = Math.abs(request.getTargetBlock() - headPosition);
            if (distance < bestDistance) {
                best = request;
                bestDistance = distance;
            }
        }
        return new DiskSelection(best, SeekMode.DIRECT,
                FIFODiskPolicy.directionTowards(headPosition, best.getTargetBlock(), direction));
    }

    @Override
    public DiskPolicyType getType() {
        return DiskPolicyType.SSTF;
    }
}
