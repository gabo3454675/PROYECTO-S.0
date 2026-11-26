package avilaos.gui;

import avilaos.config.ConfigManager;
import avilaos.config.Project2ConfigLoader;
import avilaos.model.enumeration.SchedulingPolicyType;
import avilaos.simulation.SimulationCluster;

import javax.swing.*;
import java.io.File;
import java.io.IOException;
import java.awt.GraphicsEnvironment;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Ventana principal de ÁvilaOS con las pestañas de configuración y monitor.
 */
public class MainFrame extends JFrame {

    private static final Logger LOGGER = Logger.getLogger(MainFrame.class.getName());

    private SimulationCluster simulation;
    private ConfigPanel configPanel;
    private MonitorPanel monitorPanel;
    private Project2DiskPanel diskPanel;
    private Path activeConfigPath = Path.of("config", "default-simulation.json");
    private Path activeDiskConfigPath;
    private Path activeProcessConfigPath;

    public MainFrame() {
        simulation = createInitialSimulation();

        initUI();
    }

    static SimulationCluster createInitialSimulation() {
        File defaultConfig = Path.of("config", "default-simulation.json").toFile();
        try {
            if (!Files.isRegularFile(defaultConfig.toPath())) {
                throw new IOException("No se encontró " + defaultConfig.getAbsolutePath());
            }
            return ConfigManager.loadSimulationFromJson(defaultConfig.getPath());
        } catch (IOException | RuntimeException ex) {
            LOGGER.log(Level.SEVERE, "No se pudo cargar el escenario inicial de ÁvilaOS.", ex);
            if (!GraphicsEnvironment.isHeadless()) {
                JOptionPane.showMessageDialog(null,
                        "No se pudo cargar el escenario de ejemplo:\n" + ex.getMessage()
                                + "\nSe iniciará una simulación vacía. Usa Archivo → Cargar Configuración (JSON)... "
                                + "para abrir un escenario.",
                        "Escenario inicial no disponible", JOptionPane.WARNING_MESSAGE);
            }
            SimulationCluster emptySimulation = new SimulationCluster();
            emptySimulation.addComputer(0, 1024L, SchedulingPolicyType.FCFS, 2);
            return emptySimulation;
        }
    }

    private void initUI() {
        setTitle("ÁvilaOS - Simulador de Sistema Operativo Distribuido");
        setSize(1280, 900);
        setMinimumSize(new java.awt.Dimension(1024, 700));
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLocationRelativeTo(null);

        createMenuBar();
        buildTabs();
        addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosed(java.awt.event.WindowEvent e) {
                monitorPanel.dispose();
                simulation.shutdown();
            }
        });
    }

    private void buildTabs() {
        if (diskPanel != null) diskPanel.dispose();
        JTabbedPane tabbedPane = new JTabbedPane();

        configPanel = new ConfigPanel(simulation);
        monitorPanel = new MonitorPanel(simulation, this::resetScenario);

        tabbedPane.addTab("Configuración", configPanel);
        tabbedPane.addTab("Monitor", monitorPanel);
        if (simulation.getComputers().size() == 1 && simulation.getComputers().get(0).getDisk() != null) {
            diskPanel = new Project2DiskPanel(simulation.getComputers().get(0));
            tabbedPane.addTab("Disco y archivos", diskPanel);
        } else {
            diskPanel = null;
        }

        // Al cambiar a la pestaña de monitor, actualizar datos
        tabbedPane.addChangeListener(e -> {
            if (tabbedPane.getSelectedIndex() == 1) {
                monitorPanel.updateMonitor();
            } else if (tabbedPane.getSelectedIndex() == 0) {
                configPanel.refreshTables();
            } else if (diskPanel != null) {
                diskPanel.refresh();
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

        JMenuItem loadProject2Item = new JMenuItem("Cargar Proyecto 2 (dos JSON)...");
        loadProject2Item.addActionListener(e -> loadProject2Configuration());

        JMenuItem saveDiskItem = new JMenuItem("Guardar disco Proyecto 2 (JSON)...");
        saveDiskItem.addActionListener(e -> saveProject2Disk());

        JMenu exportMenu = new JMenu("Exportar Monitor");
        addExportItem(exportMenu, "PNG", "png");
        addExportItem(exportMenu, "CSV", "csv");
        addExportItem(exportMenu, "HTML", "html");

        JMenuItem exitItem = new JMenuItem("Salir");
        exitItem.addActionListener(e -> dispose());

        fileMenu.add(saveItem);
        fileMenu.add(loadItem);
        fileMenu.add(loadProject2Item);
        fileMenu.add(saveDiskItem);
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

    private void saveProject2Disk() {
        boolean resume = monitorPanel.isAutomaticAdvancementRunning();
        monitorPanel.pauseAutomaticAdvancement();
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Guardar sistema de archivos del Proyecto 2");
        chooser.setSelectedFile(new File("project2-disk.json"));
        try {
            if (chooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) return;
            Path path = chooser.getSelectedFile().toPath();
            if (!path.toString().toLowerCase().endsWith(".json")) path = Path.of(path + ".json");
            Project2ConfigLoader.saveDisk(path, simulation);
            JOptionPane.showMessageDialog(this, "Disco guardado en:\n" + path.toAbsolutePath());
        } catch (IOException | RuntimeException ex) {
            JOptionPane.showMessageDialog(this, "No se pudo guardar el disco:\n" + ex.getMessage(),
                    "Error de guardado", JOptionPane.ERROR_MESSAGE);
        } finally {
            if (resume) monitorPanel.startAutomaticAdvancement();
        }
    }

    private void loadConfiguration() {
        boolean resumeAutomaticAdvancement = monitorPanel.isAutomaticAdvancementRunning();
        monitorPanel.pauseAutomaticAdvancement();
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Cargar Estado de ÁvilaOS");
        if (fileChooser.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) {
            if (resumeAutomaticAdvancement) monitorPanel.startAutomaticAdvancement();
            return;
        }

        File fileToLoad = fileChooser.getSelectedFile();
        try {
            SimulationCluster loaded = ConfigManager.loadSimulationFromJson(fileToLoad.getAbsolutePath());
            monitorPanel.dispose();
            if (diskPanel != null) diskPanel.dispose();
            simulation.shutdown();
            simulation = loaded;
            activeConfigPath = fileToLoad.toPath();
            activeDiskConfigPath = null;
            activeProcessConfigPath = null;
            buildTabs();
            pack();
            setSize(1280, 900);
            JOptionPane.showMessageDialog(this, "Configuración cargada y validada con éxito.", "Éxito", JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception ex) {
            if (resumeAutomaticAdvancement) monitorPanel.startAutomaticAdvancement();
            JOptionPane.showMessageDialog(this, "Error al cargar la configuración:\n" + ex.getMessage(), "Error de Validación / Carga", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void loadProject2Configuration() {
        boolean resume = monitorPanel.isAutomaticAdvancementRunning();
        monitorPanel.pauseAutomaticAdvancement();
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Seleccionar JSON del sistema de archivos");
        if (chooser.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) {
            if (resume) monitorPanel.startAutomaticAdvancement();
            return;
        }
        Path diskPath = chooser.getSelectedFile().toPath();
        chooser.setDialogTitle("Seleccionar JSON de procesos y configuración");
        if (chooser.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) {
            if (resume) monitorPanel.startAutomaticAdvancement();
            return;
        }
        Path processPath = chooser.getSelectedFile().toPath();
        try {
            SimulationCluster loaded = Project2ConfigLoader.load(diskPath, processPath);
            monitorPanel.dispose();
            simulation.shutdown();
            simulation = loaded;
            activeDiskConfigPath = diskPath;
            activeProcessConfigPath = processPath;
            buildTabs();
            setSize(1280, 900);
            JOptionPane.showMessageDialog(this, "Proyecto 2 cargado. El disco avanzará con cada ciclo.");
        } catch (IOException | RuntimeException ex) {
            if (resume) monitorPanel.startAutomaticAdvancement();
            JOptionPane.showMessageDialog(this, "No se pudo cargar el Proyecto 2:\n" + ex.getMessage(),
                    "Error de carga", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void resetScenario() {
        try {
            SimulationCluster reset = activeDiskConfigPath == null
                    ? ConfigManager.loadSimulationFromJson(activeConfigPath.toString())
                    : Project2ConfigLoader.load(activeDiskConfigPath, activeProcessConfigPath);
            monitorPanel.dispose();
            simulation.shutdown();
            simulation = reset;
            buildTabs();
            JOptionPane.showMessageDialog(this,
                    "Escenario restaurado.",
                    "Simulación reiniciada", JOptionPane.INFORMATION_MESSAGE);
        } catch (IOException | RuntimeException ex) {
            LOGGER.log(Level.SEVERE, "No se pudo reiniciar el escenario activo.", ex);
            JOptionPane.showMessageDialog(this,
                    "No se pudo reiniciar la simulación:\n" + ex.getMessage()
                            + "\nLa simulación actual se conserva.",
                    "Error al reiniciar", JOptionPane.ERROR_MESSAGE);
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
        } catch (ClassNotFoundException | InstantiationException | IllegalAccessException
                 | UnsupportedLookAndFeelException ex) {
            LOGGER.log(Level.WARNING, "No se pudo aplicar el tema Nimbus; se usará el tema disponible.", ex);
        }

        SwingUtilities.invokeLater(() -> {
            MainFrame frame = new MainFrame();
            frame.setVisible(true);
        });
    }
}
