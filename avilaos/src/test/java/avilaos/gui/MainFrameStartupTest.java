package avilaos.gui;

import avilaos.simulation.SimulationCluster;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class MainFrameStartupTest {

    @Test
    void startupLoadsTheExampleComputersProcessesAndBuffers() {
        SimulationCluster simulation = MainFrame.createInitialSimulation();
        try {
            assertEquals(2, simulation.getComputers().size());
            assertEquals(500, simulation.getCycleDurationMs());
            assertEquals(2, simulation.getNetworkLatencyCycles());
            assertEquals(2, simulation.getKernel(0).getReadyQueue().size());
            assertEquals(3, simulation.getKernel(1).getReadyQueue().size());
            assertNotNull(simulation.getSyncManager().getBuffer("buf_alpha"));
            assertNotNull(simulation.getSyncManager().getBuffer("buf_beta"));
        } finally {
            simulation.shutdown();
        }
    }
}
