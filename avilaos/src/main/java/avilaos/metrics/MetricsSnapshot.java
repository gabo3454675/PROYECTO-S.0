package avilaos.metrics;

public record MetricsSnapshot(
    long cycle,
    int computerId,
    double cpuUtilization,
    double throughputPerSecond,
    double avgResponseTime,
    double deadlineCompliance,
    double fairnessIndex,
    long processesCompleted,
    long processesBlocked,
    long memoryUsed,
    long memoryTotal,
    double avgSemaphoreBlockedCycles,
    long producedElements,
    long consumedElements
) {
}
