package avilaos.kernel.disk;

import avilaos.model.disk.DiskRequest;
import avilaos.model.disk.DiskSelection;
import avilaos.model.enumeration.DiskPolicyType;
import avilaos.model.enumeration.HeadDirection;
import avilaos.model.enumeration.SeekMode;
import avilaos.model.interfaces.DiskSchedulingPolicy;
import avilaos.util.datastructures.List;

/**
 * C-SCAN: el cabezal atiende solo en un sentido; al llegar al extremo
 * salta al extremo opuesto (el salto no cuenta como desplazamiento) y
 * vuelve a recorrer en el mismo sentido.
 */
public class CSCANDiskPolicy implements DiskSchedulingPolicy {

    @Override
    public DiskSelection selectNext(List<DiskRequest> pending, int headPosition,
                                    HeadDirection direction, int totalBlocks) {
        DiskRequest ahead = DiskPolicySupport.nearestAhead(pending, headPosition, direction);
        if (ahead != null) {
            return new DiskSelection(ahead, SeekMode.DIRECT, direction);
        }
        // Tras el salto, el cabezal empieza desde el extremo opuesto:
        // la primera solicitud es la más alejada en el sentido contrario.
        DiskRequest farthest = DiskPolicySupport.farthestBehind(pending, headPosition, direction);
        return new DiskSelection(farthest, SeekMode.SWEEP_AND_WRAP, direction);
    }

    @Override
    public DiskPolicyType getType() {
        return DiskPolicyType.C_SCAN;
    }
}
