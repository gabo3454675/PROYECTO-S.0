package avilaos.gui;

import avilaos.config.ConfigManager;
import avilaos.model.enumeration.SchedulingPolicyType;
import avilaos.simulation.SimulationCluster;

import javax.swing.*;
import java.io.File;
import java.io.IOException;

/**
 * Ventana Principal de ÁvilaOS con pestañas para Vista 1 (Configuración) y Vista 2 (Monitor).
 */
public class MainFrame extends JFrame {

    private SimulationCluster simulation;
    private ConfigPanel configPanel;
    private MonitorPanel monitorPanel;

    public MainFrame() {
        simulation = new SimulationCluster();
        simulation.addComputer(0, 1024L, SchedulingPolicyType.FCFS, 2);

        initUI();
    }

    private void initUI() {
        setTitle("ÁvilaOS - Simulador de Sistema Operativo Distribuido");
        setSize(1000, 780);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLocationRelativeTo(null);

        createMenuBar();
        buildTabs();
        addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosed(java.awt.event.WindowEvent e) {
                simulation.shutdown();
            }
        });
    }

    private void buildTabs() {
        JTabbedPane tabbedPane = new JTabbedPane();

        configPanel = new ConfigPanel(simulation);
        monitorPanel = new MonitorPanel(simulation);

        tabbedPane.addTab("Vista 1: Configuración", configPanel);
        tabbedPane.addTab("Vista 2: Monitor en Tiempo Real", monitorPanel);

        // Al cambiar a la pestaña de monitor, actualizar datos
        tabbedPane.addChangeListener(e -> {
            if (tabbedPane.getSelectedIndex() == 1) {
                monitorPanel.updateMonitor();
            } else if (tabbedPane.getSelectedIndex() == 0) {
                configPanel.refreshTables();
            }
        });

        setContentPane(tabbedPane);
        revalidate();
        repaint();
    }

    private void createMenuBar() {
        JMenuBar menuBar = new JMenuBar();
        JMenu fileMenu = new JMenu("Archivo");

        JMenuItem saveItem = new JMenuItem("Guardar Configuración (JSON)...");
        saveItem.addActionListener(e -> saveConfiguration());

        JMenuItem loadItem = new JMenuItem("Cargar Configuración (JSON)...");
        loadItem.addActionListener(e -> loadConfiguration());

        JMenu exportMenu = new JMenu("Exportar Monitor");
        addExportItem(exportMenu, "PNG", "png");
        addExportItem(exportMenu, "CSV", "csv");
        addExportItem(exportMenu, "HTML", "html");

        JMenuItem exitItem = new JMenuItem("Salir");
        exitItem.addActionListener(e -> dispose());

        fileMenu.add(saveItem);
        fileMenu.add(loadItem);
        fileMenu.add(exportMenu);
        fileMenu.addSeparator();
        fileMenu.add(exitItem);

        menuBar.add(fileMenu);
        setJMenuBar(menuBar);
    }

    private void addExportItem(JMenu menu, String label, String extension) {
        JMenuItem item = new JMenuItem(label + "...");
        item.addActionListener(e -> exportMonitor(extension));
        menu.add(item);
    }

    private void exportMonitor(String extension) {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Exportar monitor como " + extension.toUpperCase());
        fileChooser.setSelectedFile(new File("avilaos-monitor." + extension));
        if (fileChooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) {
            return;
        }

        File selectedFile = fileChooser.getSelectedFile();
        File destination = selectedFile.getName().toLowerCase().endsWith("." + extension)
                ? selectedFile
                : new File(selectedFile.getParentFile(), selectedFile.getName() + "." + extension);
        try {
            monitorPanel.updateMonitor();
            switch (extension) {
                case "png" -> monitorPanel.exportToPng(destination);
                case "csv" -> monitorPanel.exportToCsv(destination);
                case "html" -> monitorPanel.exportToHtml(destination);
                default -> throw new IllegalArgumentException("Formato de exportación no admitido: " + extension);
            }
            JOptionPane.showMessageDialog(this, "Monitor exportado en:\n" + destination.getAbsolutePath(),
                    "Exportación completada", JOptionPane.INFORMATION_MESSAGE);
        } catch (IOException | IllegalArgumentException ex) {
            JOptionPane.showMessageDialog(this, "No se pudo exportar el monitor:\n" + ex.getMessage(),
                    "Error de exportación", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void saveConfiguration() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Guardar Estado de ÁvilaOS");
        if (fileChooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            File fileToSave = fileChooser.getSelectedFile();
            String path = fileToSave.getAbsolutePath();
            if (!path.endsWith(".json")) path += ".json";

            try {
                ConfigManager.saveConfigToJson(path, simulation);
                JOptionPane.showMessageDialog(this, "Configuración guardada exitosamente en:\n" + path, "Éxito", JOptionPane.INFORMATION_MESSAGE);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Error al guardar: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void loadConfiguration() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Cargar Estado de ÁvilaOS");
        if (fileChooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            File fileToLoad = fileChooser.getSelectedFile();
            try {
                SimulationCluster loaded = ConfigManager.loadSimulationFromJson(fileToLoad.getAbsolutePath());
                simulation.shutdown();
                simulation = loaded;
                buildTabs();
                pack();
                setSize(1100, 850);
                JOptionPane.showMessageDialog(this, "Configuración cargada y validada con éxito.", "Éxito", JOptionPane.INFORMATION_MESSAGE);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Error al cargar la configuración:\n" + ex.getMessage(), "Error de Validación / Carga", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    public static void main(String[] args) {
        try {
            for (UIManager.LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equalsIgnoreCase(info.getName())) {
                    UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (Exception ignored) {}

        SwingUtilities.invokeLater(() -> {
            MainFrame frame = new MainFrame();
            frame.setVisible(true);
        });
    }
}
