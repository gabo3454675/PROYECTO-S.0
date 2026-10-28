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

import static org.junit.jupiter.api.Assertions.*;

class SimulationClusterTest {
    private SimulationCluster simulation;

    @BeforeEach
    void setUp() {
        GlobalClock.resetInstance();
        simulation = new SimulationCluster();
        simulation.addComputer(0, 512, SchedulingPolicyType.FCFS, 2);
        simulation.addComputer(1, 1024, SchedulingPolicyType.ROUND_ROBIN, 3);
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
}
