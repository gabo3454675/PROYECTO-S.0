package avilaos.gui;

import avilaos.simulation.SimulationCluster;
import avilaos.model.enumeration.ProcessType;
import avilaos.model.enumeration.SchedulingPolicyType;
import avilaos.model.pcb.PCB;
import org.junit.jupiter.api.Test;

import javax.swing.*;
import java.awt.*;

import static org.junit.jupiter.api.Assertions.*;

class ConfigPanelStartupTest {

    @Test
    void initialTablesShowProcessesBuffersAndComputersFromLoadedConfiguration() {
        SimulationCluster simulation = MainFrame.createInitialSimulation();
        try {
            ConfigPanel panel = new ConfigPanel(simulation);

            assertEquals(5, findTableRowCount(panel, 8));
            assertEquals(2, findTableRowCount(panel, 3));
            assertEquals(2, findTableRowCount(panel, 12));
        } finally {
            simulation.shutdown();
        }
    }

    @Test
    void deletingSelectedProcessRemovesItFromConfigurationAndSimulation() {
        SimulationCluster simulation = new SimulationCluster();
        simulation.addComputer(0, 128, SchedulingPolicyType.FCFS, 2);
        PCB process = new PCB("removable", 0, ProcessType.CPU_BOUND, 1, 16, 0, 5);
        simulation.submitProcess(process);
        ConfigPanel panel = new ConfigPanel(simulation);
        try {
            JTable processTable = findTable(panel, 8);
            assertNotNull(processTable);
            assertEquals(1, processTable.getRowCount());
            processTable.setRowSelectionInterval(0, 0);

            assertEquals(1, panel.removeSelectedProcesses());

            assertEquals(0, processTable.getRowCount());
            assertTrue(simulation.getKernel(0).getReadyQueue().isEmpty());
            assertEquals(0, simulation.getComputer(0).getRam().getUsedSize());
        } finally {
            simulation.shutdown();
        }
    }

    @Test
    void deletingBufferReferencedByProcessIsBlocked() {
        SimulationCluster simulation = new SimulationCluster();
        simulation.addComputer(0, 128, SchedulingPolicyType.FCFS, 2);
        simulation.createBuffer("in-use", 2, 0);
        simulation.submitProcess(new PCB("consumer", 0, ProcessType.CONSUMER, 1, 16, 0,
                5, "in-use", 1, 1));
        ConfigPanel panel = new ConfigPanel(simulation);
        try {
            JTable bufferTable = findTable(panel, 3);
            assertNotNull(bufferTable);
            assertEquals(1, bufferTable.getRowCount());
            bufferTable.setRowSelectionInterval(0, 0);

            assertThrows(IllegalStateException.class, panel::removeSelectedBuffers);

            assertEquals(1, bufferTable.getRowCount());
            assertNotNull(simulation.getSyncManager().getBuffer("in-use"));

            JTable processTable = findTable(panel, 8);
            assertNotNull(processTable);
            processTable.setRowSelectionInterval(0, 0);
            assertEquals(1, panel.removeSelectedProcesses());

            bufferTable.setRowSelectionInterval(0, 0);
            assertEquals(1, panel.removeSelectedBuffers());
            assertEquals(0, bufferTable.getRowCount());
            assertNull(simulation.getSyncManager().getBuffer("in-use"));
        } finally {
            simulation.shutdown();
        }
    }

    private JTable findTable(Container container, int columnCount) {
        for (Component component : container.getComponents()) {
            if (component instanceof JTable table && table.getColumnCount() == columnCount) {
                return table;
            }
            if (component instanceof Container child) {
                JTable table = findTable(child, columnCount);
                if (table != null) return table;
            }
        }
        return null;
    }

    private int findTableRowCount(Container container, int columnCount) {
        for (Component component : container.getComponents()) {
            if (component instanceof JTable table && table.getColumnCount() == columnCount) {
                return table.getRowCount();
            }
            if (component instanceof Container child) {
                int rowCount = findTableRowCount(child, columnCount);
                if (rowCount >= 0) return rowCount;
            }
        }
        return -1;
    }
}
