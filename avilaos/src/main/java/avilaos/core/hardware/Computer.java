package avilaos.core.hardware;

import avilaos.kernel.Kernel;
import avilaos.core.clock.GlobalClock;
import avilaos.model.interfaces.ClockListener;
import avilaos.model.enumeration.SchedulingPolicyType;
import avilaos.kernel.scheduler.FCFSFPolicy;

public class Computer implements ClockListener {
    private final int id;
    private final CPU cpu;
    private final RAM ram;
    private final Kernel kernel;
    private final GlobalClock clock;

    public Computer(int id, long ramSizeMB, SchedulingPolicyType initialPolicy, int quantum) {
        this.id = id;
        this.ram = new RAM(ramSizeMB);
        this.cpu = new CPU(id);
        this.kernel = new Kernel(this, initialPolicy, quantum);
        this.clock = GlobalClock.getInstance();
        this.clock.addListener(this);
    }

    @Override
    public void onTick(long cycle) {
        executeCycle(cycle);
    }

    public void executeCycle(long currentCycle) {
        kernel.executeCycle(currentCycle);
    }

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

    public void shutdown() {
        clock.removeListener(this);
    }
}