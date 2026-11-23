/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package avilaos.core.filesystem;

import avilaos.core.disk.DiskBlock;
import avilaos.util.datastructures.LinkedList;
import avilaos.util.datastructures.List;

/**
 * Nodo del árbol jerárquico del sistema de archivos (directorio o archivo).
 * Utiliza exclusivamente estructuras de datos propias de ÁvilaOS.
 */
public class FileSystemNode {

    private String name;
    private final boolean directory;
    private int sizeInBlocks;
    private String owner;
    private boolean publicAccess;
    private int firstBlock;
    private String creatorProcessId;
    private String colorHex;
    private FileSystemNode parent;
    private final List<FileSystemNode> children;

    /** Constructor para directorios. */
    public FileSystemNode(String name, String owner, boolean publicAccess) {
        this(name, true, 0, owner, publicAccess, DiskBlock.NO_NEXT, "SYSTEM", "#607D8B");
    }

    /** Constructor completo para archivos o directorios. */
    public FileSystemNode(String name,
                          boolean directory,
                          int sizeInBlocks,
                          String owner,
                          boolean publicAccess,
                          int firstBlock,
                          String creatorProcessId,
                          String colorHex) {
        if (name == null || name.isBlank() || name.contains("/")) {
            throw new IllegalArgumentException("Nombre de nodo inválido: " + name);
        }
        if (!directory && sizeInBlocks <= 0) {
            throw new IllegalArgumentException("Un archivo debe ocupar al menos 1 bloque");
        }
        this.name = name.trim();
        this.directory = directory;
        this.sizeInBlocks = directory ? 0 : sizeInBlocks;
        this.owner = (owner == null || owner.isBlank()) ? "admin" : owner.trim();
        this.publicAccess = publicAccess;
        this.firstBlock = directory ? DiskBlock.NO_NEXT : firstBlock;
        this.creatorProcessId = (creatorProcessId == null || creatorProcessId.isBlank()) ? "SYSTEM" : creatorProcessId;
        this.colorHex = (colorHex == null || colorHex.isBlank()) ? "#4CAF50" : colorHex;
        this.children = directory ? new LinkedList<>() : null;
    }

    /** Devuelve la ruta absoluta completa desde la raíz (ej. "/sistema/boot.bin"). */
    public String getPath() {
        if (parent == null) {
            return "/";
        }
        String parentPath = parent.getPath();
        return "/".equals(parentPath) ? "/" + name : parentPath + "/" + name;
    }

    public void addChild(FileSystemNode child) {
        if (!directory) {
            throw new IllegalStateException("No se pueden agregar hijos a un archivo: " + getPath());
        }
        if (child == null) {
            throw new IllegalArgumentException("El nodo hijo no puede ser null");
        }
        child.parent = this;
        children.add(child);
    }

    public boolean removeChild(FileSystemNode child) {
        if (!directory || child == null) {
            return false;
        }
        boolean removed = children.remove(child);
        if (removed) {
            child.parent = null;
        }
        return removed;
    }

    public FileSystemNode findChild(String childName) {
        if (!directory || childName == null) {
            return null;
        }
        for (FileSystemNode child : children) {
            if (child.getName().equals(childName)) {
                return child;
            }
        }
        return null;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        if (name == null || name.isBlank() || name.contains("/")) {
            throw new IllegalArgumentException("Nombre inválido: " + name);
        }
        this.name = name.trim();
    }

    public boolean isDirectory() {
        return directory;
    }

    public int getSizeInBlocks() {
        return sizeInBlocks;
    }

    public String getOwner() {
        return owner;
    }

    public boolean isPublicAccess() {
        return publicAccess;
    }

    public void setPublicAccess(boolean publicAccess) {
        this.publicAccess = publicAccess;
    }

    public int getFirstBlock() {
        return firstBlock;
    }

    public void setFirstBlock(int firstBlock) {
        this.firstBlock = firstBlock;
    }

    public String getCreatorProcessId() {
        return creatorProcessId;
    }

    public String getColorHex() {
        return colorHex;
    }

    public void setColorHex(String colorHex) {
        this.colorHex = colorHex;
    }

    public FileSystemNode getParent() {
        return parent;
    }

    public List<FileSystemNode> getChildren() {
        return children;
    }

    @Override
    public String toString() {
        return name;
    }
}