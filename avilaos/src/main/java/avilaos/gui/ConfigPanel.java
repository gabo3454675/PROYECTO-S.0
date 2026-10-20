package avilaos.gui;

import avilaos.kernel.Kernel;
import avilaos.model.enumeration.ProcessType;
import avilaos.model.pcb.PCB;

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
    private JTextField txtPriority;
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
        txtName = new JTextField("Proc_1", 8);
        formPanel.add(txtName);

        formPanel.add(new JLabel("Prioridad:"));
        txtPriority = new JTextField("1", 4);
        formPanel.add(txtPriority);

        formPanel.add(new JLabel("Instrucciones:"));
        txtInstructions = new JTextField("100", 5);
        formPanel.add(txtInstructions);

        formPanel.add(new JLabel("Deadline:"));
        txtDeadline = new JTextField("50", 5);
        formPanel.add(txtDeadline);

        JButton btnAdd = new JButton("Agregar Proceso");
        btnAdd.addActionListener(e -> addProcess());
        formPanel.add(btnAdd);

        panel.add(formPanel, BorderLayout.NORTH);

        // Tabla de Procesos
        String[] columns = {"ID", "Nombre", "Tipo", "Prioridad", "Instrucciones", "Deadline"};
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
            int priority = Integer.parseInt(txtPriority.getText().trim());
            long inst = Long.parseLong(txtInstructions.getText().trim());
            long deadline = Long.parseLong(txtDeadline.getText().trim());

            ProcessType defaultType = (ProcessType.values().length > 0) ? ProcessType.values()[0] : null;
            PCB pcb = new PCB(name, 0, defaultType, priority, inst, deadline, deadline);

            kernel.submitProcess(pcb);

            processTableModel.addRow(new Object[]{
                    pcb.getId(), pcb.getName(), defaultType, priority, inst, deadline
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

            if (kernel.getSyncManager() != null) {
                kernel.getSyncManager().createBuffer(bufId, 0, capacity);
                bufferTableModel.addRow(new Object[]{bufId, 0, capacity});
                JOptionPane.showMessageDialog(this, "Buffer creado exitosamente.", "Éxito", JOptionPane.INFORMATION_MESSAGE);
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Error al crear el buffer: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
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