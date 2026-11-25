/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package avilaos.core.filesystem;

import avilaos.core.disk.DiskBlock;
import avilaos.core.disk.DiskBlockMap;
import avilaos.model.enumeration.BlockUsage;
import avilaos.model.enumeration.UserMode;
import avilaos.util.datastructures.LinkedList;
import avilaos.util.datastructures.List;

import java.util.concurrent.Semaphore;

/**
 * Gestor del sistema de archivos jerárquico sobre el DiskBlockMap.
 * Todas las operaciones están sincronizadas con un semáforo binario.
 */
public class FileSystemManager {

    private static final String[] DEFAULT_PALETTE = {
        "#42A5F5", "#66BB6A", "#FFA726", "#AB47BC",
        "#26C6DA", "#EC407A", "#8D6E63", "#7E57C2"
    };

    private final DiskBlockMap blockMap;
    private final FileSystemNode root;
    private final Semaphore mutex = new Semaphore(1, true);
    private int colorCursor = 0;

    public FileSystemManager(DiskBlockMap blockMap) {
        if (blockMap == null) {
            throw new IllegalArgumentException("DiskBlockMap no puede ser null");
        }
        this.blockMap = blockMap;
        this.root = new FileSystemNode("root", "admin", true);
    }

    /** Resuelve una ruta absoluta (ej. "/sistema/boot.bin") o devuelve null si no existe. */
    public FileSystemNode resolve(String path) {
        lock();
        try {
            return resolveInternal(path);
        } finally {
            mutex.release();
        }
    }

    /** Crea un directorio bajo la ruta padre indicada. */
    public FileSystemNode createDirectory(String parentPath, String dirName, String owner, boolean publicAccess) {
        lock();
        try {
            FileSystemNode parent = requireDirectory(parentPath);
            if (parent.findChild(dirName) != null) {
                throw new IllegalArgumentException("Ya existe un elemento llamado '" + dirName + "' en " + parentPath);
            }
            FileSystemNode dir = new FileSystemNode(dirName, owner, publicAccess);
            parent.addChild(dir);
            return dir;
        } finally {
            mutex.release();
        }
    }

    /**
     * Crea un archivo asignando sus bloques en el disco mediante asignación encadenada.
     * Lanza IllegalStateException si no hay suficientes bloques libres.
     */
    public FileSystemNode createFile(String parentPath,
                                     String fileName,
                                     int sizeInBlocks,
                                     String owner,
                                     boolean publicAccess,
                                     String creatorProcessId) {
        return createFile(parentPath, fileName, sizeInBlocks, owner, publicAccess, creatorProcessId, 0);
    }

    /**
     * Crea un archivo permitiendo indicar un bloque de inicio preferido (útil al cargar el JSON inicial).
     */
    public FileSystemNode createFile(String parentPath,
                                     String fileName,
                                     int sizeInBlocks,
                                     String owner,
                                     boolean publicAccess,
                                     String creatorProcessId,
                                     int preferredStartBlock) {
        lock();
        try {
            FileSystemNode parent = requireDirectory(parentPath);
            // Validar antes de reservar bloques para no dejar espacio ocupado al fallar.
            new FileSystemNode(fileName, false, sizeInBlocks, owner, publicAccess,
                    DiskBlock.NO_NEXT, creatorProcessId, null);
            if (parent.findChild(fileName) != null) {
                throw new IllegalArgumentException("Ya existe un elemento llamado '" + fileName + "' en " + parentPath);
            }
            String fullPath = "/".equals(parent.getPath()) ? "/" + fileName : parent.getPath() + "/" + fileName;
            int firstBlock = blockMap.allocate(BlockUsage.FILE, fullPath, sizeInBlocks, preferredStartBlock);
            if (firstBlock == DiskBlock.NO_NEXT) {
                throw new IllegalStateException("Espacio insuficiente en disco para '" + fullPath
                        + "' (" + sizeInBlocks + " bloques requeridos, " + blockMap.getFreeCount() + " disponibles)");
            }
            String color = nextColor();
            FileSystemNode file = new FileSystemNode(
                    fileName, false, sizeInBlocks, owner, publicAccess, firstBlock, creatorProcessId, color
            );
            parent.addChild(file);
            return file;
        } finally {
            mutex.release();
        }
    }

    /** Renombra un archivo o directorio existente (operación UPDATE). */
    public FileSystemNode rename(String path, String newName) {
        if ("/".equals(normalizePath(path))) {
            throw new IllegalArgumentException("No se puede renombrar el directorio raíz");
        }
        lock();
        try {
            FileSystemNode target = resolveInternal(path);
            if (target == null) {
                throw new IllegalArgumentException("Ruta no encontrada: " + path);
            }
            FileSystemNode parent = target.getParent();
            if (parent != null && !target.getName().equals(newName) && parent.findChild(newName) != null) {
                throw new IllegalArgumentException("Ya existe '" + newName + "' en " + parent.getPath());
            }
            target.setName(newName);
            refreshBlockOwners(target);
            return target;
        } finally {
            mutex.release();
        }
    }

    /**
     * Elimina un archivo (liberando sus bloques) o un directorio recursivamente (liberando todo su contenido).
     *
     * @return cantidad total de bloques liberados en el disco
     */
    public int delete(String path) {
        if ("/".equals(normalizePath(path))) {
            throw new IllegalArgumentException("No se puede eliminar el directorio raíz");
        }
        lock();
        try {
            FileSystemNode target = resolveInternal(path);
            if (target == null) {
                throw new IllegalArgumentException("Ruta no encontrada: " + path);
            }
            int releasedBlocks = releaseRecursively(target);
            FileSystemNode parent = target.getParent();
            if (parent != null) {
                parent.removeChild(target);
            }
            return releasedBlocks;
        } finally {
            mutex.release();
        }
    }

    /** Devuelve una lista plana de todos los archivos del sistema (para la tabla de asignación). */
    public List<FileSystemNode> getAllFiles() {
        lock();
        try {
            List<FileSystemNode> files = new LinkedList<>();
            collectFiles(root, files);
            return files;
        } finally {
            mutex.release();
        }
    }

    /** Verifica si un usuario tiene permiso de lectura sobre el nodo. */
    public boolean canRead(FileSystemNode node, UserMode mode, String currentUser) {
        if (node == null) {
            return false;
        }
        if (mode == UserMode.ADMIN) {
            return true;
        }
        return node.isPublicAccess() || node.getOwner().equals(currentUser);
    }

    /** Verifica si un usuario tiene permiso de escritura/modificación/borrado sobre el nodo. */
    public boolean canModify(FileSystemNode node, UserMode mode, String currentUser) {
        if (node == null) {
            return false;
        }
        if (mode == UserMode.ADMIN) {
            return true;
        }
        return node.getOwner().equals(currentUser);
    }

    public FileSystemNode getRoot() {
        return root;
    }

    public DiskBlockMap getBlockMap() {
        return blockMap;
    }

    private FileSystemNode resolveInternal(String rawPath) {
        String path = normalizePath(rawPath);
        if ("/".equals(path)) {
            return root;
        }
        String[] parts = path.substring(1).split("/");
        FileSystemNode current = root;
        for (String part : parts) {
            if (part.isEmpty()) {
                continue;
            }
            current = current.findChild(part);
            if (current == null) {
                return null;
            }
        }
        return current;
    }

    private FileSystemNode requireDirectory(String path) {
        FileSystemNode node = resolveInternal(path);
        if (node == null) {
            throw new IllegalArgumentException("El directorio padre no existe: " + path);
        }
        if (!node.isDirectory()) {
            throw new IllegalArgumentException("La ruta no corresponde a un directorio: " + path);
        }
        return node;
    }

    private int releaseRecursively(FileSystemNode node) {
        if (!node.isDirectory()) {
            int first = node.getFirstBlock();
            return (first != DiskBlock.NO_NEXT) ? blockMap.free(first) : 0;
        }
        int totalReleased = 0;
        List<FileSystemNode> children = node.getChildren();
        for (FileSystemNode child : children) {
            totalReleased += releaseRecursively(child);
        }
        children.clear();
        return totalReleased;
    }

    private void collectFiles(FileSystemNode current, List<FileSystemNode> accumulator) {
        if (!current.isDirectory()) {
            accumulator.add(current);
            return;
        }
        for (FileSystemNode child : current.getChildren()) {
            collectFiles(child, accumulator);
        }
    }

    private void refreshBlockOwners(FileSystemNode node) {
        if (!node.isDirectory()) {
            blockMap.renameOwner(node.getFirstBlock(), node.getPath());
            return;
        }
        for (FileSystemNode child : node.getChildren()) {
            refreshBlockOwners(child);
        }
    }

    private String normalizePath(String path) {
        if (path == null || path.isBlank()) {
            throw new IllegalArgumentException("La ruta no puede estar vacía");
        }
        String trimmed = path.trim();
        if (!trimmed.startsWith("/")) {
            trimmed = "/" + trimmed;
        }
        if (trimmed.length() > 1 && trimmed.endsWith("/")) {
            trimmed = trimmed.substring(0, trimmed.length() - 1);
        }
        return trimmed;
    }

    private String nextColor() {
        String color = DEFAULT_PALETTE[colorCursor % DEFAULT_PALETTE.length];
        colorCursor++;
        return color;
    }

    private void lock() {
        mutex.acquireUninterruptibly();
    }
}
