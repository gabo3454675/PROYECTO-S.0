package avilaos.config;

import avilaos.core.clock.GlobalClock;
import avilaos.model.dto.BufferConfig;
import avilaos.model.dto.ComputerConfig;
import avilaos.model.dto.ProcessConfig;
import avilaos.model.dto.SimulationConfig;
import avilaos.model.enumeration.ProcessType;
import avilaos.model.enumeration.SchedulingPolicyType;
import avilaos.simulation.SimulationCluster;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class SimulationClusterConfigTest {
    @TempDir
    Path tempDir;

    @AfterEach
    void resetClock() {
        GlobalClock.resetInstance();
    }

    @Test
    void savesAndLoadsGlobalAndPerComputerConfiguration() throws IOException {
        Path configFile = tempDir.resolve("cluster.json");
        SimulationCluster original = new SimulationCluster();
        original.setCycleDurationMs(250);
        original.setNetworkLatencyCycles(7);
        original.addComputer(new ComputerConfig(3, 512, SchedulingPolicyType.EDF, 2));
        original.addComputer(new ComputerConfig(8, 2048, SchedulingPolicyType.ROUND_ROBIN, 5));
        original.getSyncManager().createBuffer("shared-buffer", 4, 3);
        original.submitProcess(new avilaos.model.pcb.PCB("remote-worker", 8, ProcessType.CPU_BOUND,
                2, 128, 0, 20));

        try {
            ConfigManager.saveConfigToJson(configFile.toString(), original);
            SimulationCluster loaded = ConfigManager.loadSimulationFromJson(configFile.toString());
            try {
                assertEquals(2, loaded.getComputers().size());
                assertEquals(250, loaded.getCycleDurationMs());
                assertEquals(7, loaded.getNetworkLatencyCycles());
                assertEquals(512L * 1024 * 1024,
                        loaded.getComputer(3).getRam().getTotalSize());
                assertEquals(2048L * 1024 * 1024,
                        loaded.getComputer(8).getRam().getTotalSize());
                assertEquals(SchedulingPolicyType.EDF,
                        loaded.getKernel(3).getScheduler().getPolicy().getType());
                assertEquals(5, loaded.getKernel(8).getQuantum());
                assertEquals(1, loaded.getKernel(8).getReadyQueue().size());
                assertEquals(8, loaded.getKernel(8).getReadyQueue().peek().getComputerId());
                assertEquals("shared-buffer", loaded.getSyncManager().getBuffer("shared-buffer").getId());
                assertEquals(7, loaded.getSyncManager().getBuffer("shared-buffer").getNetworkLatencyCycles());
            } finally {
                loaded.shutdown();
            }
        } finally {
            original.shutdown();
        }
    }

    @Test
    void extractsPerComputerRamInMegabytesAndProducerSettings() {
        SimulationCluster simulation = new SimulationCluster();
        simulation.addComputer(new ComputerConfig(0, 1024, SchedulingPolicyType.FCFS, 2));
        simulation.getSyncManager().createBuffer("buffer-1", 3, 0);
        simulation.submitProcess(new avilaos.model.pcb.PCB("producer", 0, ProcessType.PRODUCER,
                1, 64, 0, 10, "buffer-1", 2, 3));
        try {
            SimulationConfig config = ConfigManager.extractSimulationConfigFromCluster(simulation);

            assertEquals(1024, config.computers()[0].ramSizeMB());
            ProcessConfig producer = config.processes()[0];
            assertEquals("buffer-1", producer.bufferId());
            assertEquals(2, producer.produceConsumeInterval());
            assertEquals(3, producer.elementsRequired());
            assertEquals(1, config.buffers().length);
        } finally {
            simulation.shutdown();
        }
    }

    @Test
    void demoConfigurationRunsRemoteProducerAndLocalConsumerWithMetrics() throws IOException {
        SimulationCluster simulation = ConfigManager.loadSimulationFromJson(
                "config/producer-consumer-demo.json");
        try {
            for (int cycle = 0; cycle < 4; cycle++) {
                simulation.advanceCycle();
            }

            assertEquals(1, simulation.getMetricsSnapshot().producedElements());
            assertEquals(1, simulation.getMetricsSnapshot().consumedElements());
            assertEquals(2.0, simulation.getMetricsSnapshot().avgSemaphoreBlockedCycles());
            assertEquals(0, simulation.getKernel(1).getMetricsCollector()
                    .getSemaphoreWaitEpisodes());
            assertEquals(5.0, simulation.getMetricsSnapshot().throughputPerSecond(), 0.000001);
        } finally {
            simulation.shutdown();
        }
    }

    @Test
    void defaultConfigurationUsesReadableAutomaticCycleDuration() throws IOException {
        SimulationCluster simulation = ConfigManager.loadSimulationFromJson(
                "config/default-simulation.json");
        try {
            assertEquals(500, simulation.getCycleDurationMs());
            assertEquals(2, simulation.getComputers().size());
        } finally {
            simulation.shutdown();
        }
    }
}
