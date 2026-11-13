package avilaos.model.disk;

import avilaos.model.enumeration.HeadDirection;
import avilaos.model.enumeration.SeekMode;

/**
 * Decisión de una política de disco: qué solicitud atender, cómo llega
 * el cabezal hasta ella y en qué sentido queda moviéndose después.
 *
 * @param request        solicitud elegida (sigue en la cola hasta que el planificador la retire)
 * @param seekMode       recorrido del cabezal hasta el bloque destino
 * @param directionAfter sentido del cabezal al llegar al destino
 */
public record DiskSelection(DiskRequest request, SeekMode seekMode, HeadDirection directionAfter) {

    public DiskSelection {
        if (request == null || seekMode == null || directionAfter == null) {
            throw new IllegalArgumentException("La selección de disco no admite valores nulos");
        }
    }

    /**
     * Bloques que recorre el cabezal desde {@code from} hasta el destino.
     * En C-SCAN el salto de regreso al extremo opuesto no se cuenta.
     *
     * @param from        posición actual del cabezal
     * @param direction   sentido actual del cabezal (antes de la decisión)
     * @param totalBlocks tamaño del disco
     */
    public int seekDistance(int from, HeadDirection direction, int totalBlocks) {
        int target = request.getTargetBlock();
        int last = totalBlocks - 1;
        return switch (seekMode) {
            case DIRECT -> Math.abs(target - from);
            case SWEEP_AND_REVERSE -> direction == HeadDirection.UP
                    ? (last - from) + (last - target)
                    : from + target;
            case SWEEP_AND_WRAP -> direction == HeadDirection.UP
                    ? (last - from) + target
                    : from + (last - target);
        };
    }
}
