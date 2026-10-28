package avilaos.gui;

import avilaos.core.hardware.CPU;
import avilaos.core.hardware.RAM;
import avilaos.kernel.Kernel;
import avilaos.kernel.sync.Buffer;
import avilaos.model.enumeration.BlockReason;
import avilaos.model.enumeration.SchedulingPolicyType;
import avilaos.model.pcb.PCB;
import avilaos.util.datastructures.List;
import avilaos.util.datastructures.Queue;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;

public class MonitorPanel extends JPanel {

    private static final long BYTES_PER_MB = 1024L * 1024L;

    private final Kernel kernel;

    // Componentes del Control Superior
    private JLabel lblCurrentCycle;
    private JComboBox<SchedulingPolicyType> cbPolicy;
    private JButton btnAdvanceCycle;

    // Panel de CPU / Estado
    private JLabel lblCpuProcess;
    private JLabel lblCpuPC;
    private JLabel lblCpuPriority;
    private JLabel lblCpuDeadline;
    private JLabel lblCpuMode;
    private JProgressBar barMemory;

    // Modelos de Tablas de Colas
    private DefaultTableModel newQueueModel;
    private DefaultTableModel readyQueueModel;
    private DefaultTableModel blockedQueueModel;
    private DefaultTableModel termQueueModel;

    // Modelo de Tabla de Buffers
    private DefaultTableModel buffersModel;

    // Log de Eventos
    private JTextArea txtLog;

    public MonitorPanel(Kernel kernel) {
        this.kernel = kernel;
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        initComponents();
        applyDarkDashboardStyle();
        updateMonitor();
    }

    private void initComponents() {
        // --- PANEL SUPERIOR: Control de Ciclos y Políticas ---
        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 15, 5));
        topPanel.setBorder(BorderFactory.createTitledBorder("Control de Simulación"));

        lblCurrentCycle = new JLabel("Ciclo Actual: 0");
        lblCurrentCycle.setFont(new Font("Arial", Font.BOLD, 14));

        cbPolicy = new JComboBox<>(SchedulingPolicyType.values());
        cbPolicy.addActionListener(e -> changePolicy());

        btnAdvanceCycle = new JButton("Avanzar 1 Ciclo");
        btnAdvanceCycle.addActionListener(e -> advanceCycle());

        topPanel.add(lblCurrentCycle);
        topPanel.add(new JLabel("Política:"));
        topPanel.add(cbPolicy);
        topPanel.add(btnAdvanceCycle);

        add(topPanel, BorderLayout.NORTH);

        // --- PANEL CENTRAL: CPU, Memoria, Colas y Buffers ---
        JPanel centerPanel = new JPanel(new BorderLayout(10, 10));

        // CPU & Memoria
        JPanel statusPanel = new JPanel(new GridLayout(1, 2, 10, 0));

        // Estado CPU
        JPanel cpuPanel = new JPanel(new GridLayout(3, 2, 5, 5));
        cpuPanel.setBorder(BorderFactory.createTitledBorder("CPU (Proceso en Ejecución)"));
        lblCpuProcess = new JLabel("Proceso: Ninguno");
        lblCpuPC = new JLabel("PC: 0");
        lblCpuPriority = new JLabel("Prioridad: -");
        lblCpuDeadline = new JLabel("Deadline: -");
        lblCpuMode = new JLabel("Modo: OS");
        cpuPanel.add(lblCpuProcess);
        cpuPanel.add(lblCpuPC);
        cpuPanel.add(lblCpuPriority);
        cpuPanel.add(lblCpuDeadline);
        cpuPanel.add(lblCpuMode);

        // Estado Memoria
        JPanel memPanel = new JPanel(new BorderLayout(5, 5));
        memPanel.setBorder(BorderFactory.createTitledBorder("Ocupación de Memoria Principal (RAM)"));
        barMemory = new JProgressBar(0, 100);
        barMemory.setStringPainted(true);
        barMemory.setValue(0);
        barMemory.setString("0 MB / 1024 MB (0%)");
        memPanel.add(barMemory, BorderLayout.CENTER);

        statusPanel.add(cpuPanel);
        statusPanel.add(memPanel);

        centerPanel.add(statusPanel, BorderLayout.NORTH);

        // Tablas de Colas (NEW, READY, BLOCKED, TERMINATED)
        JPanel queuesPanel = new JPanel(new GridLayout(2, 2, 10, 10));

        newQueueModel = new DefaultTableModel(new String[]{"ID", "Nombre", "Prioridad", "Memoria (MB)"}, 0);
        readyQueueModel = new DefaultTableModel(new String[]{"ID", "Nombre", "Prioridad", "Restante"}, 0);
        blockedQueueModel = new DefaultTableModel(new String[]{"ID", "Nombre", "Motivo de Bloqueo"}, 0);
        termQueueModel = new DefaultTableModel(new String[]{"ID", "Nombre", "Estado"}, 0);

        queuesPanel.add(createTablePanel("Cola NEW", newQueueModel));
        queuesPanel.add(createTablePanel("Cola READY", readyQueueModel));
        queuesPanel.add(createTablePanel("Cola BLOCKED", blockedQueueModel));
        queuesPanel.add(createTablePanel("Cola TERMINATED", termQueueModel));

        centerPanel.add(queuesPanel, BorderLayout.CENTER);

        // Tablas de Buffers
        buffersModel = new DefaultTableModel(new String[]{"ID Buffer", "Host", "Ocupación", "Capacidad", "Bloqueados"}, 0);
        JPanel bufferContainer = createTablePanel("Estado de Buffers / Sincronización", buffersModel);
        bufferContainer.setPreferredSize(new Dimension(200, 110));

        centerPanel.add(bufferContainer, BorderLayout.SOUTH);

        add(centerPanel, BorderLayout.CENTER);

        // --- PANEL INFERIOR: Log de Eventos ---
        JPanel logPanel = new JPanel(new BorderLayout());
        logPanel.setBorder(BorderFactory.createTitledBorder("Log de Eventos del Sistema"));
        txtLog = new JTextArea(5, 50);
        txtLog.setEditable(false);
        txtLog.setFont(new Font("Monospaced", Font.PLAIN, 12));
        logPanel.add(new JScrollPane(txtLog), BorderLayout.CENTER);

        add(logPanel, BorderLayout.SOUTH);
    }

    private JPanel createTablePanel(String title, DefaultTableModel model) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createTitledBorder(title));
        JTable table = new JTable(model);
        table.setFillsViewportHeight(true);
        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.setPreferredSize(new Dimension(150, 110));
        panel.add(scrollPane, BorderLayout.CENTER);
        return panel;
    }

    private void advanceCycle() {
        if (kernel != null) {
            long nextCycle = kernel.getCurrentCycle() + 1;
            kernel.executeCycle(nextCycle);
            appendLog("Ciclo " + nextCycle + ": Ciclo de reloj ejecutado.");
            updateMonitor();
        }
    }

    private void changePolicy() {
        SchedulingPolicyType selectedPolicy = (SchedulingPolicyType) cbPolicy.getSelectedItem();
        if (selectedPolicy != null && kernel != null) {
            kernel.changeSchedulingPolicy(selectedPolicy, 2);
            appendLog("Política de planificación cambiada a: " + selectedPolicy.getDisplayName());
            updateMonitor();
        }
    }

    public void updateMonitor() {
        if (kernel == null) return;

        lblCurrentCycle.setText("Ciclo Actual: " + kernel.getCurrentCycle());

        // Actualizar datos de CPU de forma pasiva (sin alterar el planificador)
        CPU cpu = (kernel.getComputer() != null) ? kernel.getComputer().getCpu() : null;
        PCB currentPcb = (cpu != null) ? cpu.getCurrentProcess() : null;

        if (currentPcb != null) {
            lblCpuProcess.setText("Proceso: " + currentPcb.getName() + " (ID: " + currentPcb.getId() + ")");
            lblCpuPC.setText("PC: " + currentPcb.getProgramCounter());
            lblCpuPriority.setText("Prioridad: " + currentPcb.getPriority());
            lblCpuDeadline.setText("Deadline: " + currentPcb.getDeadline());
            lblCpuMode.setText("Modo: " + (cpu.getMode() != null ? cpu.getMode() : "Usuario"));
        } else {
            lblCpuProcess.setText("Proceso: Ninguno (IDLE)");
            lblCpuPC.setText("PC: 0");
            lblCpuPriority.setText("Prioridad: -");
            lblCpuDeadline.setText("Deadline: -");
            lblCpuMode.setText("Modo: Sistema Operativo");
        }

        // Actualizar ocupación de memoria RAM
        RAM ram = (kernel.getComputer() != null) ? kernel.getComputer().getRam() : null;
        // RAM guarda bytes internamente; la vista muestra MB
        long totalMem = (ram != null) ? ram.getTotalSize() / BYTES_PER_MB : 1024L;
        long usedMem = (ram != null) ? ram.getUsedSize() / BYTES_PER_MB : 0L;
        int percent = (totalMem > 0) ? (int) ((usedMem * 100) / totalMem) : 0;
        barMemory.setValue(Math.min(percent, 100));
        barMemory.setString(usedMem + " MB / " + totalMem + " MB (" + percent + "%)");

        // Actualizar Colas de Estados
        refreshQueueTable(kernel.getNewQueue(), newQueueModel, true);
        refreshQueueTable(kernel.getReadyQueue(), readyQueueModel, false);
        refreshQueueTable(kernel.getTerminatedQueue(), termQueueModel, false);
        refreshBlockedQueuesTable();

        // Actualizar Tabla de Buffers
        refreshBuffersTable();
    }

    private void refreshQueueTable(Queue<PCB> queue, DefaultTableModel model, boolean isNewQueue) {
        model.setRowCount(0);
        if (queue == null) return;

        int size = queue.size();
        for (int i = 0; i < size; i++) {
            PCB pcb = queue.dequeue();
            if (pcb != null) {
                if (isNewQueue) {
                    model.addRow(new Object[]{pcb.getId(), pcb.getName(), pcb.getPriority(), pcb.getMemoryRequired()});
                } else {
                    model.addRow(new Object[]{pcb.getId(), pcb.getName(), pcb.getPriority(), pcb.getRemainingTime()});
                }
                queue.enqueue(pcb);
            }
        }
    }

    private void refreshBlockedQueuesTable() {
        blockedQueueModel.setRowCount(0);
        if (kernel.getAllBlockedQueues() == null) return;

        List<Queue<PCB>> blockedList = kernel.getAllBlockedQueues();
        for (int i = 0; i < blockedList.size(); i++) {
            Queue<PCB> q = blockedList.get(i);
            if (q == null) continue;
            int qSize = q.size();
            for (int j = 0; j < qSize; j++) {
                PCB pcb = q.dequeue();
                if (pcb != null) {
                    String reason = (pcb.getBlockReason() != null) ? pcb.getBlockReason().name() : "I/O / Semáforo";
                    blockedQueueModel.addRow(new Object[]{pcb.getId(), pcb.getName(), reason});
                    q.enqueue(pcb);
                }
            }
        }
    }

    private void refreshBuffersTable() {
        buffersModel.setRowCount(0);
        if (kernel.getSyncManager() == null || kernel.getSyncManager().getAllBuffers() == null) return;

        List<Buffer> buffers = kernel.getSyncManager().getAllBuffers();
        for (int i = 0; i < buffers.size(); i++) {
            Buffer b = buffers.get(i);
            int blockedCount = 0;
            if (b.getSemEmpty() != null) blockedCount += b.getSemEmpty().getWaitingCount();
            if (b.getSemFull() != null) blockedCount += b.getSemFull().getWaitingCount();

            buffersModel.addRow(new Object[]{
                    b.getId(),
                    b.getHostComputerId(),
                    b.getCurrentSize(),
                    b.getCapacity(),
                    blockedCount
            });
        }
    }

    public void appendLog(String text) {
        txtLog.append(text + "\n");
        txtLog.setCaretPosition(txtLog.getDocument().getLength());
    }

    private void applyDarkDashboardStyle() {
        Color bgColor = new Color(24, 28, 36);         // Fondo principal oscuro (#181C24)
        Color cardColor = new Color(34, 40, 52);       // Fondo tarjetas (#222834)
        Color textColor = new Color(220, 225, 230);    // Texto claro
        Color accentColor = new Color(46, 204, 113);   // Verde lima

        Font mainFont = new Font("Segoe UI", Font.PLAIN, 12);
        Font titleFont = new Font("Segoe UI", Font.BOLD, 13);

        setBackground(bgColor);

        // Barra de memoria
        barMemory.setForeground(accentColor);
        barMemory.setBackground(new Color(50, 60, 75));
        barMemory.setFont(new Font("Segoe UI", Font.BOLD, 11));

        // Área de Log
        txtLog.setBackground(new Color(18, 22, 28));
        txtLog.setForeground(new Color(160, 220, 160));
        txtLog.setCaretColor(Color.WHITE);

        // Estilizar árbol de componentes
        styleComponentsDark(this, bgColor, cardColor, textColor, mainFont, titleFont);
    }

    private void styleComponentsDark(Component comp, Color bg, Color cardBg, Color text, Font font, Font titleFont) {
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
                styleComponentsDark(child, bg, cardBg, text, font, titleFont);
            }
        } else if (comp instanceof JScrollPane) {
            JScrollPane scroll = (JScrollPane) comp;
            scroll.getViewport().setBackground(cardBg);
            scroll.setBackground(cardBg);
            scroll.setBorder(BorderFactory.createLineBorder(new Color(55, 65, 80)));
            if (scroll.getPreferredSize().height < 90) {
                scroll.setPreferredSize(new Dimension(150, 110));
            }
            for (Component child : scroll.getComponents()) {
                styleComponentsDark(child, bg, cardBg, text, font, titleFont);
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
                table.getTableHeader().setBackground(new Color(44, 52, 68));
                table.getTableHeader().setForeground(text);
            }
        } else if (comp instanceof JLabel) {
            comp.setForeground(text);
            comp.setFont(font);
        } else if (comp instanceof JComboBox || comp instanceof JButton) {
            comp.setFont(font);
        }
    }
}
