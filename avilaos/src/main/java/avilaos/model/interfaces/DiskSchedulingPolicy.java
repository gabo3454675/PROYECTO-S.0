package avilaos.model.interfaces;

import avilaos.model.disk.DiskRequest;
import avilaos.model.disk.DiskSelection;
import avilaos.model.enumeration.DiskPolicyType;
import avilaos.model.enumeration.HeadDirection;
import avilaos.util.datastructures.List;

/**
 * Política de planificación de disco. El planificador de disco solo
 * depende de esta interfaz, de modo que agregar una política nueva
 * (LOOK, C-LOOK, ...) no requiere modificar su código.
 */
public interface DiskSchedulingPolicy {

    /**
     * Elige la próxima solicitud a atender. No modifica la cola.
     *
     * @param pending      solicitudes en espera, en orden de llegada (no vacía)
     * @param headPosition bloque donde está el cabezal
     * @param direction    sentido en que se mueve el cabezal
     * @param totalBlocks  tamaño del disco
     */
    DiskSelection selectNext(List<DiskRequest> pending, int headPosition,
                             HeadDirection direction, int totalBlocks);

    DiskPolicyType getType();

    default String getDescription() {
        return getType().getDisplayName();
    }
}
