package avilaos.gui;

import avilaos.kernel.Kernel;
import avilaos.kernel.sync.Buffer;
import avilaos.core.hardware.Computer;
import avilaos.model.enumeration.ProcessType;
import avilaos.model.enumeration.SchedulingPolicyType;
import avilaos.model.pcb.PCB;
import avilaos.simulation.SimulationCluster;
import avilaos.util.datastructures.List;
import avilaos.util.datastructures.Queue;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;

/**
 * Vista 1: Panel de Configuración editable para crear procesos y buffers.
 */
public class ConfigPanel extends JPanel {

    private final SimulationCluster simulation;
    private final Kernel legacyKernel;
    private DefaultTableModel processTableModel;
    private DefaultTableModel bufferTableModel;
    private DefaultTableModel computerTableModel;

    private JTextField txtName;
    private JComboBox<ProcessType> cbType;
    private JTextField txtPriority;
    private JTextField txtMemory;
    private JTextField txtInstructions;
    private JTextField txtDeadline;

    private JTextField txtBufferId;
    private JTextField txtBufferCapacity;
    private JComboBox<Integer> cbProcessComputer;
    private JComboBox<Integer> cbBufferHost;
    private JComboBox<String> cbProcessBuffer;
    private JTextField txtProduceConsumeInterval;
    private JTextField txtElementsRequired;
    private JTextField txtComputerId;
    private JTextField txtComputerRam;
    private JComboBox<SchedulingPolicyType> cbComputerPolicy;
    private JTextField txtComputerQuantum;
    private JTextField txtCycleDuration;
    private JTextField txtNetworkLatency;

    public ConfigPanel(Kernel kernel) {
        this.simulation = null;
        this.legacyKernel = kernel;
        init();
    }

    public ConfigPanel(SimulationCluster simulation) {
        this.simulation = simulation;
        this.legacyKernel = null;
        init();
    }

    private void init() {
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        initUI();
        refreshComputerChoices();
    }

    private void initUI() {
        JTabbedPane subTabs = new JTabbedPane();

        // Sub-pestaña 1: Gestión de Procesos
        subTabs.addTab("Procesos", createProcessManagerPanel());

        // Sub-pestaña 2: Gestión de Buffers
        subTabs.addTab("Buffers / Sincronización", createBufferManagerPanel());

        if (simulation != null) {
            subTabs.addTab("Computadores / Global", createComputerManagerPanel());
        }

        add(subTabs, BorderLayout.CENTER);

        // Aplicar el estilo oscuro
        applyDarkDashboardStyle();
    }

    private JPanel createProcessManagerPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));

        JPanel formPanel = new JPanel(new BorderLayout(10, 10));
        formPanel.setBorder(BorderFactory.createTitledBorder("Agregar Nuevo Proceso"));
        JPanel fieldsPanel = new JPanel(new GridLayout(0, 5, 12, 8));

        txtName = new JTextField("Proc_1", 7);
        cbType = new JComboBox<>(ProcessType.values());
        txtPriority = new JTextField("1", 3);
        txtMemory = new JTextField("64", 4);
        txtInstructions = new JTextField("100", 5);
        txtDeadline = new JTextField("50", 4);
        cbProcessComputer = new JComboBox<>();
        cbProcessBuffer = new JComboBox<>();
        txtProduceConsumeInterval = new JTextField("1", 3);
        txtElementsRequired = new JTextField("1", 3);
        fieldsPanel.add(createLabeledField("Nombre", txtName));
        fieldsPanel.add(createLabeledField("Tipo de proceso", cbType));
        fieldsPanel.add(createLabeledField("Prioridad", txtPriority));
        fieldsPanel.add(createLabeledField("Memoria (MB)", txtMemory));
        fieldsPanel.add(createLabeledField("Instrucciones", txtInstructions));
        fieldsPanel.add(createLabeledField("Deadline", txtDeadline));
        fieldsPanel.add(createLabeledField("Computador", cbProcessComputer));
        fieldsPanel.add(createLabeledField("Buffer", cbProcessBuffer));
        fieldsPanel.add(createLabeledField("Intervalo", txtProduceConsumeInterval));
        fieldsPanel.add(createLabeledField("Elementos", txtElementsRequired));
        formPanel.add(fieldsPanel, BorderLayout.CENTER);

        JButton btnAdd = new JButton("Agregar Proceso");
        btnAdd.addActionListener(e -> addProcess());
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        actions.add(btnAdd);
        formPanel.add(actions, BorderLayout.SOUTH);
        panel.add(formPanel, BorderLayout.NORTH);

        // Tabla de Procesos
        String[] columns = {"ID", "Nombre", "Computador", "Tipo", "Prioridad", "Memoria (MB)", "Instrucciones", "Deadline"};
        processTableModel = new DefaultTableModel(columns, 0);
        JTable table = new JTable(processTableModel);
        panel.add(new JScrollPane(table), BorderLayout.CENTER);

        return panel;
    }

    private JPanel createBufferManagerPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));

        JPanel formPanel = new JPanel(new BorderLayout(10, 10));
        formPanel.setBorder(BorderFactory.createTitledBorder("Crear Nuevo Buffer"));
        JPanel fieldsPanel = new JPanel(new GridLayout(1, 3, 12, 8));

        txtBufferId = new JTextField("buf_1", 8);
        txtBufferCapacity = new JTextField("10", 5);
        cbBufferHost = new JComboBox<>();
        fieldsPanel.add(createLabeledField("ID del buffer", txtBufferId));
        fieldsPanel.add(createLabeledField("Capacidad", txtBufferCapacity));
        fieldsPanel.add(createLabeledField("Computador host", cbBufferHost));
        formPanel.add(fieldsPanel, BorderLayout.CENTER);

        JButton btnAdd = new JButton("Crear Buffer");
        btnAdd.addActionListener(e -> addBuffer());
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        actions.add(btnAdd);
        formPanel.add(actions, BorderLayout.SOUTH);
        panel.add(formPanel, BorderLayout.NORTH);

        // Tabla de Buffers
        String[] columns = {"ID Buffer", "Equipo Host", "Capacidad"};
        bufferTableModel = new DefaultTableModel(columns, 0);
        JTable table = new JTable(bufferTableModel);
        panel.add(new JScrollPane(table), BorderLayout.CENTER);

        return panel;
    }

    private JPanel createComputerManagerPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        JPanel globalPanel = new JPanel(new GridLayout(1, 3, 12, 8));
        globalPanel.setBorder(BorderFactory.createTitledBorder("Configuración global"));
        txtCycleDuration = new JTextField(String.valueOf(simulation.getCycleDurationMs()), 6);
        txtNetworkLatency = new JTextField(String.valueOf(simulation.getNetworkLatencyCycles()), 6);
        globalPanel.add(createLabeledField("Duración del ciclo (ms)", txtCycleDuration));
        globalPanel.add(createLabeledField("Latencia de red (ciclos)", txtNetworkLatency));
        JButton applyGlobal = new JButton("Aplicar");
        applyGlobal.addActionListener(e -> applyGlobalSettings());
        globalPanel.add(applyGlobal);

        JPanel form = new JPanel(new GridLayout(1, 5, 12, 8));
        form.setBorder(BorderFactory.createTitledBorder("Agregar computador"));
        txtComputerId = new JTextField("1", 4);
        txtComputerRam = new JTextField("1024", 6);
        cbComputerPolicy = new JComboBox<>(SchedulingPolicyType.values());
        txtComputerQuantum = new JTextField("2", 4);
        form.add(createLabeledField("ID", txtComputerId));
        form.add(createLabeledField("RAM (MB)", txtComputerRam));
        form.add(createLabeledField("Política de planificación", cbComputerPolicy));
        form.add(createLabeledField("Quantum", txtComputerQuantum));
        JButton addComputer = new JButton("Agregar computador");
        addComputer.addActionListener(e -> addComputer());
        form.add(addComputer);

        JPanel top = new JPanel(new GridLayout(2, 1, 0, 10));
        top.add(globalPanel);
        top.add(form);
        panel.add(top, BorderLayout.NORTH);

        computerTableModel = new DefaultTableModel(
                new String[]{"ID", "RAM (MB)", "Política", "Quantum", "Ciclos", "CPU (%)", "Throughput"}, 0);
        panel.add(new JScrollPane(new JTable(computerTableModel)), BorderLayout.CENTER);
        refreshComputerChoices();
        return panel;
    }

    private JPanel createLabeledField(String label, JComponent field) {
        JPanel container = new JPanel(new BorderLayout(0, 5));
        container.add(new JLabel(label), BorderLayout.NORTH);
        container.add(field, BorderLayout.CENTER);
        return container;
    }

    private void applyGlobalSettings() {
        try {
            int duration = Integer.parseInt(txtCycleDuration.getText().trim());
            int latency = Integer.parseInt(txtNetworkLatency.getText().trim());
            if (duration <= 0 || latency < 0) {
                throw new IllegalArgumentException("La duración debe ser positiva y la latencia no negativa.");
            }
            simulation.setCycleDurationMs(duration);
            simulation.setNetworkLatencyCycles(latency);
            refreshTables();
            JOptionPane.showMessageDialog(this, "Configuración global aplicada.", "Éxito",
                    JOptionPane.INFORMATION_MESSAGE);
        } catch (RuntimeException ex) {
            JOptionPane.showMessageDialog(this, "Configuración global inválida: " + ex.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void addComputer() {
        try {
            int id = Integer.parseInt(txtComputerId.getText().trim());
            long ram = Long.parseLong(txtComputerRam.getText().trim());
            int quantum = Integer.parseInt(txtComputerQuantum.getText().trim());
            SchedulingPolicyType policy = (SchedulingPolicyType) cbComputerPolicy.getSelectedItem();
            simulation.addComputer(id, ram, policy, quantum);
            refreshTables();
            refreshComputerChoices();
        } catch (RuntimeException ex) {
            JOptionPane.showMessageDialog(this, "No se pudo agregar el computador: " + ex.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void refreshComputerChoices() {
        if (simulation == null || cbProcessComputer == null || cbBufferHost == null) return;
        Integer selectedProcess = (Integer) cbProcessComputer.getSelectedItem();
        Integer selectedHost = (Integer) cbBufferHost.getSelectedItem();
        cbProcessComputer.removeAllItems();
        cbBufferHost.removeAllItems();
        if (simulation != null) {
            for (int i = 0; i < simulation.getComputers().size(); i++) {
                int id = simulation.getComputers().get(i).getId();
                cbProcessComputer.addItem(id);
                cbBufferHost.addItem(id);
            }
        } else if (legacyKernel.getComputer() != null) {
            cbProcessComputer.addItem(legacyKernel.getComputer().getId());
            cbBufferHost.addItem(legacyKernel.getComputer().getId());
        }
        if (selectedProcess != null) cbProcessComputer.setSelectedItem(selectedProcess);
        if (selectedHost != null) cbBufferHost.setSelectedItem(selectedHost);

        if (cbProcessBuffer != null) {
            String selectedBuffer = (String) cbProcessBuffer.getSelectedItem();
            cbProcessBuffer.removeAllItems();
            List<Buffer> availableBuffers = getSyncManager().getAllBuffers();
            for (int i = 0; i < availableBuffers.size(); i++) {
                cbProcessBuffer.addItem(availableBuffers.get(i).getId());
            }
            if (selectedBuffer != null) cbProcessBuffer.setSelectedItem(selectedBuffer);
        }
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

            int compId = simulation != null
                    ? (Integer) cbProcessComputer.getSelectedItem()
                    : (legacyKernel.getComputer() != null ? legacyKernel.getComputer().getId() : 0);
            String bufferId = null;
            int interval = 0;
            int elements = 0;
            if (selectedType == ProcessType.PRODUCER || selectedType == ProcessType.CONSUMER) {
                bufferId = (String) cbProcessBuffer.getSelectedItem();
                interval = Integer.parseInt(txtProduceConsumeInterval.getText().trim());
                elements = Integer.parseInt(txtElementsRequired.getText().trim());
                if (bufferId == null) throw new IllegalArgumentException("Seleccione o cree un buffer.");
                if (interval <= 0 || elements <= 0 || getSyncManager().getBuffer(bufferId) == null) {
                    throw new IllegalArgumentException("El buffer debe existir y los valores deben ser positivos.");
                }
            }
            PCB pcb = new PCB(name, compId, selectedType, priority, memory, deadline, inst,
                    bufferId, interval, elements);

            if (simulation != null) simulation.submitProcess(pcb);
            else legacyKernel.submitProcess(pcb);

            processTableModel.addRow(new Object[]{
                    pcb.getId(), pcb.getName(), compId, selectedType, priority, memory, inst, deadline
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
            int hostId = simulation != null
                    ? (Integer) cbBufferHost.getSelectedItem()
                    : (legacyKernel.getComputer() != null ? legacyKernel.getComputer().getId() : 0);

            if (simulation != null) {
                simulation.createBuffer(bufId, capacity, hostId);
                bufferTableModel.addRow(new Object[]{bufId, hostId, capacity});
                refreshComputerChoices();
                JOptionPane.showMessageDialog(this, "Buffer creado exitosamente.", "Éxito", JOptionPane.INFORMATION_MESSAGE);
            } else if (legacyKernel.getSyncManager() != null) {
                if (legacyKernel.getSyncManager().getBuffer(bufId) != null) {
                    throw new IllegalArgumentException("Ya existe el buffer " + bufId + ".");
                }
                legacyKernel.getSyncManager().createBuffer(bufId, capacity, hostId);
                bufferTableModel.addRow(new Object[]{bufId, hostId, capacity});
                refreshComputerChoices();
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
        if (simulation == null && legacyKernel == null) return;

        // Refrescar Buffers
        bufferTableModel.setRowCount(0);
        if (getSyncManager() != null && getSyncManager().getAllBuffers() != null) {
            List<Buffer> buffers = getSyncManager().getAllBuffers();
            for (int i = 0; i < buffers.size(); i++) {
                Buffer b = buffers.get(i);
                bufferTableModel.addRow(new Object[]{b.getId(), b.getHostComputerId(), b.getCapacity()});
            }
        }

        // Refrescar Procesos
        processTableModel.setRowCount(0);
        if (simulation != null) {
            for (int i = 0; i < simulation.getComputers().size(); i++) {
                refreshKernelProcesses(simulation.getComputers().get(i).getKernel());
            }
            refreshComputerTable();
        } else {
            refreshKernelProcesses(legacyKernel);
        }
    }

    private void refreshKernelProcesses(Kernel kernel) {
        refreshProcessesFromQueue(kernel.getNewQueue());
        refreshProcessesFromQueue(kernel.getReadyQueue());
        refreshProcessesFromQueue(kernel.getTerminatedQueue());
        refreshProcessesFromQueue(kernel.getAllBlockedQueues());
        PCB running = kernel.getComputer().getCpu().getCurrentProcess();
        if (running != null) {
            processTableModel.addRow(new Object[]{running.getId(), running.getName(),
                    running.getComputerId(), running.getType(), running.getPriority(),
                    running.getMemoryRequired(), running.getRemainingTime(), running.getDeadline()});
        }
    }

    private void refreshProcessesFromQueue(List<Queue<PCB>> queues) {
        for (int i = 0; i < queues.size(); i++) refreshProcessesFromQueue(queues.get(i));
    }

    private void refreshComputerTable() {
        computerTableModel.setRowCount(0);
        for (int i = 0; i < simulation.getComputers().size(); i++) {
            Computer computer = simulation.getComputers().get(i);
            Kernel kernel = computer.getKernel();
            computerTableModel.addRow(new Object[]{
                    computer.getId(), computer.getRam().getTotalSize() / (1024L * 1024L),
                    kernel.getScheduler().getPolicy().getType(), kernel.getQuantum(),
                    kernel.getCyclesExecuted(), String.format("%.2f", kernel.getCpuUtilization()),
                    String.format("%.4f", kernel.getThroughput())
            });
        }
    }

    private avilaos.kernel.sync.SyncManager getSyncManager() {
        return simulation != null ? simulation.getSyncManager() : legacyKernel.getSyncManager();
    }

    private void refreshProcessesFromQueue(Queue<PCB> queue) {
        if (queue == null) return;
        int size = queue.size();
        for (int i = 0; i < size; i++) {
            PCB p = queue.dequeue();
            if (p != null) {
                processTableModel.addRow(new Object[]{
                        p.getId(), p.getName(), p.getComputerId(), p.getType(), p.getPriority(),
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
