package avilaos.kernel.disk;

import avilaos.model.disk.DiskRequest;
import avilaos.model.disk.DiskSelection;
import avilaos.model.enumeration.HeadDirection;
import avilaos.model.enumeration.IOOperationType;
import avilaos.model.interfaces.DiskSchedulingPolicy;
import avilaos.util.datastructures.LinkedList;
import avilaos.util.datastructures.List;

/**
 * Ejecuta una política sobre una cola fija de bloques (sin reloj) y
 * devuelve el orden de atención y el desplazamiento total del cabezal.
 */
final class DiskPolicyRun {

    final int[] order;
    final int totalSeek;

    private DiskPolicyRun(int[] order, int totalSeek) {
        this.order = order;
        this.totalSeek = totalSeek;
    }

    static DiskPolicyRun run(DiskSchedulingPolicy policy, int head, HeadDirection direction,
                             int totalBlocks, int... blocks) {
        List<DiskRequest> pending = new LinkedList<>();
        for (int i = 0; i < blocks.length; i++) {
            pending.add(new DiskRequest(i + 1, "P" + (i + 1), IOOperationType.READ, null, blocks[i], 0));
        }
        int[] order = new int[blocks.length];
        int seek = 0;
        for (int i = 0; i < blocks.length; i++) {
            DiskSelection selection = policy.selectNext(pending, head, direction, totalBlocks);
            seek += selection.seekDistance(head, direction, totalBlocks);
            head = selection.request().getTargetBlock();
            direction = selection.directionAfter();
            pending.remove(selection.request());
            order[i] = head;
        }
        return new DiskPolicyRun(order, seek);
    }
}
