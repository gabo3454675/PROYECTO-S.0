package avilaos.metrics;

import avilaos.model.enumeration.ProcessType;
import avilaos.model.pcb.PCB;
import avilaos.util.datastructures.AssociativeList;

public final class MetricsCollector {
    private final AssociativeList<Long, ProcessMetrics> processes = new AssociativeList<>();
    private final AssociativeList<Long, Long> semaphoreWaitStarts = new AssociativeList<>();

    private double totalResponseCycles;
    private long responseSamples;
    private double totalSemaphoreBlockedCycles;
    private long semaphoreWaitEpisodes;
    private long successfulCompletions;
    private long terminations;
    private long producedElements;
    private long consumedElements;

    public void registerProcess(PCB process, long submissionCycle) {
        if (process != null) {
            processes.putIfAbsent(process.getId(), new ProcessMetrics(submissionCycle));
        }
    }

    public void recordCpuCycle(PCB process, long cycle) {
        ProcessMetrics metrics = getOrRegister(process, cycle);
        metrics.cpuCycles++;
        if (!metrics.hasStarted) {
            metrics.hasStarted = true;
            metrics.responseCycles = Math.max(0, cycle - metrics.submissionCycle);
            totalResponseCycles += metrics.responseCycles;
            responseSamples++;
        }
    }

    public void startSemaphoreWait(PCB process, long cycle) {
        if (process != null) {
            semaphoreWaitStarts.putIfAbsent(process.getId(), cycle);
        }
    }

    public void finishSemaphoreWait(PCB process, long cycle) {
        if (process == null) {
            return;
        }
        Long startCycle = semaphoreWaitStarts.remove(process.getId());
        if (startCycle != null) {
            ProcessMetrics metrics = getOrRegister(process, cycle);
            long blockedCycles = Math.max(0, cycle - startCycle);
            metrics.semaphoreBlockedCycles += blockedCycles;
            metrics.semaphoreWaitEpisodes++;
            totalSemaphoreBlockedCycles += blockedCycles;
            semaphoreWaitEpisodes++;
        }
    }

    public void recordElementProcessed(ProcessType type) {
        if (type == ProcessType.PRODUCER) {
            producedElements++;
        } else if (type == ProcessType.CONSUMER) {
            consumedElements++;
        }
    }

    public void recordElementProcessed(PCB process) {
        if (process == null) {
            throw new IllegalArgumentException("El proceso no puede ser null.");
        }
        recordElementProcessed(process.getType());
        ProcessMetrics metrics = getOrRegister(process, 0);
        if (process.getType() == ProcessType.PRODUCER) {
            metrics.producedElements++;
        } else if (process.getType() == ProcessType.CONSUMER) {
            metrics.consumedElements++;
        }
    }

    public void recordTermination(PCB process, long cycle, boolean completed) {
        ProcessMetrics metrics = getOrRegister(process, cycle);
        finishSemaphoreWait(process, cycle);
        terminations++;
        if (completed) {
            successfulCompletions++;
        }
        metrics.terminated = true;
        metrics.completed = completed;
    }

    public boolean removeProcess(PCB process) {
        if (process == null) {
            return false;
        }
        ProcessMetrics metrics = processes.remove(process.getId());
        semaphoreWaitStarts.remove(process.getId());
        if (metrics == null) {
            return false;
        }
        if (metrics.hasStarted) {
            totalResponseCycles -= metrics.responseCycles;
            responseSamples--;
        }
        totalSemaphoreBlockedCycles -= metrics.semaphoreBlockedCycles;
        semaphoreWaitEpisodes -= metrics.semaphoreWaitEpisodes;
        terminations -= metrics.terminated ? 1 : 0;
        successfulCompletions -= metrics.completed ? 1 : 0;
        producedElements -= metrics.producedElements;
        consumedElements -= metrics.consumedElements;
        return true;
    }

    public double getAverageResponseTime() {
        return responseSamples == 0 ? 0 : totalResponseCycles / responseSamples;
    }

    public long getResponseSamples() {
        return responseSamples;
    }

    public double getFairnessIndex() {
        return fairnessIndex(getProcessCount(), getTotalCpuCycles(), getTotalCpuCyclesSquared());
    }

    public int getProcessCount() {
        return processes.size();
    }

    public double getTotalCpuCycles() {
        double total = 0;
        for (ProcessMetrics metrics : processes.values()) {
            total += metrics.cpuCycles;
        }
        return total;
    }

    public double getTotalCpuCyclesSquared() {
        double total = 0;
        for (ProcessMetrics metrics : processes.values()) {
            total += (double) metrics.cpuCycles * metrics.cpuCycles;
        }
        return total;
    }

    public static double fairnessIndex(int processCount, double totalCpuCycles,
                                       double totalCpuCyclesSquared) {
        double denominator = processCount * totalCpuCyclesSquared;
        return denominator <= 0 ? 0 : totalCpuCycles * totalCpuCycles / denominator;
    }

    public double getAverageSemaphoreBlockedCycles() {
        return semaphoreWaitEpisodes == 0 ? 0
                : totalSemaphoreBlockedCycles / semaphoreWaitEpisodes;
    }

    public long getSemaphoreWaitEpisodes() {
        return semaphoreWaitEpisodes;
    }

    public long getSuccessfulCompletions() {
        return successfulCompletions;
    }

    public long getTerminations() {
        return terminations;
    }

    public long getProducedElements() {
        return producedElements;
    }

    public long getConsumedElements() {
        return consumedElements;
    }

    private ProcessMetrics getOrRegister(PCB process, long cycle) {
        if (process == null) {
            throw new IllegalArgumentException("El proceso no puede ser null.");
        }
        registerProcess(process, cycle);
        return processes.get(process.getId());
    }

    private static final class ProcessMetrics {
        private final long submissionCycle;
        private long cpuCycles;
        private double responseCycles;
        private double semaphoreBlockedCycles;
        private long semaphoreWaitEpisodes;
        private long producedElements;
        private long consumedElements;
        private boolean hasStarted;
        private boolean terminated;
        private boolean completed;

        private ProcessMetrics(long submissionCycle) {
            this.submissionCycle = submissionCycle;
        }
    }
}
