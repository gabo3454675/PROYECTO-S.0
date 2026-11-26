package avilaos.gui;

import avilaos.core.disk.DiskBlock;
import avilaos.core.disk.DiskBlockMap;
import avilaos.core.filesystem.FileSystemNode;
import avilaos.core.hardware.Computer;
import avilaos.model.disk.DiskRequest;
import avilaos.model.enumeration.DiskPolicyType;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import java.awt.*;

/** Vista de observación del disco, archivos y solicitudes del Proyecto 2. */
public final class Project2DiskPanel extends JPanel {
    private final Computer computer;
    private final JLabel diskStatus = new JLabel();
    private final JLabel fileDetails = new JLabel("Selecciona un archivo o directorio.");
    private final JTree fileTree = new JTree();
    private final DefaultTableModel blockModel = new DefaultTableModel(
            new String[]{"Bloque", "Uso", "Archivo / proceso", "Siguiente"}, 0);
    private final DefaultTableModel requestModel = new DefaultTableModel(
            new String[]{"Proceso", "Operación", "Bloque", "Llegada"}, 0);
    private final JComboBox<DiskPolicyType> policy = new JComboBox<>(DiskPolicyType.values());
    private final Timer refreshTimer;

    public Project2DiskPanel(Computer computer) {
        if (computer == null || computer.getDisk() == null)
            throw new IllegalArgumentException("Se requiere un computador con disco");
        this.computer = computer;
        setLayout(new BorderLayout(12, 12));
        setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

        JPanel controls = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 6));
        controls.add(diskStatus);
        controls.add(new JLabel("Política de disco"));
        policy.setSelectedItem(computer.getKernel().getIoProcessManager().getDiskScheduler().getPolicyType());
        policy.addActionListener(e -> computer.getKernel().getIoProcessManager()
                .getDiskScheduler().setPolicy((DiskPolicyType) policy.getSelectedItem()));
        controls.add(policy);
        JButton refresh = new JButton("Actualizar");
        refresh.addActionListener(e -> refresh());
        controls.add(refresh);
        add(controls, BorderLayout.NORTH);

        fileTree.addTreeSelectionListener(e -> {
            var selected = fileTree.getLastSelectedPathComponent();
            if (selected instanceof DefaultMutableTreeNode treeNode
                    && treeNode.getUserObject() instanceof FileSystemNode node) {
                fileDetails.setText(node.getPath() + "  |  "
                        + (node.isDirectory() ? "Directorio" : node.getSizeInBlocks() + " bloques, inicio " + node.getFirstBlock())
                        + "  |  Dueño: " + node.getOwner()
                        + "  |  " + (node.isPublicAccess() ? "Público" : "Privado"));
            }
        });
        JPanel files = new JPanel(new BorderLayout(6, 6));
        files.setBorder(BorderFactory.createTitledBorder("Sistema de archivos"));
        files.add(new JScrollPane(fileTree), BorderLayout.CENTER);
        files.add(fileDetails, BorderLayout.SOUTH);

        JTable blocks = new JTable(blockModel);
        blocks.setAutoCreateRowSorter(true);
        JPanel map = new JPanel(new BorderLayout());
        map.setBorder(BorderFactory.createTitledBorder("Mapa de bloques"));
        map.add(new JScrollPane(blocks), BorderLayout.CENTER);
        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, files, map);
        split.setResizeWeight(0.35);
        add(split, BorderLayout.CENTER);

        JTable requests = new JTable(requestModel);
        JPanel queue = new JPanel(new BorderLayout());
        queue.setBorder(BorderFactory.createTitledBorder("Cola de E/S"));
        queue.add(new JScrollPane(requests), BorderLayout.CENTER);
        queue.setPreferredSize(new Dimension(600, 155));
        add(queue, BorderLayout.SOUTH);

        refresh();
        applyDarkStyle(this);
        refreshTimer = new Timer(500, e -> { if (isShowing()) refresh(); });
        refreshTimer.start();
    }

    public void dispose() { refreshTimer.stop(); }

    public void refresh() {
        var disk = computer.getDisk();
        DiskBlockMap map = computer.getKernel().getIoProcessManager().getFileSystemManager().getBlockMap();
        diskStatus.setText("Cabezal: " + disk.getHeadPosition() + "  |  Sentido: "
                + disk.getDirection() + "  |  Desplazamiento: " + disk.getTotalHeadMovement()
                + "  |  Swap: " + map.getSwapCapacity() + " bloques");
        blockModel.setRowCount(0);
        for (int i = 0; i < map.getTotalBlocks(); i++) {
            DiskBlock block = map.getBlock(i);
            blockModel.addRow(new Object[]{i, block.getUsage().getDisplayName(),
                    block.getOwnerId() == null ? "" : block.getOwnerId(),
                    block.getNext() == DiskBlock.NO_NEXT ? "" : block.getNext()});
        }
        requestModel.setRowCount(0);
        DiskRequest active = disk.getActiveRequest();
        if (active != null) addRequest(active, "En curso: ");
        for (DiskRequest request : computer.getKernel().getIoProcessManager()
                .getDiskScheduler().snapshotQueue()) addRequest(request, "");
        FileSystemNode root = computer.getKernel().getIoProcessManager().getFileSystemManager().getRoot();
        String selectedPath = null;
        if (fileTree.getLastSelectedPathComponent() instanceof DefaultMutableTreeNode selected
                && selected.getUserObject() instanceof FileSystemNode node) selectedPath = node.getPath();
        DefaultMutableTreeNode treeRoot = treeNode(root);
        fileTree.setModel(new DefaultTreeModel(treeRoot));
        for (int row = 0; row < fileTree.getRowCount(); row++) fileTree.expandRow(row);
        if (selectedPath != null) {
            TreePath path = findPath(treeRoot, selectedPath);
            if (path != null) fileTree.setSelectionPath(path);
        }
    }

    private void addRequest(DiskRequest request, String prefix) {
        requestModel.addRow(new Object[]{request.getProcessName(), prefix + request.getOperation().getDisplayName(),
                request.getTargetBlock(), request.getArrivalCycle()});
    }

    private DefaultMutableTreeNode treeNode(FileSystemNode node) {
        DefaultMutableTreeNode tree = new DefaultMutableTreeNode(node);
        if (node.isDirectory()) for (FileSystemNode child : node.getChildren()) tree.add(treeNode(child));
        return tree;
    }

    private TreePath findPath(DefaultMutableTreeNode tree, String path) {
        if (tree.getUserObject() instanceof FileSystemNode node && path.equals(node.getPath()))
            return new TreePath(tree.getPath());
        for (int i = 0; i < tree.getChildCount(); i++) {
            TreePath found = findPath((DefaultMutableTreeNode) tree.getChildAt(i), path);
            if (found != null) return found;
        }
        return null;
    }

    private void applyDarkStyle(Component component) {
        Color background = new Color(34, 40, 52);
        Color foreground = new Color(220, 225, 230);
        if (component instanceof JPanel panel) {
            panel.setBackground(background);
            if (panel.getBorder() instanceof javax.swing.border.TitledBorder border)
                border.setTitleColor(foreground);
        } else if (component instanceof JTable table) {
            table.setBackground(background);
            table.setForeground(foreground);
            table.setGridColor(new Color(55, 65, 80));
            table.setRowHeight(24);
            table.getTableHeader().setBackground(new Color(44, 52, 68));
            table.getTableHeader().setForeground(Color.BLACK);
        } else if (component instanceof JTree tree) {
            tree.setBackground(background);
            tree.setForeground(foreground);
        } else if (component instanceof JLabel label) {
            label.setForeground(foreground);
        } else if (component instanceof JScrollPane scroll) {
            scroll.getViewport().setBackground(background);
        }
        if (component instanceof Container container)
            for (Component child : container.getComponents()) applyDarkStyle(child);
    }
}
