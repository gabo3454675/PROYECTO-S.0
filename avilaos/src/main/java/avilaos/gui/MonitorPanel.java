package avilaos.gui;

import avilaos.core.hardware.CPU;
import avilaos.core.hardware.RAM;
import avilaos.core.hardware.Computer;
import avilaos.kernel.Kernel;
import avilaos.kernel.sync.Buffer;
import avilaos.model.enumeration.BlockReason;
import avilaos.model.enumeration.SchedulingPolicyType;
import avilaos.model.pcb.PCB;
import avilaos.simulation.SimulationCluster;
import avilaos.util.datastructures.List;
import avilaos.util.datastructures.Queue;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableModel;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import javax.imageio.ImageIO;

public class MonitorPanel extends JPanel {

    private static final long BYTES_PER_MB = 1024L * 1024L;

    private final SimulationCluster simulation;
    private Kernel kernel;

    // Componentes del Control Superior
    private JLabel lblCurrentCycle;
    private JComboBox<SchedulingPolicyType> cbPolicy;
    private JButton btnAdvanceCycle;
    private JComboBox<Integer> cbComputer;
    private JLabel lblCpuUtilization;
    private JLabel lblThroughput;
    private boolean updatingControls;

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
        this.simulation = null;
        this.kernel = kernel;
        initialize();
    }

    public MonitorPanel(SimulationCluster simulation) {
        if (simulation == null || simulation.getComputers().isEmpty()) {
            throw new IllegalArgumentException("La simulación debe tener al menos un computador.");
        }
        this.simulation = simulation;
        this.kernel = simulation.getComputers().get(0).getKernel();
        initialize();
    }

    private void initialize() {
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

        if (simulation != null) {
            cbComputer = new JComboBox<>();
            for (Computer computer : simulation.getComputers()) cbComputer.addItem(computer.getId());
            cbComputer.setSelectedItem(kernel.getComputer().getId());
            cbComputer.addActionListener(e -> selectComputer());
            topPanel.add(new JLabel("Computador:"));
            topPanel.add(cbComputer);
            lblCpuUtilization = new JLabel("CPU global: 0.00%");
            lblThroughput = new JLabel("Throughput global: 0.0000 procesos/ciclo");
            topPanel.add(lblCpuUtilization);
            topPanel.add(lblThroughput);
        }

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
        if (simulation != null) {
            simulation.advanceCycle();
            long nextCycle = simulation.getCurrentCycle();
            appendLog("Ciclo " + nextCycle + ": ciclo global ejecutado en "
                    + simulation.getComputers().size() + " computadores.");
            updateMonitor();
        } else if (kernel != null) {
            long nextCycle = kernel.getCurrentCycle() + 1;
            kernel.executeCycle(nextCycle);
            appendLog("Ciclo " + nextCycle + ": Ciclo de reloj ejecutado.");
            updateMonitor();
        }
    }

    private void selectComputer() {
        if (updatingControls || simulation == null) return;
        Integer computerId = (Integer) cbComputer.getSelectedItem();
        if (computerId == null) return;
        Kernel selected = simulation.getKernel(computerId);
        if (selected != null) {
            kernel = selected;
            updateMonitor();
        }
    }

    private void changePolicy() {
        if (updatingControls) return;
        SchedulingPolicyType selectedPolicy = (SchedulingPolicyType) cbPolicy.getSelectedItem();
        if (selectedPolicy != null && kernel != null) {
            kernel.changeSchedulingPolicy(selectedPolicy, kernel.getQuantum());
            appendLog("Política de planificación cambiada a: " + selectedPolicy.getDisplayName());
            updateMonitor();
        }
    }

    public void updateMonitor() {
        if (kernel == null) return;

        lblCurrentCycle.setText("Ciclo Actual: "
                + (simulation == null ? kernel.getCurrentCycle() : simulation.getCurrentCycle()));
        updatingControls = true;
        if (simulation != null) {
            for (Computer computer : simulation.getComputers()) {
                boolean present = false;
                for (int i = 0; i < cbComputer.getItemCount(); i++) {
                    if (cbComputer.getItemAt(i) == computer.getId()) {
                        present = true;
                        break;
                    }
                }
                if (!present) cbComputer.addItem(computer.getId());
            }
            cbComputer.setSelectedItem(kernel.getComputer().getId());
        }
        cbPolicy.setSelectedItem(kernel.getScheduler().getPolicy().getType());
        updatingControls = false;
        updateGlobalMetrics();

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

    private void updateGlobalMetrics() {
        if (simulation == null) return;
        lblCpuUtilization.setText(String.format("CPU global: %.2f%%", getGlobalCpuUtilization()));
        lblThroughput.setText(String.format("Throughput global: %.4f procesos/ciclo", getGlobalThroughput()));
    }

    private double getGlobalCpuUtilization() {
        long totalCycles = 0;
        double weightedUtilization = 0;
        for (Computer computer : simulation.getComputers()) {
            Kernel computerKernel = computer.getKernel();
            long cycles = computerKernel.getCyclesExecuted();
            totalCycles += cycles;
            weightedUtilization += computerKernel.getCpuUtilization() * cycles;
        }
        return totalCycles == 0 ? 0 : weightedUtilization / totalCycles;
    }

    private double getGlobalThroughput() {
        if (simulation == null || simulation.getCurrentCycle() == 0) return 0;
        long completed = 0;
        for (Computer computer : simulation.getComputers()) {
            completed += computer.getKernel().getTerminatedQueue().size();
        }
        return (double) completed / simulation.getCurrentCycle();
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
        avilaos.kernel.sync.SyncManager syncManager = simulation == null
                ? kernel.getSyncManager() : simulation.getSyncManager();
        if (syncManager == null || syncManager.getAllBuffers() == null) return;

        List<Buffer> buffers = syncManager.getAllBuffers();
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

    public void exportToPng(File file) throws IOException {
        requireFile(file);

        Dimension previousSize = getSize();
        int width = Math.max(getWidth(), 1200);
        int height = Math.max(getHeight(), 800);
        setSize(width, height);
        layoutTree(this);

        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = image.createGraphics();
        try {
            printAll(graphics);
        } finally {
            graphics.dispose();
            if (previousSize.width > 0 && previousSize.height > 0) {
                setSize(previousSize);
                layoutTree(this);
            }
        }

        if (!ImageIO.write(image, "png", file)) {
            throw new IOException("No hay un escritor PNG disponible.");
        }
    }

    public void exportToCsv(File file) throws IOException {
        requireFile(file);
        StringBuilder csv = new StringBuilder();
        appendCsvRow(csv, "Sección", "Fila", "Campo", "Valor");
        appendCsvRow(csv, "General", "0", "Ciclo actual",
                simulation == null ? kernel.getCurrentCycle() : simulation.getCurrentCycle());
        appendCsvRow(csv, "General", "0", "Computador seleccionado", kernel.getComputer().getId());
        appendCsvRow(csv, "General", "0", "Política", kernel.getScheduler().getPolicy().getType());
        appendCsvRow(csv, "Métricas", "0", "Utilización CPU (%)", kernel.getCpuUtilization());
        appendCsvRow(csv, "Métricas", "0", "Throughput (procesos/ciclo)", kernel.getThroughput());
        if (simulation != null) {
            appendCsvRow(csv, "Métricas globales", "0", "Utilización CPU (%)", getGlobalCpuUtilization());
            appendCsvRow(csv, "Métricas globales", "0", "Throughput (procesos/ciclo)", getGlobalThroughput());
        }
        appendCsvRow(csv, "CPU", "0", "Proceso en ejecución",
                kernel.getComputer().getCpu().getCurrentProcess() == null
                        ? "Ninguno"
                        : kernel.getComputer().getCpu().getCurrentProcess().getName());
        appendCsvRow(csv, "Memoria", "0", "Usada (MB)",
                kernel.getComputer().getRam().getUsedSize() / BYTES_PER_MB);
        appendCsvRow(csv, "Memoria", "0", "Total (MB)",
                kernel.getComputer().getRam().getTotalSize() / BYTES_PER_MB);
        appendCsvTable(csv, "NEW", newQueueModel);
        appendCsvTable(csv, "READY", readyQueueModel);
        appendCsvTable(csv, "BLOCKED", blockedQueueModel);
        appendCsvTable(csv, "TERMINATED", termQueueModel);
        appendCsvTable(csv, "Buffers", buffersModel);
        appendCsvRow(csv, "Log", "0", "Eventos", txtLog.getText());
        Files.writeString(file.toPath(), csv.toString(), StandardCharsets.UTF_8);
    }

    public void exportToHtml(File file) throws IOException {
        requireFile(file);
        StringBuilder html = new StringBuilder(4096);
        html.append("<!doctype html><html lang=\"es\"><head><meta charset=\"UTF-8\">")
                .append("<meta name=\"viewport\" content=\"width=device-width, initial-scale=1\">")
                .append("<title>Monitor ÁvilaOS</title><style>")
                .append("body{font:14px Segoe UI,Arial,sans-serif;background:#181c24;color:#dce1e6;margin:2rem}")
                .append("h1,h2{color:#2ecc71}table{border-collapse:collapse;width:100%;margin:1rem 0 2rem}")
                .append("th,td{border:1px solid #374150;padding:.5rem;text-align:left}")
                .append("th{background:#2c3444}caption{text-align:left;font-size:1.2rem;margin:.5rem 0}")
                .append("pre{white-space:pre-wrap;background:#12161c;padding:1rem}</style></head><body>")
                .append("<h1>Monitor de ÁvilaOS</h1><p>Ciclo actual: ")
                .append(simulation == null ? kernel.getCurrentCycle() : simulation.getCurrentCycle())
                .append("</p><p>Computador: ").append(kernel.getComputer().getId())
                .append("</p><p>Política: ")
                .append(htmlEscape(kernel.getScheduler().getPolicy().getType().getDisplayName()))
                .append("</p><p>Utilización CPU: ")
                .append(String.format("%.2f%%", kernel.getCpuUtilization()))
                .append(" · Throughput: ")
                .append(String.format("%.4f procesos/ciclo", kernel.getThroughput()));
        if (simulation != null) {
            html.append("</p><p>Métricas globales — CPU: ")
                    .append(String.format("%.2f%%", getGlobalCpuUtilization()))
                    .append(" · Throughput: ")
                    .append(String.format("%.4f procesos/ciclo", getGlobalThroughput()));
        }
        html.append("</p><p>Memoria: ")
                .append(kernel.getComputer().getRam().getUsedSize() / BYTES_PER_MB)
                .append(" MB / ")
                .append(kernel.getComputer().getRam().getTotalSize() / BYTES_PER_MB)
                .append(" MB</p>");
        appendHtmlTable(html, "Cola NEW", newQueueModel);
        appendHtmlTable(html, "Cola READY", readyQueueModel);
        appendHtmlTable(html, "Cola BLOCKED", blockedQueueModel);
        appendHtmlTable(html, "Cola TERMINATED", termQueueModel);
        appendHtmlTable(html, "Buffers", buffersModel);
        html.append("<h2>Eventos</h2><pre>").append(htmlEscape(txtLog.getText()))
                .append("</pre></body></html>");
        Files.writeString(file.toPath(), html.toString(), StandardCharsets.UTF_8);
    }

    private static void requireFile(File file) {
        if (file == null) {
            throw new IllegalArgumentException("El archivo de exportación no puede ser null.");
        }
    }

    private static void layoutTree(Container container) {
        container.doLayout();
        for (Component child : container.getComponents()) {
            if (child instanceof Container nested) {
                layoutTree(nested);
            }
        }
    }

    private static void appendCsvTable(StringBuilder csv, String section, TableModel model) {
        for (int row = 0; row < model.getRowCount(); row++) {
            for (int column = 0; column < model.getColumnCount(); column++) {
                appendCsvRow(csv, section, row + 1, model.getColumnName(column),
                        model.getValueAt(row, column));
            }
        }
    }

    private static void appendCsvRow(StringBuilder csv, Object section, Object row,
                                     Object field, Object value) {
        csv.append(csvEscape(section)).append(',')
                .append(csvEscape(row)).append(',')
                .append(csvEscape(field)).append(',')
                .append(csvEscape(value)).append("\r\n");
    }

    private static String csvEscape(Object value) {
        String text = value == null ? "" : value.toString();
        return "\"" + text.replace("\"", "\"\"") + "\"";
    }

    private static void appendHtmlTable(StringBuilder html, String title, TableModel model) {
        html.append("<table><caption>").append(htmlEscape(title))
                .append("</caption><thead><tr>");
        for (int column = 0; column < model.getColumnCount(); column++) {
            html.append("<th>").append(htmlEscape(model.getColumnName(column))).append("</th>");
        }
        html.append("</tr></thead><tbody>");
        for (int row = 0; row < model.getRowCount(); row++) {
            html.append("<tr>");
            for (int column = 0; column < model.getColumnCount(); column++) {
                html.append("<td>").append(htmlEscape(model.getValueAt(row, column))).append("</td>");
            }
            html.append("</tr>");
        }
        html.append("</tbody></table>");
    }

    private static String htmlEscape(Object value) {
        String text = value == null ? "" : value.toString();
        return text.replace("&", "&amp;").replace("<", "&lt;")
                .replace(">", "&gt;").replace("\"", "&quot;")
                .replace("'", "&#39;");
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
