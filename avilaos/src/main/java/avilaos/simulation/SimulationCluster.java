package avilaos.simulation;

import avilaos.core.clock.GlobalClock;
import avilaos.core.hardware.Computer;
import avilaos.kernel.Kernel;
import avilaos.kernel.sync.SyncManager;
import avilaos.metrics.MetricsCollector;
import avilaos.metrics.MetricsSnapshot;
import avilaos.model.dto.ComputerConfig;
import avilaos.model.dto.SimulationConfig;
import avilaos.model.enumeration.SchedulingPolicyType;
import avilaos.model.enumeration.ProcessType;
import avilaos.model.pcb.PCB;
import avilaos.util.datastructures.LinkedList;
import avilaos.util.datastructures.List;
import avilaos.util.datastructures.LinkedQueue;
import avilaos.util.datastructures.Queue;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.Semaphore;

public final class SimulationCluster {
    private final List<Computer> computers = new LinkedList<>();
    private final SyncManager syncManager = new SyncManager(this::onProcessUnblocked);
    private final Queue<PCB> pendingUnblocks = new LinkedQueue<>();
    private final Semaphore pendingUnblocksLock = new Semaphore(1, true);
    private final Semaphore cycleLifecycleLock = new Semaphore(1, true);
    private long currentCycle;
    private volatile boolean advancingCycle;

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
        if (computers.isEmpty()) {
            return;
        }

        cycleLifecycleLock.acquireUninterruptibly();
        advancingCycle = true;
        cycleLifecycleLock.release();

        ExecutorService executor = Executors.newFixedThreadPool(computers.size());
        List<Future<?>> tasks = new LinkedList<>();
        // Preserve node order while each kernel runs on a worker thread.
        Semaphore[] turnPermits = new Semaphore[computers.size() + 1];
        for (int i = 0; i < turnPermits.length; i++) {
            turnPermits[i] = new Semaphore(i == 0 ? 1 : 0, true);
        }
        RuntimeException failure = null;
        boolean interrupted = false;
        try {
            long cycle = currentCycle;
            for (int i = 0; i < computers.size(); i++) {
                Computer computer = computers.get(i);
                int turn = i;
                tasks.add(executor.submit(() -> {
                    turnPermits[turn].acquireUninterruptibly();
                    try {
                        computer.executeCycle(cycle);
                    } finally {
                        turnPermits[turn + 1].release();
                    }
                }));
            }
            for (Future<?> task : tasks) {
                boolean complete = false;
                while (!complete) {
                    try {
                        task.get();
                        complete = true;
                    } catch (InterruptedException ex) {
                        interrupted = true;
                    } catch (ExecutionException ex) {
                        if (failure == null) {
                            failure = new IllegalStateException(
                                    "Falló la ejecución de un computador durante el ciclo " + cycle + ".",
                                    ex.getCause());
                        }
                        complete = true;
                    }
                }
            }
        } finally {
            executor.shutdown();
            cycleLifecycleLock.acquireUninterruptibly();
            advancingCycle = false;
            cycleLifecycleLock.release();
            processPendingUnblocks();
        }
        if (interrupted) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Se interrumpió la espera del ciclo de simulación.");
        }
        if (failure != null) {
            throw failure;
        }
    }

    public void shutdown() {
        for (Computer computer : computers) {
            computer.shutdown();
        }
    }

    public MetricsSnapshot getMetricsSnapshot() {
        double weightedCpuUtilization = 0;
        long observedCpuCycles = 0;
        long completedProcesses = 0;
        long terminations = 0;
        long blockedProcesses = 0;
        long memoryUsed = 0;
        long memoryTotal = 0;
        double responseCycles = 0;
        long responseSamples = 0;
        double semaphoreBlockedCycles = 0;
        long semaphoreWaitEpisodes = 0;
        int processCount = 0;
        double totalProcessCpuCycles = 0;
        double totalProcessCpuCyclesSquared = 0;
        long producedElements = 0;
        long consumedElements = 0;

        for (Computer computer : computers) {
            Kernel kernel = computer.getKernel();
            MetricsCollector collector = kernel.getMetricsCollector();
            long cycles = kernel.getCyclesExecuted();
            observedCpuCycles += cycles;
            weightedCpuUtilization += kernel.getCpuUtilization() * cycles;
            completedProcesses += collector.getSuccessfulCompletions();
            terminations += collector.getTerminations();
            responseCycles += collector.getAverageResponseTime() * collector.getResponseSamples();
            responseSamples += collector.getResponseSamples();
            semaphoreBlockedCycles += collector.getAverageSemaphoreBlockedCycles()
                    * collector.getSemaphoreWaitEpisodes();
            semaphoreWaitEpisodes += collector.getSemaphoreWaitEpisodes();
            processCount += collector.getProcessCount();
            totalProcessCpuCycles += collector.getTotalCpuCycles();
            totalProcessCpuCyclesSquared += collector.getTotalCpuCyclesSquared();
            producedElements += collector.getProducedElements();
            consumedElements += collector.getConsumedElements();
            memoryUsed += computer.getRam().getUsedSize();
            memoryTotal += computer.getRam().getTotalSize();
            for (var queue : kernel.getAllBlockedQueues()) {
                blockedProcesses += queue.size();
            }
        }

        double simulatedSeconds = currentCycle * getCycleDurationMs() / 1000.0;
        double averageResponse = responseSamples == 0 ? 0 : responseCycles / responseSamples;
        double averageSemaphoreWait = semaphoreWaitEpisodes == 0 ? 0
                : semaphoreBlockedCycles / semaphoreWaitEpisodes;
        double cpuUtilization = observedCpuCycles == 0 ? 0
                : weightedCpuUtilization / observedCpuCycles;
        double throughput = simulatedSeconds == 0 ? 0 : completedProcesses / simulatedSeconds;
        double deadlineCompliance = terminations == 0 ? 0 : (double) completedProcesses / terminations;
        double fairness = MetricsCollector.fairnessIndex(processCount, totalProcessCpuCycles,
                totalProcessCpuCyclesSquared);

        return new MetricsSnapshot(currentCycle, -1, cpuUtilization, throughput, averageResponse,
                deadlineCompliance, fairness, completedProcesses, blockedProcesses,
                memoryUsed, memoryTotal, averageSemaphoreWait, producedElements, consumedElements);
    }

    private void onProcessUnblocked(PCB process) {
        cycleLifecycleLock.acquireUninterruptibly();
        try {
            if (advancingCycle) {
                pendingUnblocksLock.acquireUninterruptibly();
                try {
                    pendingUnblocks.enqueue(process);
                } finally {
                    pendingUnblocksLock.release();
                }
            } else {
                unblockProcess(process);
            }
        } finally {
            cycleLifecycleLock.release();
        }
    }

    private void processPendingUnblocks() {
        while (true) {
            pendingUnblocksLock.acquireUninterruptibly();
            PCB process;
            try {
                process = pendingUnblocks.isEmpty() ? null : pendingUnblocks.dequeue();
            } finally {
                pendingUnblocksLock.release();
            }
            if (process == null) {
                return;
            }
            unblockProcess(process);
        }
    }

    private void unblockProcess(PCB process) {
        Kernel kernel = getKernel(process.getComputerId());
        if (kernel == null) {
            throw new IllegalStateException("No existe el computador " + process.getComputerId()
                    + " para desbloquear el proceso " + process.getName() + ".");
        }
        kernel.unblockProcess(process, currentCycle);
    }
}
