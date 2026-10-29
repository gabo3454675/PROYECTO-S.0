package avilaos.kernel;

import avilaos.core.hardware.Computer;
import avilaos.core.hardware.CPU;
import avilaos.kernel.scheduler.Scheduler;
import avilaos.kernel.scheduler.FCFSFPolicy;
import avilaos.kernel.scheduler.RoundRobinPolicy;
import avilaos.kernel.scheduler.EDFPolicy;
import avilaos.kernel.scheduler.PriorityPreemptivePolicy;
import avilaos.model.interfaces.SchedulingPolicy;
import avilaos.model.interfaces.StateChangeListener;
import avilaos.kernel.memory.MemoryManager;
import avilaos.kernel.sync.SyncManager;
import avilaos.kernel.process.ProcessManager;
import avilaos.model.pcb.PCB;
import avilaos.model.enumeration.ProcessState;
import avilaos.model.enumeration.SchedulingPolicyType;
import avilaos.model.enumeration.BlockReason;
import avilaos.model.enumeration.ProcessType;
import avilaos.util.datastructures.Queue;
import avilaos.util.datastructures.LinkedQueue;
import avilaos.util.datastructures.List;
import avilaos.util.datastructures.LinkedList;

public class Kernel {
    private final Computer computer;
    private final Scheduler scheduler;
    private final MemoryManager memoryManager;
    private final SyncManager syncManager;
    private final ProcessManager processManager;
    private final CPU cpu;

    private final Queue<PCB> terminatedQueue;
    private final List<Queue<PCB>> blockedQueues;
    private final List<StateChangeListener> stateListeners;

    private long currentCycle;
    private long cyclesExecuted;
    private long busyCycles;
    private int quantum;

    public Kernel(Computer computer, SchedulingPolicyType initialPolicy, int quantum) {
        this(computer, initialPolicy, quantum, null);
    }

    public Kernel(Computer computer, SchedulingPolicyType initialPolicy, int quantum,
                  SyncManager sharedSyncManager) {
        this.computer = computer;
        this.cpu = computer.getCpu();
        this.memoryManager = new MemoryManager(computer.getRam());
        this.syncManager = sharedSyncManager != null
                ? sharedSyncManager
                : new SyncManager(this::onProcessUnblocked);
        this.processManager = new ProcessManager();

        this.terminatedQueue = new LinkedQueue<>();
        this.blockedQueues = new LinkedList<>();
        this.stateListeners = new LinkedList<>();

        for (int i = 0; i < BlockReason.values().length; i++) {
            blockedQueues.add(new LinkedQueue<>());
        }

        this.quantum = quantum;
        this.scheduler = new Scheduler(createPolicy(initialPolicy, quantum));
        this.currentCycle = 0;
        this.cyclesExecuted = 0;
        this.busyCycles = 0;
    }

    private avilaos.model.interfaces.SchedulingPolicy createPolicy(SchedulingPolicyType type, int quantum) {
        return switch (type) {
            case FCFS -> new FCFSFPolicy();
            case ROUND_ROBIN -> new RoundRobinPolicy(quantum);
            case EDF -> new EDFPolicy();
            case PRIORITY_PREEMPTIVE -> new PriorityPreemptivePolicy();
        };
    }

    public void executeCycle(long currentCycle) {
        this.currentCycle = currentCycle;
        cpu.resetCycleCounter();

        handleDeadlines();
        memoryManager.checkNewQueue(scheduler);
        checkPreemption();

        if (cpu.isIdle()) {
            PCB next = scheduler.selectNext();
            if (next != null) {
                cpu.contextSwitch(next);
            }
        }

        if (!cpu.isIdle()) {
            cpu.executeCycle();
            PCB running = cpu.getCurrentProcess();

            if (running != null) {
                handleRunningProcess(running);
            }
        }

        handleBlockedProcesses();
        cyclesExecuted++;
        if (cpu.getInstructionsExecutedThisCycle() > 0) {
            busyCycles++;
        }
    }

    private void checkPreemption() {
        if (cpu.isIdle() || scheduler.isReadyEmpty()) {
            return;
        }

        PCB running = cpu.getCurrentProcess();
        SchedulingPolicy policy = scheduler.getPolicy();

        if (policy instanceof PriorityPreemptivePolicy ppp) {
            PCB bestCandidate = policy.selectNext(scheduler.getReadyQueue());
            if (bestCandidate != null && ppp.shouldPreempt(running, bestCandidate)) {
                running.setState(ProcessState.READY);
                scheduler.addToReady(running);
                cpu.contextSwitch(null);
            }
        } else if (policy instanceof EDFPolicy) {
            PCB bestCandidate = policy.selectNext(scheduler.getReadyQueue());
            if (bestCandidate != null && running.getDeadline() > 0 &&
                bestCandidate.getDeadline() > 0 && bestCandidate.getDeadline() < running.getDeadline()) {
                running.setState(ProcessState.READY);
                scheduler.addToReady(running);
                cpu.contextSwitch(null);
            }
        }
    }

    private void handleDeadlines() {
        for (int i = 0; i < blockedQueues.size(); i++) {
            Queue<PCB> queue = blockedQueues.get(i);
            int size = queue.size();
            for (int j = 0; j < size; j++) {
                PCB pcb = queue.dequeue();
                if (pcb.isDeadlineReached(currentCycle)) {
                    terminateProcess(pcb);
                } else {
                    queue.enqueue(pcb);
                }
            }
        }

        Queue<PCB> readyQueue = scheduler.getReadyQueue();
        int readySize = readyQueue.size();
        for (int i = 0; i < readySize; i++) {
            PCB pcb = readyQueue.dequeue();
            if (pcb.isDeadlineReached(currentCycle)) {
                terminateProcess(pcb);
            } else {
                readyQueue.enqueue(pcb);
            }
        }

        Queue<PCB> newQueue = memoryManager.getNewQueue();
        int newSize = newQueue.size();
        for (int i = 0; i < newSize; i++) {
            PCB pcb = newQueue.dequeue();
            if (pcb.isDeadlineReached(currentCycle)) {
                terminateProcess(pcb);
            } else {
                newQueue.enqueue(pcb);
            }
        }
    }

    private void handleRunningProcess(PCB pcb) {
        if (pcb.getState() == ProcessState.RUNNING && pcb.getRemainingTime() > 0) {
            runSynchronizationOperation(pcb);
        }

        if (pcb.getState() == ProcessState.TERMINATED) {
            cpu.contextSwitch(null);
            terminateProcess(pcb);
            return;
        }

        if (pcb.isDeadlineReached(currentCycle)) {
            cpu.contextSwitch(null);
            terminateProcess(pcb);
            return;
        }

        if (pcb.getState() == ProcessState.BLOCKED) {
            cpu.contextSwitch(null);
            addToBlockedQueue(pcb);
            return;
        }

        applyRoundRobinPreemption(pcb);
    }

    private void runSynchronizationOperation(PCB pcb) {
        if (pcb.getType() != ProcessType.PRODUCER && pcb.getType() != ProcessType.CONSUMER) return;
        if (pcb.getBufferId() == null || pcb.getProduceConsumeInterval() <= 0
                || pcb.getElementsRequired() <= 0) {
            throw new IllegalStateException("El proceso " + pcb.getName()
                    + " requiere buffer, intervalo y cantidad de elementos válidos.");
        }
        if (pcb.getElementsProcessed() >= pcb.getElementsRequired()
                || pcb.getProgramCounter() % pcb.getProduceConsumeInterval() != 0) {
            return;
        }

        avilaos.kernel.sync.Buffer buffer = syncManager.getBuffer(pcb.getBufferId());
        if (buffer == null) {
            throw new IllegalStateException("No existe el buffer " + pcb.getBufferId()
                    + " del proceso " + pcb.getName() + ".");
        }

        boolean completed;
        try {
            if (pcb.getType() == ProcessType.PRODUCER) {
                completed = buffer.put(pcb, pcb.getName() + "-" + (pcb.getElementsProcessed() + 1));
            } else {
                completed = buffer.get(pcb) != null;
            }
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Se interrumpió la sincronización del proceso " + pcb.getName(), ex);
        }

        if (!completed || pcb.getState() == ProcessState.BLOCKED) return;
        pcb.incrementElementsProcessed();
        if (pcb.getElementsProcessed() >= pcb.getElementsRequired()) {
            pcb.setRemainingTime(0);
            pcb.setState(ProcessState.TERMINATED);
        }
    }

    private void applyRoundRobinPreemption(PCB pcb) {
        SchedulingPolicy policy = scheduler.getPolicy();
        if (!(policy instanceof RoundRobinPolicy rr)) {
            return;
        }
        pcb.incrementQuantumUsed();
        if (pcb.getQuantumUsed() >= rr.getQuantum()) {
            pcb.setState(ProcessState.READY);
            pcb.resetQuantumUsed();
            scheduler.addToReady(pcb);
            cpu.contextSwitch(null);
        }
    }

    private void handleBlockedProcesses() {
        for (int i = 0; i < blockedQueues.size(); i++) {
            Queue<PCB> queue = blockedQueues.get(i);
            int size = queue.size();
            for (int j = 0; j < size; j++) {
                PCB pcb = queue.dequeue();
                if (shouldUnblock(pcb, BlockReason.values()[i])) {
                    pcb.setState(ProcessState.READY);
                    pcb.setBlockReason(null);
                    scheduler.addToReady(pcb);
                } else {
                    queue.enqueue(pcb);
                }
            }
        }
    }

    private boolean shouldUnblock(PCB pcb, BlockReason reason) {
        return switch (reason) {
            case NETWORK_LATENCY -> {
                pcb.decrementNetworkLatency();
                yield pcb.getNetworkLatencyRemaining() <= 0;
            }
            case IO_WAIT -> false;
            case SEMAPHORE_WAIT -> false;
            case SEMAPHORE_FULL -> false;
            case SEMAPHORE_EMPTY -> false;
            case MEMORY_WAIT -> memoryManager.getRam().canAllocate(pcb.getMemoryRequired());
        };
    }

    public void addStateChangeListener(StateChangeListener listener) {
        if (listener != null && !stateListeners.contains(listener)) {
            stateListeners.add(listener);
        }
    }

    public void removeStateChangeListener(StateChangeListener listener) {
        stateListeners.remove(listener);
    }

    private void notifyProcessCreated(PCB pcb) {
        for (int i = 0; i < stateListeners.size(); i++) {
            stateListeners.get(i).onProcessCreated(pcb);
        }
    }

    private void notifyProcessTerminated(PCB pcb) {
        for (int i = 0; i < stateListeners.size(); i++) {
            stateListeners.get(i).onProcessTerminated(pcb);
        }
    }

    public void submitProcess(PCB pcb) {
        notifyProcessCreated(pcb);
        if (memoryManager.admitProcess(pcb)) {
            scheduler.addToReady(pcb);
        }
    }

    public void addToNewQueue(PCB pcb) {
        notifyProcessCreated(pcb);
        memoryManager.addToNewQueue(pcb);
    }

    public void addToReadyQueue(PCB pcb) {
        scheduler.addToReady(pcb);
    }

    public void addToBlockedQueue(PCB pcb) {
        BlockReason reason = pcb.getBlockReason();
        if (reason != null) {
            int index = reason.ordinal();
            if (index < blockedQueues.size()) {
                blockedQueues.get(index).enqueue(pcb);
            }
        }
    }

    public void unblockProcess(PCB pcb) {
        for (int i = 0; i < blockedQueues.size(); i++) {
            blockedQueues.get(i).remove(pcb);
        }
        if (pcb.getState() == ProcessState.TERMINATED) return;
        pcb.setState(ProcessState.READY);
        pcb.setBlockReason(null);
        boolean alreadyReady = false;
        for (PCB ready : scheduler.getReadyQueue()) {
            if (ready == pcb) {
                alreadyReady = true;
                break;
            }
        }
        if (!alreadyReady) {
            scheduler.addToReady(pcb);
        }
    }

    public void terminateProcess(PCB pcb) {
        memoryManager.releaseProcess(pcb);
        terminatedQueue.enqueue(pcb);
        notifyProcessTerminated(pcb);
    }

    public void changeSchedulingPolicy(SchedulingPolicyType type, int quantum) {
        avilaos.model.interfaces.SchedulingPolicy newPolicy = createPolicy(type, quantum);
        scheduler.setPolicy(newPolicy);
        this.quantum = quantum;
    }

    public Computer getComputer() {
        return computer;
    }

    public Scheduler getScheduler() {
        return scheduler;
    }

    public MemoryManager getMemoryManager() {
        return memoryManager;
    }

    public SyncManager getSyncManager() {
        return syncManager;
    }

    public ProcessManager getProcessManager() {
        return processManager;
    }

    public Queue<PCB> getNewQueue() {
        return memoryManager.getNewQueue();
    }

    public Queue<PCB> getReadyQueue() {
        return scheduler.getReadyQueue();
    }

    public Queue<PCB> getTerminatedQueue() {
        return terminatedQueue;
    }

    public Queue<PCB> getBlockedQueue(BlockReason reason) {
        int index = reason.ordinal();
        return index < blockedQueues.size() ? blockedQueues.get(index) : new LinkedQueue<>();
    }

    public List<Queue<PCB>> getAllBlockedQueues() {
        return blockedQueues;
    }

    public long getCurrentCycle() {
        return currentCycle;
    }

    public int getQuantum() {
        return quantum;
    }

    public double getCpuUtilization() {
        return cyclesExecuted == 0 ? 0.0 : (double) busyCycles * 100.0 / cyclesExecuted;
    }

    public double getThroughput() {
        return cyclesExecuted == 0 ? 0.0 : (double) terminatedQueue.size() / cyclesExecuted;
    }

    public long getCyclesExecuted() {
        return cyclesExecuted;
    }

    public void setNetworkLatencyCycles(int cycles) {
        syncManager.setNetworkLatencyCycles(cycles);
    }

    private void onProcessUnblocked(PCB pcb) {
        unblockProcess(pcb);
    }
}