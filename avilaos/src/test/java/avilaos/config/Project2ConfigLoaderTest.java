package avilaos.config;

import avilaos.model.enumeration.ProcessState;
import avilaos.simulation.SimulationCluster;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class Project2ConfigLoaderTest {
    @TempDir Path temp;

    @Test
    void savedDiskCanBeLoadedWithCurrentFiles() throws Exception {
        SimulationCluster original = Project2ConfigLoader.load(
                Path.of("config/project2-disk-demo.json"),
                Path.of("config/project2-processes-demo.json"));
        Path saved = temp.resolve("saved-disk.json");
        try {
            for (int cycle = 0; cycle < 60; cycle++) original.advanceCycle();
            Project2ConfigLoader.saveDisk(saved, original);
        } finally {
            original.shutdown();
        }
        SimulationCluster restored = Project2ConfigLoader.load(saved,
                Path.of("config/project2-processes-demo.json"));
        try {
            var files = restored.getComputer(0).getKernel().getIoProcessManager().getFileSystemManager();
            assertEquals(3, files.resolve("/usuarios/resumen.txt").getSizeInBlocks());
            assertEquals("user1", files.resolve("/usuarios/resumen.txt").getOwner());
        } finally {
            restored.shutdown();
        }
    }

    @Test
    void requiresBothFilesAndRunsIoThroughDiskClock() throws Exception {
        Path disk = temp.resolve("disk.json");
        Path processes = temp.resolve("processes.json");
        Files.writeString(disk, """
                {"diskConfig":{"totalBlocks":100,"initialHead":0,"positionsPerCycle":2},
                 "fileSystem":[{"path":"/docs","type":"DIRECTORY","owner":"admin","permissions":"public"},
                               {"path":"/docs/data.txt","type":"FILE","startBlock":5,"blocks":2,"owner":"admin","permissions":"public"}]}
                """);
        Files.writeString(processes, """
                {"globalConfig":{"cycleDurationMs":300,"swapSizeBlocks":20,"initialDiskPolicy":"FIFO"},
                 "computer":{"ramCapacity":1024,"policy":"FCFS","quantum":0},
                 "processes":[{"name":"read","memory":16,"deadline":100,"instructions":5,
                               "targetPath":"/docs/data.txt","operation":"READ"}]}
                """);
        assertThrows(IllegalArgumentException.class, () -> Project2ConfigLoader.load(disk, null));
        SimulationCluster simulation = Project2ConfigLoader.load(disk, processes);
        try {
            var computer = simulation.getComputer(0);
            assertNotNull(computer.getDisk());
            assertEquals(20, simulation.getConfiguredSwapSizeBlocks());
            simulation.advanceCycle();
            assertEquals(2, computer.getDisk().getHeadPosition());
            assertEquals(ProcessState.BLOCKED, computer.getKernel().getBlockedQueue(
                    avilaos.model.enumeration.BlockReason.IO_WAIT).peek().getState());
            for (int cycle = 0; cycle < 5; cycle++) simulation.advanceCycle();
            assertEquals(1, computer.getDisk().getCompletedRequests());
        } finally {
            simulation.shutdown();
        }
    }

    @Test
    void bundledDemoLoadsAndCompletesFileCreation() throws Exception {
        SimulationCluster simulation = Project2ConfigLoader.load(
                Path.of("config/project2-disk-demo.json"),
                Path.of("config/project2-processes-demo.json"));
        try {
            assertEquals(1, simulation.getComputers().size());
            for (int cycle = 0; cycle < 60; cycle++) simulation.advanceCycle();
            var files = simulation.getComputer(0).getKernel().getIoProcessManager().getFileSystemManager();
            assertNotNull(files.resolve("/usuarios/resumen.txt"));
            assertEquals(3, files.resolve("/usuarios/resumen.txt").getSizeInBlocks());
        } finally {
            simulation.shutdown();
        }
    }
}
