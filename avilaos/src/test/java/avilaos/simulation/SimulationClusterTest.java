package avilaos.simulation;

import avilaos.core.clock.GlobalClock;
import avilaos.core.hardware.Computer;
import avilaos.kernel.Kernel;
import avilaos.kernel.sync.Buffer;
import avilaos.model.enumeration.BlockReason;
import avilaos.model.enumeration.ProcessState;
import avilaos.model.enumeration.ProcessType;
import avilaos.model.enumeration.SchedulingPolicyType;
import avilaos.model.pcb.PCB;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class SimulationClusterTest {
    private SimulationCluster simulation;

    @BeforeEach
    void setUp() {
        GlobalClock.resetInstance();
        simulation = new SimulationCluster();
        simulation.addComputer(0, 512, SchedulingPolicyType.FCFS, 2);
        simulation.addComputer(1, 1024, SchedulingPolicyType.ROUND_ROBIN, 3);
        simulation.setCycleDurationMs(100);
    }

    @AfterEach
    void tearDown() {
        simulation.shutdown();
        GlobalClock.resetInstance();
    }

    @Test
    void computersShareSynchronizationAndProcessesAreRoutedByComputerId() {
        Kernel kernel0 = simulation.getKernel(0);
        Kernel kernel1 = simulation.getKernel(1);
        PCB process = new PCB("worker-1", 1, ProcessType.CPU_BOUND, 1, 64, 0, 4);

        assertSame(simulation.getSyncManager(), kernel0.getSyncManager());
        assertSame(kernel0.getSyncManager(), kernel1.getSyncManager());
        simulation.submitProcess(process);

        assertEquals(0, kernel0.getReadyQueue().size());
        assertEquals(1, kernel1.getReadyQueue().size());
        assertSame(process, kernel1.getReadyQueue().peek());
    }

    @Test
    void sharedBufferUnblocksProcessOnItsOwningComputer() throws InterruptedException {
        simulation.setNetworkLatencyCycles(0);
        Buffer buffer = simulation.getSyncManager().createBuffer("shared", 1, 0);
        PCB consumer = new PCB("remote-consumer", 1, ProcessType.CPU_BOUND, 1, 64, 0, 4);

        assertNull(buffer.get(consumer));
        assertEquals(ProcessState.BLOCKED, consumer.getState());
        simulation.getKernel(1).addToBlockedQueue(consumer);

        buffer.getSemFull().signal();

        assertEquals(ProcessState.READY, consumer.getState());
        assertEquals(0, simulation.getKernel(1).getBlockedQueue(BlockReason.SEMAPHORE_WAIT).size());
        assertEquals(1, simulation.getKernel(1).getReadyQueue().size());
        assertEquals(0, simulation.getKernel(0).getReadyQueue().size());
    }

    @Test
    void oneGlobalCycleRunsEveryComputerAndTracksCpuMetrics() {
        simulation.submitProcess(new PCB("first", 0, ProcessType.CPU_BOUND, 1, 64, 0, 1));
        simulation.submitProcess(new PCB("second", 1, ProcessType.CPU_BOUND, 1, 64, 0, 1));

        simulation.advanceCycle();

        assertEquals(1, simulation.getCurrentCycle());
        for (Computer computer : simulation.getComputers()) {
            assertEquals(1, computer.getKernel().getCyclesExecuted());
            assertEquals(100.0, computer.getKernel().getCpuUtilization());
            assertEquals(1, computer.getKernel().getTerminatedQueue().size());
        }
    }

    @Test
    void eachComputerExecutesTheGlobalCycleOnItsOwnWorkerThread() {
        AtomicReference<Thread> firstWorker = new AtomicReference<>();
        AtomicReference<Thread> secondWorker = new AtomicReference<>();
        simulation.getKernel(0).addMetricsListener(snapshot -> firstWorker.set(Thread.currentThread()));
        simulation.getKernel(1).addMetricsListener(snapshot -> secondWorker.set(Thread.currentThread()));

        simulation.advanceCycle();

        assertNotNull(firstWorker.get());
        assertNotNull(secondWorker.get());
        assertNotSame(Thread.currentThread(), firstWorker.get());
        assertNotSame(Thread.currentThread(), secondWorker.get());
        assertNotSame(firstWorker.get(), secondWorker.get());
    }

    @Test
    void producerAndConsumerExchangeDataAcrossComputersThroughSharedBuffer() {
        simulation.setNetworkLatencyCycles(0);
        simulation.createBuffer("pipeline", 2, 0);
        PCB producer = new PCB("producer", 0, ProcessType.PRODUCER, 1, 64, 0,
                10, "pipeline", 1, 1);
        PCB consumer = new PCB("consumer", 1, ProcessType.CONSUMER, 1, 64, 0,
                10, "pipeline", 1, 1);
        simulation.submitProcess(producer);
        simulation.submitProcess(consumer);

        simulation.advanceCycle();

        assertEquals(1, producer.getElementsProcessed());
        assertEquals(1, consumer.getElementsProcessed());
        assertEquals(1, simulation.getKernel(0).getTerminatedQueue().size());
        assertEquals(1, simulation.getKernel(1).getTerminatedQueue().size());
        assertTrue(simulation.getSyncManager().getBuffer("pipeline").isEmpty());
    }

    @Test
    void remoteProducerWaitsForConfiguredNetworkLatencyBeforeDelivery() {
        simulation.setNetworkLatencyCycles(2);
        simulation.createBuffer("remote-pipeline", 2, 0);
        PCB consumer = new PCB("local-consumer", 0, ProcessType.CONSUMER, 1, 64, 0,
                10, "remote-pipeline", 1, 1);
        PCB producer = new PCB("remote-producer", 1, ProcessType.PRODUCER, 1, 64, 0,
                10, "remote-pipeline", 1, 1);
        simulation.submitProcess(consumer);
        simulation.submitProcess(producer);

        simulation.advanceCycle();
        assertEquals(ProcessState.BLOCKED, consumer.getState());
        assertEquals(BlockReason.SEMAPHORE_WAIT, consumer.getBlockReason());
        assertEquals(ProcessState.BLOCKED, producer.getState());
        assertEquals(BlockReason.NETWORK_LATENCY, producer.getBlockReason());

        simulation.advanceCycle();
        assertEquals(ProcessState.READY, producer.getState());
        assertEquals(0, producer.getNetworkLatencyRemaining());

        simulation.advanceCycle();
        assertEquals(ProcessState.TERMINATED, producer.getState());
        assertEquals(1, producer.getElementsProcessed());
        assertEquals(ProcessState.READY, consumer.getState());

        simulation.advanceCycle();
        assertEquals(ProcessState.TERMINATED, consumer.getState());
        assertEquals(1, consumer.getElementsProcessed());
        assertTrue(simulation.getSyncManager().getBuffer("remote-pipeline").isEmpty());

        assertEquals(1, simulation.getKernel(0).getMetricsCollector()
                .getSemaphoreWaitEpisodes());
        assertEquals(2.0, simulation.getKernel(0).getMetricsCollector()
                .getAverageSemaphoreBlockedCycles());
        assertEquals(0, simulation.getKernel(1).getMetricsCollector()
                .getSemaphoreWaitEpisodes(), "La latencia remota no es espera de semáforo.");
        assertEquals(1, simulation.getMetricsSnapshot().producedElements());
        assertEquals(1, simulation.getMetricsSnapshot().consumedElements());
        assertEquals(1.0, simulation.getMetricsSnapshot().avgResponseTime());
        assertEquals(1.0, simulation.getMetricsSnapshot().fairnessIndex(), 0.000001);
        assertEquals(5.0, simulation.getMetricsSnapshot().throughputPerSecond(), 0.000001);
    }
}
