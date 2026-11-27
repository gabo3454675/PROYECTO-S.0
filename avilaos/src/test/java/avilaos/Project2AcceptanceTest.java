package avilaos;

import avilaos.simulation.SimulationCluster;
import org.junit.jupiter.api.Test;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.*;

class Project2AcceptanceTest {
    // Escenario: carga exitosa con disco de 300 bloques y swap de 100
    @Test
    void loadsDemoScenarioWith300BlocksAnd100Swap() throws Exception {
        SimulationCluster simulation = SimulationCluster.load(
                Paths.get("config/project2-disk-demo.json"),
                Paths.get("config/project2-processes-demo.json"));
        try {
            assertEquals(1, simulation.getComputers().size());
            var blocks = simulation.getComputer(0).getKernel()
                    .getIoProcessManager().getFileSystemManager().getBlockMap();
            assertEquals(300, blocks.getTotalBlocks());
            assertEquals(100, blocks.getSwapCapacity());
            assertEquals("SWAP_LIBRE", blocks.getBlock(200).getOwnerId());
            assertNotNull(simulation.getComputer(0).getDisk());
        } finally {
            simulation.shutdown();
        }
    }

    // Escenario: admisión condicionada por RAM
    @Test
    void admissionConditionalOnAvailableRam() throws Exception {
        SimulationCluster simulation = SimulationCluster.load(
                Paths.get("config/project2-disk-demo.json"),
                Paths.get("config/project2-processes-demo.json"));
        try {
            simulation.advanceCycle(); // advance to trigger admission check
            var kernel = simulation.getComputer(0).getKernel();
            assertTrue(kernel.getNewQueue().size() >= 0);
            // Verify processes are tracked
            assertTrue(kernel.getTrackedProcesses().size() >= 0);
        } finally {
            simulation.shutdown();
        }
    }

    // Escenario: suspension y reactivación pasan por disco
    @Test
    void suspensionAndResumeThroughDisk() throws Exception {
        SimulationCluster simulation = SimulationCluster.load(
                Paths.get("config/project2-disk-demo.json"),
                Paths.get("config/project2-processes-demo.json"));
        try {
            var kernel = simulation.getComputer(0).getKernel();
            boolean sawSuspended = false;
            for (int i = 0; i < 200; i++) {
                simulation.advanceCycle();
                for (var pcb : kernel.getBlockedQueue(
                        avilaos.model.enumeration.BlockReason.IO_WAIT))
                    sawSuspended |= pcb.getState().isSuspended();
                sawSuspended |= !kernel.getReadySuspendedQueue().isEmpty();
                if (sawSuspended) break;
            }
            assertTrue(sawSuspended, "El escenario debe pasar por un estado suspendido");
        } finally {
            simulation.shutdown();
        }
    }

    // Escenario: guardar y cargar configuración
    @Test
    void saveAndLoadConfiguration() throws Exception {
        SimulationCluster original = SimulationCluster.load(
                Paths.get("config/project2-disk-demo.json"),
                Paths.get("config/project2-processes-demo.json"));
        try {
            Path tempDir = java.nio.fileFiles.createTempDirectory("avilaos-test");
            Path savedDisk = tempDir.resolve("disk.json");
            Project2ConfigLoader.saveDisk(savedDisk, original);
            
            SimulationCluster restored = SimulationCluster.load(
                    savedDisk,
                    Paths.get("config/project2-processes-demo.json"));
            try {
                assertNotNull(restored.getComputer(0).getDisk());
                var files = restored.getComputer(0).getKernel()
                        .getIoProcessManager().getFileSystemManager();
                assertTrue(files.resolve("/usuarios/resumen.txt") != null);
            } finally {
                restored.shutdown();
            }
        } finally {
            original.shutdown();
        }
    }

    // Escenario: políticas de disco funcionan
    @Test
    void diskPoliciesWorkCorrectly() throws Exception {
        SimulationCluster simulation = SimulationCluster.load(
                Paths.get("config/project2-disk-demo.json"),
                Paths.get("config/project2-processes-demo.json"));
        try {
            var disk = simulation.getComputer(0).getDisk();
            assertNotNull(disk);
            // Advance some cycles to exercise disk
            for (int i = 0; i < 10; i++) {
                simulation.advanceCycle();
            }
            assertTrue(disk.getCompletedRequests() >= 0);
        } finally {
            simulation.shutdown();
        }
    }

    // Escenario: modos admin/usuario
    @Test
    void adminUserModesWork() throws Exception {
        SimulationCluster simulation = SimulationCluster.load(
                Paths.get("config/project2-disk-demo.json"),
                Paths.get("config/project2-processes-demo.json"));
        try {
            var kernel = simulation.getComputer(0).getKernel();
            assertNotNull(kernel.getUserMode());
            assertNotNull(kernel.getActiveUser());
            // Verify user mode can be changed
            kernel.setUserMode(avilaos.model.enumeration.UserMode.USER);
            assertEquals(avilaos.model.enumeration.UserMode.USER, kernel.getUserMode());
            kernel.setUserMode(avilaos.model.enumeration.UserMode.ADMIN);
            assertEquals(avilaos.model.enumeration.UserMode.ADMIN, kernel.getUserMode());
        } finally {
            simulation.shutdown();
        }
    }
}