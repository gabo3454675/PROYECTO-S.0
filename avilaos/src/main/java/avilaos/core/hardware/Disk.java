package avilaos.core.hardware;

import avilaos.core.filesystem.FileSystemManager;
import avilaos.core.filesystem.FileSystemNode;
import avilaos.kernel.Kernel;
import avilaos.kernel.disk.DiskScheduler;
import avilaos.model.disk.DiskRequest;
import avilaos.model.disk.DiskSelection;
import avilaos.model.enumeration.HeadDirection;
import avilaos.model.enumeration.IOOperationType;
import avilaos.model.enumeration.DiskPolicyType;
import avilaos.model.enumeration.SeekMode;
import avilaos.model.interfaces.ClockListener;
import avilaos.model.pcb.PCB;

/** Disco local: una solicitud activa y exactamente un ciclo de trabajo por tick. */
public final class Disk implements ClockListener {
    private final DiskScheduler scheduler;
    private final FileSystemManager fileSystem;
    private final Kernel kernel;
    private final int totalBlocks;
    private final int positionsPerCycle;
    private int headPosition;
    private HeadDirection direction;
    private DiskSelection active;
    private int seekTarget;
    private int stage;
    private int serviceCyclesRemaining;
    private long totalHeadMovement;
    private long completedRequests;
    private long totalQueueWaitCycles;
    private long dispatchedRequests;
    private final long[] requestsByPolicy = new long[DiskPolicyType.values().length];
    private DiskPolicyType activePolicy;

    public Disk(DiskScheduler scheduler, FileSystemManager fileSystem, Kernel kernel,
                int initialHead, HeadDirection initialDirection, int positionsPerCycle) {
        if (scheduler == null || fileSystem == null || kernel == null || initialDirection == null) {
            throw new IllegalArgumentException("El disco requiere planificador, archivos, kernel y dirección");
        }
        this.totalBlocks = fileSystem.getBlockMap().getTotalBlocks();
        if (initialHead < 0 || initialHead >= totalBlocks || positionsPerCycle <= 0) {
            throw new IllegalArgumentException("Posición o velocidad del cabezal inválida");
        }
        this.scheduler = scheduler;
        this.fileSystem = fileSystem;
        this.kernel = kernel;
        this.headPosition = initialHead;
        this.direction = initialDirection;
        this.positionsPerCycle = positionsPerCycle;
    }

    @Override
    public void onTick(long cycle) {
        if (active == null) {
            active = scheduler.dispatch(headPosition, direction, totalBlocks);
            if (active == null) return;
            DiskPolicyType selectedPolicy = scheduler.getPolicyType();
            activePolicy = selectedPolicy;
            dispatchedRequests++;
            totalQueueWaitCycles += Math.max(0, cycle - active.request().getArrivalCycle());
            kernel.notifyDiskDispatch(active.request().getProcessName(),
                    active.request().getTargetBlock(), selectedPolicy.name());
            stage = 0;
            seekTarget = active.seekMode() == SeekMode.DIRECT
                    ? active.request().getTargetBlock()
                    : (direction == HeadDirection.UP ? totalBlocks - 1 : 0);
            serviceCyclesRemaining = serviceBlocks(active.request());
        }

        boolean moved = headPosition != seekTarget;
        if (moved) {
            int distance = Math.min(positionsPerCycle, Math.abs(seekTarget - headPosition));
            headPosition += Integer.signum(seekTarget - headPosition) * distance;
            totalHeadMovement += distance;
        }
        if (headPosition != seekTarget) return;

        if (active.seekMode() != SeekMode.DIRECT && stage == 0) {
            stage = 1;
            if (active.seekMode() == SeekMode.SWEEP_AND_WRAP) {
                headPosition = direction == HeadDirection.UP ? 0 : totalBlocks - 1;
            } else {
                direction = direction.opposite();
            }
            seekTarget = active.request().getTargetBlock();
            if (headPosition != seekTarget) return;
        }
        if (headPosition != active.request().getTargetBlock()) return;

        // Un tick que mueve el cabezal no puede además leer o escribir un bloque.
        if (!moved && serviceCyclesRemaining > 0 && headPosition == seekTarget) {
            serviceCyclesRemaining--;
            if (serviceCyclesRemaining == 0) {
                DiskRequest completed = active.request();
                direction = active.directionAfter();
                active = null;
                completedRequests++;
                requestsByPolicy[activePolicy.ordinal()]++;
                kernel.getIoProcessManager().completeDiskRequest(completed, kernel);
            }
        }
    }

    private int serviceBlocks(DiskRequest request) {
        IOOperationType operation = request.getOperation();
        if (operation.isSwapOperation()) {
            PCB pcb = kernel.findProcessById(request.getProcessId());
            return pcb == null ? 1 : (int) Math.min(Integer.MAX_VALUE, Math.max(1, pcb.getMemoryRequired()));
        }
        if (operation == IOOperationType.CREATE) {
            PCB pcb = kernel.findProcessById(request.getProcessId());
            return pcb == null ? 1 : Math.max(1, pcb.getFileSizeInBlocks());
        }
        FileSystemNode node = request.getTargetPath() == null ? null : fileSystem.resolve(request.getTargetPath());
        return node == null || node.isDirectory() ? 1 : Math.max(1, node.getSizeInBlocks());
    }

    public int getHeadPosition() { return headPosition; }
    public HeadDirection getDirection() { return direction; }
    public DiskRequest getActiveRequest() { return active == null ? null : active.request(); }
    public long getTotalHeadMovement() { return totalHeadMovement; }
    public long getCompletedRequests() { return completedRequests; }
    public double getAverageQueueWaitCycles() {
        return dispatchedRequests == 0 ? 0 : (double) totalQueueWaitCycles / dispatchedRequests;
    }
    public long getRequestsByPolicy(DiskPolicyType policy) { return requestsByPolicy[policy.ordinal()]; }
    public int getPositionsPerCycle() { return positionsPerCycle; }
}
