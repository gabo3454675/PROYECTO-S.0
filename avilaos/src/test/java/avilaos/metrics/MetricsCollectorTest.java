package avilaos.metrics;

import avilaos.model.enumeration.ProcessType;
import avilaos.model.pcb.PCB;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class MetricsCollectorTest {
    @Test
    void measuresResponseFromSubmissionAndJainFairnessIncludingZeroService() {
        MetricsCollector collector = new MetricsCollector();
        PCB first = new PCB("first", 0, ProcessType.CPU_BOUND, 1, 1, 0, 5);
        PCB second = new PCB("second", 0, ProcessType.CPU_BOUND, 1, 1, 0, 5);
        collector.registerProcess(first, 1);
        collector.registerProcess(second, 2);

        collector.recordCpuCycle(first, 4);
        collector.recordCpuCycle(first, 5);
        collector.recordCpuCycle(second, 4);

        assertEquals(2.5, collector.getAverageResponseTime());
        assertEquals(2, collector.getResponseSamples());
        assertEquals(0.9, collector.getFairnessIndex(), 0.000001);
    }

    @Test
    void averagesSemaphoreWaitEpisodesAndCountsProducerConsumerElements() {
        MetricsCollector collector = new MetricsCollector();
        PCB producer = new PCB("producer", 0, ProcessType.PRODUCER, 1, 1, 0,
                5, "buffer", 1, 1);
        PCB consumer = new PCB("consumer", 0, ProcessType.CONSUMER, 1, 1, 0,
                5, "buffer", 1, 1);

        collector.startSemaphoreWait(consumer, 2);
        collector.finishSemaphoreWait(consumer, 5);
        collector.recordElementProcessed(producer.getType());
        collector.recordElementProcessed(consumer.getType());

        assertEquals(3.0, collector.getAverageSemaphoreBlockedCycles());
        assertEquals(1, collector.getSemaphoreWaitEpisodes());
        assertEquals(1, collector.getProducedElements());
        assertEquals(1, collector.getConsumedElements());
    }

    @Test
    void returnsZeroForMetricsWithoutSamples() {
        MetricsCollector collector = new MetricsCollector();

        assertEquals(0, collector.getAverageResponseTime());
        assertEquals(0, collector.getFairnessIndex());
        assertEquals(0, collector.getAverageSemaphoreBlockedCycles());
        assertEquals(0, MetricsCollector.fairnessIndex(0, 0, 0));
    }

    @Test
    void removingProcessAlsoRemovesItsSamplesAndCompletions() {
        MetricsCollector collector = new MetricsCollector();
        PCB process = new PCB("worker", 0, ProcessType.CPU_BOUND, 1, 1, 0, 5);
        collector.registerProcess(process, 1);
        collector.recordCpuCycle(process, 4);
        collector.recordTermination(process, 5, true);

        assertTrue(collector.removeProcess(process));
        assertEquals(0, collector.getProcessCount());
        assertEquals(0, collector.getResponseSamples());
        assertEquals(0, collector.getSuccessfulCompletions());
        assertEquals(0, collector.getTerminations());
        assertEquals(0, collector.getFairnessIndex());
    }
}
