package avilaos.kernel;

import avilaos.core.clock.GlobalClock;
import avilaos.core.hardware.RAM;
import avilaos.model.enumeration.ProcessState;
import avilaos.model.enumeration.ProcessType;
import avilaos.model.enumeration.SchedulingPolicyType;
import avilaos.model.pcb.PCB;
import avilaos.simulation.SimulationCluster;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class KernelLifecycleTest {
    private SimulationCluster simulation;
    private Kernel kernel;

    @BeforeEach
    void setUp() {
        GlobalClock.resetInstance();
        simulation = new SimulationCluster();
        simulation.addComputer(0, 1, SchedulingPolicyType.FCFS, 2);
        kernel = simulation.getKernel(0);
    }

    @AfterEach
    void tearDown() {
        simulation.shutdown();
        GlobalClock.resetInstance();
    }

    @Test
    void memoryRequirementsAreAllocatedAndReleasedInMegabytesOnlyOnce() {
        PCB process = new PCB("worker", 0, ProcessType.CPU_BOUND, 1, 1, 0, 5);
        simulation.submitProcess(process);

        assertEquals(1024L * 1024L, kernel.getComputer().getRam().getUsedSize());

        kernel.terminateProcess(process);
        kernel.terminateProcess(process);

        assertEquals(0, kernel.getComputer().getRam().getUsedSize());
        assertEquals(1, kernel.getTerminatedQueue().size());
    }

    @Test
    void deadlineOnNewProcessDoesNotReleaseMemoryOwnedByAnotherProcess() {
        PCB running = new PCB("running", 0, ProcessType.CPU_BOUND, 1, 1, 0, 5);
        PCB waiting = new PCB("waiting", 0, ProcessType.CPU_BOUND, 1, 1, 2, 5);
        simulation.submitProcess(running);
        simulation.submitProcess(waiting);

        simulation.advanceCycle();
        assertEquals(ProcessState.NEW, waiting.getState());
        assertEquals(1024L * 1024L, kernel.getComputer().getRam().getUsedSize());

        simulation.advanceCycle();

        assertEquals(ProcessState.TERMINATED, waiting.getState());
        assertTrue(kernel.getNewQueue().isEmpty());
        assertEquals(1024L * 1024L, kernel.getComputer().getRam().getUsedSize());
    }

    @Test
    void expiredRunningProcessDoesNotExecuteAtItsDeadline() {
        PCB process = new PCB("deadline", 0, ProcessType.CPU_BOUND, 1, 1, 1, 5);
        simulation.submitProcess(process);

        simulation.advanceCycle();

        assertEquals(ProcessState.TERMINATED, process.getState());
        assertEquals(0, process.getProgramCounter());
        assertEquals(0, kernel.getComputer().getRam().getUsedSize());
        assertEquals(1, kernel.getTerminatedQueue().size());
    }

    @Test
    void expiredSemaphoreWaiterIsRemovedAndCannotReturnToReady() {
        simulation.createBuffer("empty", 1, 0);
        PCB consumer = new PCB("consumer", 0, ProcessType.CONSUMER, 1, 1, 2,
                10, "empty", 1, 1);
        simulation.submitProcess(consumer);

        simulation.advanceCycle();
        assertEquals(ProcessState.BLOCKED, consumer.getState());
        assertEquals(1, simulation.getSyncManager().getBuffer("empty")
                .getSemFull().getWaitingCount());

        simulation.advanceCycle();
        simulation.getSyncManager().getBuffer("empty").getSemFull().signal();

        assertEquals(ProcessState.TERMINATED, consumer.getState());
        assertEquals(0, kernel.getReadyQueue().size());
        assertEquals(0, kernel.getBlockedQueue(
                avilaos.model.enumeration.BlockReason.SEMAPHORE_WAIT).size());
        assertEquals(0, simulation.getSyncManager().getBuffer("empty")
                .getSemFull().getWaitingCount());
        assertEquals(0, kernel.getComputer().getRam().getUsedSize());
    }

    @Test
    void oversizedProcessMemoryIsRejectedInsteadOfRemainingInNewQueue() {
        PCB process = new PCB("too-large", 0, ProcessType.CPU_BOUND, 1, 2, 0, 1);

        assertThrows(IllegalArgumentException.class, () -> simulation.submitProcess(process));
        assertTrue(kernel.getNewQueue().isEmpty());
        assertTrue(kernel.getReadyQueue().isEmpty());
    }

    @Test
    void memoryConversionsRejectValuesThatWouldOverflow() {
        assertThrows(IllegalArgumentException.class, () -> new RAM(Long.MAX_VALUE));

        PCB process = new PCB("overflow", 0, ProcessType.CPU_BOUND, 1, Long.MAX_VALUE,
                0, 1);
        assertThrows(IllegalArgumentException.class, () -> simulation.submitProcess(process));
        assertTrue(kernel.getNewQueue().isEmpty());
    }
}
