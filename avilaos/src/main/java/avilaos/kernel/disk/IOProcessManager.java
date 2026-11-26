/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package avilaos.kernel.disk;

import avilaos.core.filesystem.FileSystemManager;
import avilaos.core.filesystem.FileSystemNode;
import avilaos.core.hardware.CPU;
import avilaos.kernel.Kernel;
import avilaos.model.disk.DiskRequest;
import avilaos.model.enumeration.BlockReason;
import avilaos.model.enumeration.ExecutionMode;
import avilaos.model.enumeration.IOOperationType;
import avilaos.model.enumeration.ProcessState;
import avilaos.model.enumeration.UserMode;
import avilaos.model.pcb.PCB;

/**
 * Coordina la ejecución de procesos de E/S entre la CPU, el planificador de
 * disco ({@link DiskScheduler}) y el sistema de archivos ({@link FileSystemManager}).
 */
public class IOProcessManager {

    private final DiskScheduler diskScheduler;
    private final FileSystemManager fileSystemManager;
    private long systemModeInstructions;

    public IOProcessManager(DiskScheduler diskScheduler, FileSystemManager fileSystemManager) {
        if (diskScheduler == null) {
            throw new IllegalArgumentException("DiskScheduler no puede ser nulo");
        }
        if (fileSystemManager == null) {
            throw new IllegalArgumentException("FileSystemManager no puede ser nulo");
        }
        this.diskScheduler = diskScheduler;
        this.fileSystemManager = fileSystemManager;
        this.systemModeInstructions = 0;
    }

    /**
     * Ejecuta la llamada al sistema de E/S cuando el proceso obtiene la CPU:
     * cambia a modo {@link ExecutionMode#KERNEL}, encola su {@link DiskRequest}
     * en {@link DiskScheduler} y bloquea el proceso por {@link BlockReason#IO_WAIT}.
     */
    public DiskRequest submitFromCpu(PCB pcb, CPU cpu, long currentCycle) {
        if (pcb == null || !pcb.hasPendingIORequest()) {
            return null;
        }

        pcb.setMode(ExecutionMode.KERNEL);
        if (cpu != null) {
            cpu.setMode(ExecutionMode.KERNEL);
        }
        systemModeInstructions++;

        int targetBlock = resolveTargetBlock(pcb);
        DiskRequest request = new DiskRequest(
                pcb.getId(),
                pcb.getName(),
                pcb.getIoOperation(),
                pcb.getTargetPath(),
                targetBlock,
                currentCycle
        );

        diskScheduler.submit(request);
        pcb.markIoRequestSubmitted();
        pcb.setBlockReason(BlockReason.IO_WAIT);
        pcb.setState(ProcessState.BLOCKED);
        return request;
    }

    /**
     * Completa una solicitud atendida por el disco: valida permisos según el modo activo,
     * ejecuta la operación CRUD sobre el sistema de archivos, restaura modo {@link ExecutionMode#USER}
     * y despierta al proceso en el {@link Kernel}.
     */
    public void completeDiskRequest(DiskRequest request, Kernel kernel) {
        if (request == null) {
            throw new IllegalArgumentException("La solicitud atendida no puede ser nula");
        }
        if (kernel == null) {
            throw new IllegalArgumentException("El kernel no puede ser nulo");
        }

        PCB pcb = kernel.findProcessById(request.getProcessId());
        if (pcb == null || kernel.isProcessTerminated(pcb)) {
            return;
        }
        String error = executeFileSystemOperation(request, pcb, kernel);

        pcb.setIoErrorMessage(error);
        pcb.markIoRequestCompleted();
        pcb.setMode(ExecutionMode.USER);
        if (pcb.isFinished() || pcb.getRemainingTime() <= 0) {
            kernel.unblockAndTerminate(pcb, error == null);
        } else {
            kernel.unblockProcess(pcb);
        }
    }

    private int resolveTargetBlock(PCB pcb) {
        IOOperationType op = pcb.getIoOperation();
        String path = pcb.getTargetPath();
        if (op == null || path == null || op == IOOperationType.CREATE || op.isSwapOperation()) {
            return 0;
        }
        FileSystemNode node = fileSystemManager.resolve(path);
        if (node == null || node.isDirectory()) {
            return 0;
        }
        return Math.max(0, node.getFirstBlock());
    }

    private String executeFileSystemOperation(DiskRequest request, PCB pcb, Kernel kernel) {
        IOOperationType op = request.getOperation();
        if (op == null || !op.isFileOperation()) {
            return null;
        }

        UserMode mode = kernel.getUserMode();
        String currentUser = kernel.getActiveUser();
        String path = request.getTargetPath();

        try {
            switch (op) {
                case CREATE -> {
                    int blocks = pcb != null ? pcb.getFileSizeInBlocks() : 1;
                    String requestedOwner = pcb != null ? pcb.getFileOwner() : null;
                    String effectiveOwner = mode.isAdmin()
                            ? (requestedOwner != null && !requestedOwner.isBlank() ? requestedOwner : "admin")
                            : currentUser;
                    boolean isPublic = pcb == null || pcb.isFilePublic();
                    int lastSlash = path != null ? path.lastIndexOf('/') : -1;
                    String parentPath = lastSlash > 0 ? path.substring(0, lastSlash) : "/";
                    String fileName = lastSlash >= 0 ? path.substring(lastSlash + 1) : path;

                    FileSystemNode parentDir = fileSystemManager.resolve(parentPath);
                    if (parentDir == null || !parentDir.isDirectory()) {
                        throw new IllegalArgumentException("El directorio padre no existe: " + parentPath);
                    }
                    if (!mode.isAdmin() && !fileSystemManager.canModify(parentDir, mode, currentUser)) {
                        throw new SecurityException("Permiso denegado en modo Usuario ('" + currentUser
                                + "'): solo puede crear archivos en sus propios directorios (el directorio '"
                                + parentPath + "' pertenece a '" + parentDir.getOwner() + "').");
                    }

                    fileSystemManager.createFile(parentPath, fileName, blocks, effectiveOwner, isPublic,
                            Long.toString(pcb.getId()));
                }
                case READ -> {
                    FileSystemNode node = fileSystemManager.resolve(path);
                    if (node == null) {
                        throw new IllegalArgumentException("Ruta inexistente: " + path);
                    }
                    if (!fileSystemManager.canRead(node, mode, currentUser)) {
                        throw new SecurityException("Permiso denegado en modo Usuario ('" + currentUser
                                + "'): el archivo '" + path + "' es privado y pertenece a '" + node.getOwner() + "'.");
                    }
                }
                case UPDATE -> {
                    FileSystemNode node = fileSystemManager.resolve(path);
                    if (node == null) {
                        throw new IllegalArgumentException("Ruta no encontrada: " + path);
                    }
                    if (!fileSystemManager.canModify(node, mode, currentUser)) {
                        throw new SecurityException("Permiso denegado en modo Usuario ('" + currentUser
                                + "'): no puede modificar '" + path + "' porque pertenece a '" + node.getOwner() + "'.");
                    }
                    String newName = pcb != null ? pcb.getNewFileName() : null;
                    fileSystemManager.rename(path, newName);
                }
                case DELETE -> {
                    FileSystemNode node = fileSystemManager.resolve(path);
                    if (node == null) {
                        throw new IllegalArgumentException("Ruta no encontrada: " + path);
                    }
                    if (!fileSystemManager.canModify(node, mode, currentUser)) {
                        throw new SecurityException("Permiso denegado en modo Usuario ('" + currentUser
                                + "'): no puede eliminar '" + path + "' porque pertenece a '" + node.getOwner() + "'.");
                    }
                    fileSystemManager.delete(path);
                }
                default -> {
                }
            }
            return null;
        } catch (RuntimeException ex) {
            return ex.getMessage();
        }
    }

    public DiskScheduler getDiskScheduler() {
        return diskScheduler;
    }

    public FileSystemManager getFileSystemManager() {
        return fileSystemManager;
    }

    public long getSystemModeInstructions() {
        return systemModeInstructions;
    }
}