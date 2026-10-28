package avilaos.simulation;

import avilaos.core.clock.GlobalClock;
import avilaos.core.hardware.Computer;
import avilaos.kernel.Kernel;
import avilaos.kernel.sync.SyncManager;
import avilaos.model.dto.ComputerConfig;
import avilaos.model.dto.SimulationConfig;
import avilaos.model.enumeration.SchedulingPolicyType;
import avilaos.model.enumeration.ProcessType;
import avilaos.model.pcb.PCB;
import avilaos.util.datastructures.LinkedList;
import avilaos.util.datastructures.List;

public final class SimulationCluster {
    private final List<Computer> computers = new LinkedList<>();
    private final SyncManager syncManager = new SyncManager(this::onProcessUnblocked);
    private long currentCycle;

    public SimulationCluster() {
        this.currentCycle = 0;
    }

    public SimulationCluster(Computer computer) {
        this();
        if (computer == null) throw new IllegalArgumentException("El computador no puede ser null.");
        computers.add(computer);
    }

    public Computer addComputer(ComputerConfig config) {
        if (config == null) throw new IllegalArgumentException("La configuración del computador es obligatoria.");
        if (getComputer(config.id()) != null) {
            throw new IllegalArgumentException("Ya existe el computador " + config.id() + ".");
        }
        Computer computer = new Computer(config.id(), config.ramSizeMB(), config.initialPolicy(),
                config.quantum(), syncManager);
        computers.add(computer);
        return computer;
    }

    public Computer addComputer(int id, long ramSizeMB, SchedulingPolicyType policy, int quantum) {
        return addComputer(new ComputerConfig(id, ramSizeMB, policy, quantum));
    }

    public Computer getComputer(int id) {
        for (Computer computer : computers) {
            if (computer.getId() == id) return computer;
        }
        return null;
    }

    public Kernel getKernel(int computerId) {
        Computer computer = getComputer(computerId);
        return computer == null ? null : computer.getKernel();
    }

    public List<Computer> getComputers() {
        return computers;
    }

    public SyncManager getSyncManager() {
        return syncManager;
    }

    public long getCurrentCycle() {
        return currentCycle;
    }

    public int getCycleDurationMs() {
        return GlobalClock.getInstance().getCycleDurationMs();
    }

    public void setCycleDurationMs(int durationMs) {
        GlobalClock.getInstance().setCycleDurationMs(durationMs);
    }

    public int getNetworkLatencyCycles() {
        return syncManager.getNetworkLatencyCycles();
    }

    public void setNetworkLatencyCycles(int cycles) {
        syncManager.setNetworkLatencyCycles(cycles);
    }

    public void submitProcess(PCB process) {
        if (process == null) throw new IllegalArgumentException("El proceso no puede ser null.");
        Kernel kernel = getKernel(process.getComputerId());
        if (kernel == null) {
            throw new IllegalArgumentException("No existe el computador " + process.getComputerId()
                    + " asignado al proceso " + process.getName() + ".");
        }
        if (process.getType() == ProcessType.PRODUCER || process.getType() == ProcessType.CONSUMER) {
            if (process.getBufferId() == null || process.getProduceConsumeInterval() <= 0
                    || process.getElementsRequired() <= 0
                    || syncManager.getBuffer(process.getBufferId()) == null) {
                throw new IllegalArgumentException("El proceso productor/consumidor requiere un buffer existente, "
                        + "intervalo positivo y cantidad de elementos positiva.");
            }
        }
        kernel.submitProcess(process);
    }

    public avilaos.kernel.sync.Buffer createBuffer(String id, int capacity, int hostComputerId) {
        if (getComputer(hostComputerId) == null) {
            throw new IllegalArgumentException("No existe el computador host " + hostComputerId + ".");
        }
        if (syncManager.getBuffer(id) != null) {
            throw new IllegalArgumentException("Ya existe el buffer " + id + ".");
        }
        return syncManager.createBuffer(id, capacity, hostComputerId);
    }

    public void advanceCycle() {
        currentCycle++;
        for (Computer computer : computers) {
            computer.executeCycle(currentCycle);
        }
    }

    public void shutdown() {
        for (Computer computer : computers) {
            computer.shutdown();
        }
    }

    private void onProcessUnblocked(PCB process) {
        Kernel kernel = getKernel(process.getComputerId());
        if (kernel == null) {
            throw new IllegalStateException("No existe el computador " + process.getComputerId()
                    + " para desbloquear el proceso " + process.getName() + ".");
        }
        kernel.unblockProcess(process);
    }
}
