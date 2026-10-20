package avilaos.gui;

import avilaos.config.ConfigManager;
import avilaos.core.hardware.Computer;
import avilaos.kernel.Kernel;
import avilaos.model.enumeration.SchedulingPolicyType;

import javax.swing.*;
import java.io.File;
import java.io.IOException;

/**
 * Ventana Principal de ÁvilaOS con pestañas para Vista 1 (Configuración) y Vista 2 (Monitor).
 */
public class MainFrame extends JFrame {

    private final Kernel kernel;
    private ConfigPanel configPanel;
    private MonitorPanel monitorPanel;

    public MainFrame() {
        Computer computer = new Computer(0, 1024L, SchedulingPolicyType.FCFS, 2);
        this.kernel = new Kernel(computer, SchedulingPolicyType.FCFS, 2);

        initUI();
    }

    private void initUI() {
        setTitle("ÁvilaOS - Simulador de Sistema Operativo Distribuido");
        setSize(1000, 780);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        createMenuBar();

        JTabbedPane tabbedPane = new JTabbedPane();

        configPanel = new ConfigPanel(kernel);
        monitorPanel = new MonitorPanel(kernel);

        tabbedPane.addTab("Vista 1: Configuración", configPanel);
        tabbedPane.addTab("Vista 2: Monitor en Tiempo Real", monitorPanel);

        // Al cambiar a la pestaña de monitor, actualizar datos
        tabbedPane.addChangeListener(e -> {
            if (tabbedPane.getSelectedIndex() == 1) {
                monitorPanel.updateMonitor();
            }
        });

        add(tabbedPane);
    }

    private void createMenuBar() {
        JMenuBar menuBar = new JMenuBar();
        JMenu fileMenu = new JMenu("Archivo");

        JMenuItem saveItem = new JMenuItem("Guardar Configuración (JSON)...");
        saveItem.addActionListener(e -> saveConfiguration());

        JMenuItem loadItem = new JMenuItem("Cargar Configuración (JSON)...");
        loadItem.addActionListener(e -> loadConfiguration());

        JMenuItem exitItem = new JMenuItem("Salir");
        exitItem.addActionListener(e -> System.exit(0));

        fileMenu.add(saveItem);
        fileMenu.add(loadItem);
        fileMenu.addSeparator();
        fileMenu.add(exitItem);

        menuBar.add(fileMenu);
        setJMenuBar(menuBar);
    }

    private void saveConfiguration() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Guardar Estado de ÁvilaOS");
        if (fileChooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            File fileToSave = fileChooser.getSelectedFile();
            String path = fileToSave.getAbsolutePath();
            if (!path.endsWith(".json")) path += ".json";

            try {
                ConfigManager.saveConfigToJson(path, kernel);
                JOptionPane.showMessageDialog(this, "Configuración guardada en:\n" + path, "Éxito", JOptionPane.INFORMATION_MESSAGE);
            } catch (IOException ex) {
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
                ConfigManager.loadConfigFromJson(fileToLoad.getAbsolutePath(), kernel);
                monitorPanel.updateMonitor();
                JOptionPane.showMessageDialog(this, "Configuración cargada con éxito.", "Éxito", JOptionPane.INFORMATION_MESSAGE);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Error al cargar: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

public static void main(String[] args) {
    try {
        // Look and Feel nativo oscuro de Swing / FlatLaf si está disponible
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