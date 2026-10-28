package avilaos.gui;

import avilaos.kernel.Kernel;
import avilaos.kernel.sync.Buffer;
import avilaos.model.enumeration.ProcessType;
import avilaos.model.pcb.PCB;
import avilaos.util.datastructures.List;
import avilaos.util.datastructures.Queue;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;

/**
 * Vista 1: Panel de Configuración editable para crear procesos y buffers.
 */
public class ConfigPanel extends JPanel {

    private final Kernel kernel;
    private DefaultTableModel processTableModel;
    private DefaultTableModel bufferTableModel;

    private JTextField txtName;
    private JComboBox<ProcessType> cbType;
    private JTextField txtPriority;
    private JTextField txtMemory;
    private JTextField txtInstructions;
    private JTextField txtDeadline;

    private JTextField txtBufferId;
    private JTextField txtBufferCapacity;

    public ConfigPanel(Kernel kernel) {
        this.kernel = kernel;
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        initUI();
    }

    private void initUI() {
        JTabbedPane subTabs = new JTabbedPane();

        // Sub-pestaña 1: Gestión de Procesos
        subTabs.addTab("Procesos", createProcessManagerPanel());

        // Sub-pestaña 2: Gestión de Buffers
        subTabs.addTab("Buffers / Sincronización", createBufferManagerPanel());

        add(subTabs, BorderLayout.CENTER);

        // Aplicar el estilo oscuro
        applyDarkDashboardStyle();
    }

    private JPanel createProcessManagerPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));

        // Formulario Superior
        JPanel formPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        formPanel.setBorder(BorderFactory.createTitledBorder("Agregar Nuevo Proceso"));

        formPanel.add(new JLabel("Nombre:"));
        txtName = new JTextField("Proc_1", 7);
        formPanel.add(txtName);

        formPanel.add(new JLabel("Tipo:"));
        cbType = new JComboBox<>(ProcessType.values());
        formPanel.add(cbType);

        formPanel.add(new JLabel("Prioridad:"));
        txtPriority = new JTextField("1", 3);
        formPanel.add(txtPriority);

        formPanel.add(new JLabel("Memoria (MB):"));
        txtMemory = new JTextField("64", 4);
        formPanel.add(txtMemory);

        formPanel.add(new JLabel("Instrucciones:"));
        txtInstructions = new JTextField("100", 5);
        formPanel.add(txtInstructions);

        formPanel.add(new JLabel("Deadline:"));
        txtDeadline = new JTextField("50", 4);
        formPanel.add(txtDeadline);

        JButton btnAdd = new JButton("Agregar Proceso");
        btnAdd.addActionListener(e -> addProcess());
        formPanel.add(btnAdd);

        panel.add(formPanel, BorderLayout.NORTH);

        // Tabla de Procesos
        String[] columns = {"ID", "Nombre", "Tipo", "Prioridad", "Memoria (MB)", "Instrucciones", "Deadline"};
        processTableModel = new DefaultTableModel(columns, 0);
        JTable table = new JTable(processTableModel);
        panel.add(new JScrollPane(table), BorderLayout.CENTER);

        return panel;
    }

    private JPanel createBufferManagerPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));

        // Formulario Superior
        JPanel formPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        formPanel.setBorder(BorderFactory.createTitledBorder("Crear Nuevo Buffer"));

        formPanel.add(new JLabel("Buffer ID:"));
        txtBufferId = new JTextField("buf_1", 8);
        formPanel.add(txtBufferId);

        formPanel.add(new JLabel("Capacidad:"));
        txtBufferCapacity = new JTextField("10", 5);
        formPanel.add(txtBufferCapacity);

        JButton btnAdd = new JButton("Crear Buffer");
        btnAdd.addActionListener(e -> addBuffer());
        formPanel.add(btnAdd);

        panel.add(formPanel, BorderLayout.NORTH);

        // Tabla de Buffers
        String[] columns = {"ID Buffer", "Equipo Host", "Capacidad"};
        bufferTableModel = new DefaultTableModel(columns, 0);
        JTable table = new JTable(bufferTableModel);
        panel.add(new JScrollPane(table), BorderLayout.CENTER);

        return panel;
    }

    private void addProcess() {
        try {
            String name = txtName.getText().trim();
            ProcessType selectedType = (ProcessType) cbType.getSelectedItem();
            if (selectedType == null) selectedType = ProcessType.CPU_BOUND;

            int priority = Integer.parseInt(txtPriority.getText().trim());
            long memory = Long.parseLong(txtMemory.getText().trim());
            long inst = Long.parseLong(txtInstructions.getText().trim());
            long deadline = Long.parseLong(txtDeadline.getText().trim());

            int compId = (kernel.getComputer() != null) ? kernel.getComputer().getId() : 0;
            PCB pcb = new PCB(name, compId, selectedType, priority, memory, deadline, inst);

            kernel.submitProcess(pcb);

            processTableModel.addRow(new Object[]{
                    pcb.getId(), pcb.getName(), selectedType, priority, memory, inst, deadline
            });

            JOptionPane.showMessageDialog(this, "Proceso agregado exitosamente.", "Éxito", JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Error en los datos del proceso: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void addBuffer() {
        try {
            String bufId = txtBufferId.getText().trim();
            int capacity = Integer.parseInt(txtBufferCapacity.getText().trim());
            int hostId = (kernel.getComputer() != null) ? kernel.getComputer().getId() : 0;

            if (kernel.getSyncManager() != null) {
                kernel.getSyncManager().createBuffer(bufId, capacity, hostId);
                bufferTableModel.addRow(new Object[]{bufId, hostId, capacity});
                JOptionPane.showMessageDialog(this, "Buffer creado exitosamente.", "Éxito", JOptionPane.INFORMATION_MESSAGE);
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Error al crear el buffer: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * Sincroniza las tablas de Vista 1 con el estado actual del Kernel.
     */
    public void refreshTables() {
        if (kernel == null) return;

        // Refrescar Buffers
        bufferTableModel.setRowCount(0);
        if (kernel.getSyncManager() != null && kernel.getSyncManager().getAllBuffers() != null) {
            List<Buffer> buffers = kernel.getSyncManager().getAllBuffers();
            for (int i = 0; i < buffers.size(); i++) {
                Buffer b = buffers.get(i);
                bufferTableModel.addRow(new Object[]{b.getId(), b.getHostComputerId(), b.getCapacity()});
            }
        }

        // Refrescar Procesos
        processTableModel.setRowCount(0);
        refreshProcessesFromQueue(kernel.getNewQueue());
        refreshProcessesFromQueue(kernel.getReadyQueue());
        refreshProcessesFromQueue(kernel.getTerminatedQueue());
    }

    private void refreshProcessesFromQueue(Queue<PCB> queue) {
        if (queue == null) return;
        int size = queue.size();
        for (int i = 0; i < size; i++) {
            PCB p = queue.dequeue();
            if (p != null) {
                processTableModel.addRow(new Object[]{
                        p.getId(), p.getName(), p.getType(), p.getPriority(),
                        p.getMemoryRequired(), p.getRemainingTime(), p.getDeadline()
                });
                queue.enqueue(p);
            }
        }
    }

    private void applyDarkDashboardStyle() {
        Color bgColor = new Color(24, 28, 36);         // Fondo principal oscuro (#181C24)
        Color cardColor = new Color(34, 40, 52);       // Fondo de tarjetas (#222834)
        Color textColor = new Color(220, 225, 230);    // Texto claro
        Color tableHeaderColor = new Color(44, 52, 68);

        Font mainFont = new Font("Segoe UI", Font.PLAIN, 12);
        Font titleFont = new Font("Segoe UI", Font.BOLD, 13);

        setBackground(bgColor);

        styleComponentsDark(this, bgColor, cardColor, textColor, tableHeaderColor, mainFont, titleFont);
    }

    private void styleComponentsDark(Component comp, Color bg, Color cardBg, Color text, Color headerBg, Font font, Font titleFont) {
        if (comp instanceof JPanel) {
            JPanel p = (JPanel) comp;
            p.setBackground(cardBg);
            if (p.getBorder() instanceof javax.swing.border.TitledBorder) {
                javax.swing.border.TitledBorder border = (javax.swing.border.TitledBorder) p.getBorder();
                border.setTitleFont(titleFont);
                border.setTitleColor(text);
                border.setBorder(BorderFactory.createLineBorder(new Color(55, 65, 80)));
            }
            for (Component child : p.getComponents()) {
                styleComponentsDark(child, bg, cardBg, text, headerBg, font, titleFont);
            }
        } else if (comp instanceof JTabbedPane) {
            JTabbedPane tabs = (JTabbedPane) comp;
            tabs.setBackground(cardBg);
            tabs.setForeground(text);
            tabs.setFont(titleFont);
            for (Component child : tabs.getComponents()) {
                styleComponentsDark(child, bg, cardBg, text, headerBg, font, titleFont);
            }
        } else if (comp instanceof JScrollPane) {
            JScrollPane scroll = (JScrollPane) comp;
            scroll.getViewport().setBackground(cardBg);
            scroll.setBackground(cardBg);
            scroll.setBorder(BorderFactory.createLineBorder(new Color(55, 65, 80)));
            for (Component child : scroll.getComponents()) {
                styleComponentsDark(child, bg, cardBg, text, headerBg, font, titleFont);
            }
        } else if (comp instanceof JTable) {
            JTable table = (JTable) comp;
            table.setBackground(cardBg);
            table.setForeground(text);
            table.setFont(font);
            table.setRowHeight(24);
            table.setFillsViewportHeight(true);
            table.setShowGrid(true);
            table.setGridColor(new Color(55, 65, 80));

            if (table.getTableHeader() != null) {
                table.getTableHeader().setFont(titleFont);
                table.getTableHeader().setBackground(headerBg);
                table.getTableHeader().setForeground(text);
            }
        } else if (comp instanceof JLabel) {
            comp.setForeground(text);
            comp.setFont(font);
        } else if (comp instanceof JTextField || comp instanceof JComboBox || comp instanceof JButton) {
            comp.setFont(font);
        }
    }
}
