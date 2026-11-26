package avilaos.config;

import avilaos.model.enumeration.BlockReason;
import avilaos.simulation.SimulationCluster;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class Project2SwapScenariosTest {
    private static final Path DISK = Path.of("config/project2-swap-disk.json");

    @Test
    void sufficientRamCompletesWithoutSwapRequests() throws Exception {
        SimulationCluster simulation = Project2ConfigLoader.load(DISK,
                Path.of("config/project2-swap-no-pressure.json"));
        try {
            for (int i = 0; i < 100; i++) simulation.advanceCycle();
            assertEquals(2, simulation.getComputer(0).getKernel().getTerminatedQueue().size());
            assertEquals(2, simulation.getComputer(0).getDisk().getCompletedRequests());
        } finally {
            simulation.shutdown();
        }
    }

    @Test
    void pressureScenarioSuspendsAndResumesThroughDisk() throws Exception {
        SimulationCluster simulation = Project2ConfigLoader.load(DISK,
                Path.of("config/project2-swap-pressure.json"));
        try {
            var kernel = simulation.getComputer(0).getKernel();
            boolean sawSuspended = false;
            for (int i = 0; i < 180; i++) {
                simulation.advanceCycle();
                for (var pcb : kernel.getBlockedQueue(BlockReason.IO_WAIT))
                    sawSuspended |= pcb.getState().isSuspended();
                sawSuspended |= !kernel.getReadySuspendedQueue().isEmpty();
            }
            assertTrue(sawSuspended, "El escenario debe pasar por un estado suspendido");
            assertEquals(2, kernel.getTerminatedQueue().size());
            assertEquals(4, simulation.getComputer(0).getDisk().getCompletedRequests());
            assertEquals("SWAP_LIBRE", kernel.getIoProcessManager().getFileSystemManager()
                    .getBlockMap().getBlock(80).getOwnerId());
        } finally {
            simulation.shutdown();
        }
    }

    @Test
    void insufficientSwapKeepsWaitingProcessInLongTermQueue() throws Exception {
        SimulationCluster simulation = Project2ConfigLoader.load(DISK,
                Path.of("config/project2-swap-too-small.json"));
        try {
            simulation.advanceCycle();
            assertEquals(1, simulation.getComputer(0).getKernel().getNewQueue().size());
            for (int i = 0; i < 100; i++) simulation.advanceCycle();
            assertEquals(2, simulation.getComputer(0).getKernel().getTerminatedQueue().size());
            assertEquals(2, simulation.getComputer(0).getDisk().getCompletedRequests());
        } finally {
            simulation.shutdown();
        }
    }

    @Test
    void rejectsFileInsideReservedSwapArea() {
        assertThrows(IllegalArgumentException.class, () -> Project2ConfigLoader.load(
                Path.of("config/project2-swap-conflict-disk.json"),
                Path.of("config/project2-swap-pressure.json")));
    }
}
