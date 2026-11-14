package avilaos.kernel.disk;

import avilaos.model.enumeration.DiskPolicyType;
import avilaos.model.interfaces.DiskSchedulingPolicy;

/**
 * Crea la política de disco correspondiente a cada {@link DiskPolicyType}.
 * Agregar una política nueva solo toca este método y el enum, nunca
 * al {@link DiskScheduler}.
 */
public final class DiskPolicyFactory {

    private DiskPolicyFactory() {
    }

    public static DiskSchedulingPolicy create(DiskPolicyType type) {
        if (type == null) {
            throw new IllegalArgumentException("La política de disco no puede ser nula");
        }
        return switch (type) {
            case FIFO -> new FIFODiskPolicy();
            case SSTF -> new SSTFDiskPolicy();
            case SCAN -> new SCANDiskPolicy();
            case C_SCAN -> new CSCANDiskPolicy();
        };
    }
}
