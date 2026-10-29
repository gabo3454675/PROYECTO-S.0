package avilaos.gui;

import avilaos.core.hardware.Computer;
import avilaos.model.enumeration.ProcessType;
import avilaos.model.enumeration.SchedulingPolicyType;
import avilaos.model.pcb.PCB;
import avilaos.simulation.SimulationCluster;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javax.imageio.ImageIO;
import javax.swing.SwingUtilities;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class MonitorPanelExportTest {

    @TempDir
    Path tempDir;

    private MonitorPanel createMonitor(String processName) {
        Computer computer = new Computer(0, 1024, SchedulingPolicyType.FCFS, 2);
        computer.getKernel().submitProcess(new PCB(
                processName, 0, ProcessType.CPU_BOUND, 1, 64, 50, 20));
        MonitorPanel monitor = new MonitorPanel(computer.getKernel());
        monitor.updateMonitor();
        computer.shutdown();
        return monitor;
    }

    @Test
    void exportsDashboardAsPng() throws IOException {
        File file = tempDir.resolve("monitor.png").toFile();
        createMonitor("PNG worker").exportToPng(file);

        assertTrue(file.isFile());
        var image = ImageIO.read(file);
        assertNotNull(image);
        assertTrue(image.getHeight() > 800);
    }

    @Test
    void exportsDashboardAsCsvWithEscapedValues() throws IOException {
        File file = tempDir.resolve("monitor.csv").toFile();
        createMonitor("Worker, \"A\"").exportToCsv(file);

        String csv = Files.readString(file.toPath(), StandardCharsets.UTF_8);
        assertTrue(csv.contains("\"READY\",\"1\",\"Nombre\",\"Worker, \"\"A\"\"\""));
        assertTrue(csv.contains("\"General\",\"0\",\"Ciclo actual\",\"0\""));
    }

    @Test
    void exportsDashboardAsHtmlWithEscapedValues() throws IOException {
        File file = tempDir.resolve("monitor.html").toFile();
        createMonitor("Worker <A&B>").exportToHtml(file);

        String html = Files.readString(file.toPath(), StandardCharsets.UTF_8);
        assertTrue(html.contains("<!doctype html>"));
        assertTrue(html.contains("Worker &lt;A&amp;B&gt;"));
        assertTrue(html.contains("<caption>Cola READY</caption>"));
        assertTrue(html.contains("<h2>Datos de los gráficos</h2>"));
    }

    @Test
    void rejectsNullExportDestination() {
        MonitorPanel monitor = createMonitor("Worker");

        assertAll(
                () -> assertThrows(IllegalArgumentException.class, () -> monitor.exportToPng(null)),
                () -> assertThrows(IllegalArgumentException.class, () -> monitor.exportToCsv(null)),
                () -> assertThrows(IllegalArgumentException.class, () -> monitor.exportToHtml(null)));
    }

    @Test
    void automaticAdvancementRunsCyclesAndStopsWhenPaused() throws Exception {
        SimulationCluster simulation = new SimulationCluster();
        simulation.addComputer(0, 1024, SchedulingPolicyType.FCFS, 2);
        int originalCycleDuration = simulation.getCycleDurationMs();
        simulation.setCycleDurationMs(10);
        MonitorPanel monitor = new MonitorPanel(simulation);
        try {
            SwingUtilities.invokeAndWait(monitor::startAutomaticAdvancement);

            long timeout = System.nanoTime() + 2_000_000_000L;
            while (simulation.getCurrentCycle() == 0 && System.nanoTime() < timeout) {
                Thread.sleep(10);
            }
            assertTrue(simulation.getCurrentCycle() > 0, "El timer debe ejecutar al menos un ciclo.");

            SwingUtilities.invokeAndWait(monitor::pauseAutomaticAdvancement);
            long pausedCycle = simulation.getCurrentCycle();
            Thread.sleep(50);
            assertEquals(pausedCycle, simulation.getCurrentCycle());
            assertFalse(monitor.isAutomaticAdvancementRunning());
        } finally {
            monitor.dispose();
            simulation.shutdown();
            simulation.setCycleDurationMs(originalCycleDuration);
        }
    }

    @Test
    void exportsGlobalMultiComputerMetrics() throws IOException {
        SimulationCluster simulation = new SimulationCluster();
        simulation.addComputer(0, 512, SchedulingPolicyType.FCFS, 2);
        simulation.addComputer(1, 1024, SchedulingPolicyType.ROUND_ROBIN, 3);
        simulation.submitProcess(new PCB("node-0", 0, ProcessType.CPU_BOUND, 1, 64, 0, 1));
        simulation.submitProcess(new PCB("node-1", 1, ProcessType.CPU_BOUND, 1, 64, 0, 1));
        simulation.advanceCycle();
        MonitorPanel monitor = new MonitorPanel(simulation);
        File file = tempDir.resolve("cluster.csv").toFile();
        try {
            monitor.updateMonitor();
            monitor.exportToCsv(file);

            String csv = Files.readString(file.toPath(), StandardCharsets.UTF_8);
            assertTrue(csv.contains("\"Métricas globales\",\"0\",\"Utilización CPU (%)\",\"100.0\""));
            assertTrue(csv.contains("\"Métricas globales\",\"0\",\"Throughput (procesos/ciclo)\",\"2.0\""));
            assertTrue(csv.contains("\"Gráfico CPU\",\"1\",\"Computador 0\",\"100.0\""));
            assertTrue(csv.contains("\"Gráfico CPU\",\"2\",\"Computador 1\",\"100.0\""));
            assertTrue(csv.contains("\"Gráfico procesos\""));
            assertTrue(csv.contains("\"Gráfico procesos\",\"1\",\"Terminado\",\"1.0\""));
        } finally {
            simulation.shutdown();
        }
    }
}
