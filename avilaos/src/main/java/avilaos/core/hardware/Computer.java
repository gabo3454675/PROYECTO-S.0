package avilaos.core.hardware;

import avilaos.kernel.Kernel;
import avilaos.core.clock.GlobalClock;
import avilaos.model.interfaces.ClockListener;
import avilaos.model.enumeration.SchedulingPolicyType;
import avilaos.kernel.scheduler.FCFSFPolicy;
import avilaos.kernel.sync.SyncManager;
import avilaos.core.filesystem.FileSystemManager;
import avilaos.kernel.disk.DiskScheduler;
import avilaos.model.enumeration.HeadDirection;

public class Computer implements ClockListener {
    private final int id;
    private final CPU cpu;
    private final RAM ram;
    private final Kernel kernel;
    private final GlobalClock clock;
    private Disk disk;

    public Computer(int id, long ramSizeMB, SchedulingPolicyType initialPolicy, int quantum) {
        this(id, ramSizeMB, initialPolicy, quantum, null);
    }

    public Computer(int id, long ramSizeMB, SchedulingPolicyType initialPolicy, int quantum,
                    SyncManager sharedSyncManager) {
        this.id = id;
        this.ram = new RAM(ramSizeMB);
        this.cpu = new CPU(id);
        this.kernel = new Kernel(this, initialPolicy, quantum, sharedSyncManager);
        this.clock = GlobalClock.getInstance();
        this.clock.addListener(this);
    }

    @Override
    public void onTick(long cycle) {
        executeCycle(cycle);
    }

    public void executeCycle(long currentCycle) {
        kernel.executeCycle(currentCycle);
        if (disk != null) disk.onTick(currentCycle);
    }

    public void attachDisk(DiskScheduler scheduler, FileSystemManager fileSystem,
                           int initialHead, HeadDirection direction, int positionsPerCycle) {
        if (disk != null) throw new IllegalStateException("El computador ya tiene un disco");
        Disk newDisk = new Disk(scheduler, fileSystem, kernel, initialHead, direction, positionsPerCycle);
        kernel.attachIOSubsystem(scheduler, fileSystem);
        disk = newDisk;
    }

    public Disk getDisk() { return disk; }

    public CPU getCpu() {
        return cpu;
    }

    public RAM getRam() {
        return ram;
    }

    public Kernel getKernel() {
        return kernel;
    }

    public int getId() {
        return id;
    }

    public GlobalClock getClock() {
        return clock;
    }

    public void shutdown() {
        clock.removeListener(this);
    }
}
