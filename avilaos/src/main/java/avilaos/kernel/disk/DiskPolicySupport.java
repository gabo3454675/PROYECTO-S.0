package avilaos.kernel.disk;

import avilaos.model.disk.DiskRequest;
import avilaos.model.enumeration.HeadDirection;
import avilaos.util.datastructures.List;

/**
 * Búsquedas compartidas por las políticas de barrido (SCAN, C-SCAN y
 * sus variantes LOOK). Los empates se resuelven por orden de llegada.
 */
final class DiskPolicySupport {

    private DiskPolicySupport() {
    }

    /**
     * Solicitud más cercana en el sentido {@code direction}, incluyendo la
     * posición actual del cabezal. {@code null} si no hay ninguna.
     */
    static DiskRequest nearestAhead(List<DiskRequest> pending, int head, HeadDirection direction) {
        DiskRequest best = null;
        int bestDistance = Integer.MAX_VALUE;
        for (DiskRequest request : pending) {
            int distance = (request.getTargetBlock() - head) * direction.getStep();
            if (distance >= 0 && distance < bestDistance) {
                best = request;
                bestDistance = distance;
            }
        }
        return best;
    }

    /**
     * Solicitud más lejana en sentido contrario a {@code direction}
     * (la primera que se encuentra después del salto de C-SCAN).
     */
    static DiskRequest farthestBehind(List<DiskRequest> pending, int head, HeadDirection direction) {
        DiskRequest best = null;
        int bestDistance = Integer.MIN_VALUE;
        for (DiskRequest request : pending) {
            int distance = (head - request.getTargetBlock()) * direction.getStep();
            if (distance > bestDistance) {
                best = request;
                bestDistance = distance;
            }
        }
        return best;
    }
}
