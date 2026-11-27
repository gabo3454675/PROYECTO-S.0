package avilaos;

import avilaos.config.Project2ConfigLoader;
import avilaos.simulation.SimulationCluster;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class Project2AcceptanceTest {
    private static final Path DISK = Path.of("config/project2-disk-demo.json");
    private static final Path PROCESSES = Path.of("config/project2-processes-demo.json");

    @Test
    void loadsDemoDiskAndSwap() throws Exception {
        SimulationCluster simulation = Project2ConfigLoader.load(DISK, PROCESSES);
        try {
            var computer = simulation.getComputer(0);
            assertNotNull(computer.getDisk());
            var map = computer.getKernel().getIoProcessManager().getFileSystemManager().getBlockMap();
            assertEquals(200, map.getTotalBlocks());
            assertEquals(20, map.getSwapCapacity());
            assertEquals("SWAP_LIBRE", map.getBlock(180).getOwnerId());
            assertNotNull(computer.getKernel().getIoProcessManager().getFileSystemManager()
                    .resolve("/usuarios/notas.txt"));
        } finally {
            simulation.shutdown();
        }
    }

    @Test
    void savedDiskCanBeLoadedAgain() throws Exception {
        SimulationCluster simulation = Project2ConfigLoader.load(DISK, PROCESSES);
        Path savedDisk = Files.createTempFile("avilaos-disk-", ".json");
        try {
            Project2ConfigLoader.saveDisk(savedDisk, simulation);
            SimulationCluster restored = Project2ConfigLoader.load(savedDisk, PROCESSES);
            try {
                var files = restored.getComputer(0).getKernel().getIoProcessManager().getFileSystemManager();
                assertNotNull(files.resolve("/sistema/boot.bin"));
                assertEquals(20, files.getBlockMap().getSwapCapacity());
            } finally {
                restored.shutdown();
            }
        } finally {
            simulation.shutdown();
            Files.deleteIfExists(savedDisk);
        }
    }
}
