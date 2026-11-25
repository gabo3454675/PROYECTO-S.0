package avilaos.model.pcb;

import avilaos.model.enumeration.BlockReason;
import avilaos.model.enumeration.ExecutionMode;
import avilaos.model.enumeration.IOOperationType;
import avilaos.model.enumeration.ProcessState;
import avilaos.model.enumeration.ProcessType;
import avilaos.util.IDGenerator;

public final class PCB {
    private final long id;
    private final String name;
    private final int computerId;
    private ProcessState state;
    private final ProcessType type;
    private final int priority;
    private final long memoryRequired;
    private long deadline;
    private long remainingTime;
    private int programCounter;
    private ExecutionMode mode;
    private BlockReason blockReason;
    private int quantumUsed;
    private int networkLatencyRemaining;
    private final String bufferId;
    private final int produceConsumeInterval;
    private final int elementsRequired;
    private int elementsProcessed;

    // Metadatos para procesos de E/S sobre el sistema de archivos (Bloque 3)
    private final IOOperationType ioOperation;
    private final String targetPath;
    private final int fileSizeInBlocks;
    private final String fileOwner;
    private final boolean filePublic;
    private final String newFileName;
    private boolean ioRequestSubmitted;
    private boolean ioRequestCompleted;
    private String ioErrorMessage;

    public PCB(String name, int computerId, ProcessType type, int priority,
               long memoryRequired, long deadline, long remainingTime) {
        this(name, computerId, type, priority, memoryRequired, deadline, remainingTime,
                null, 0, 0, null, null, 0, "system", true, null);
    }

    public PCB(String name, int computerId, ProcessType type, int priority,
               long memoryRequired, long deadline, long remainingTime, String bufferId,
               int produceConsumeInterval, int elementsRequired) {
        this(name, computerId, type, priority, memoryRequired, deadline, remainingTime,
                bufferId, produceConsumeInterval, elementsRequired,
                null, null, 0, "system", true, null);
    }

    public PCB(String name, int computerId, ProcessType type, int priority,
               long memoryRequired, long deadline, long remainingTime,
               IOOperationType ioOperation, String targetPath, int fileSizeInBlocks,
               String fileOwner, boolean filePublic, String newFileName) {
        this(name, computerId, type, priority, memoryRequired, deadline, remainingTime,
                null, 0, 0, ioOperation, targetPath, fileSizeInBlocks, fileOwner, filePublic, newFileName);
    }

    private PCB(String name, int computerId, ProcessType type, int priority,
                long memoryRequired, long deadline, long remainingTime,
                String bufferId, int produceConsumeInterval, int elementsRequired,
                IOOperationType ioOperation, String targetPath, int fileSizeInBlocks,
                String fileOwner, boolean filePublic, String newFileName) {
        this.id = IDGenerator.getNextId();
        this.name = name;
        this.computerId = computerId;
        this.state = ProcessState.NEW;
        this.type = type;
        this.priority = priority;
        this.memoryRequired = memoryRequired;
        this.deadline = deadline;
        this.remainingTime = remainingTime;
        this.programCounter = 0;
        this.mode = ExecutionMode.USER;
        this.blockReason = null;
        this.quantumUsed = 0;
        this.networkLatencyRemaining = 0;
        this.bufferId = bufferId;
        this.produceConsumeInterval = produceConsumeInterval;
        this.elementsRequired = elementsRequired;
        this.elementsProcessed = 0;

        this.ioOperation = ioOperation;
        this.targetPath = targetPath;
        this.fileSizeInBlocks = fileSizeInBlocks;
        this.fileOwner = fileOwner != null && !fileOwner.isBlank() ? fileOwner : "system";
        this.filePublic = filePublic;
        this.newFileName = newFileName;
        this.ioRequestSubmitted = false;
        this.ioRequestCompleted = false;
        this.ioErrorMessage = null;
    }

    public static PCB forFileCreate(String processName, int computerId, int priority,
                                    long memoryRequired, long deadline, long remainingTime,
                                    String targetPath, int sizeInBlocks, String owner, boolean isPublic) {
        return new PCB(processName, computerId, ProcessType.IO_BOUND, priority,
                memoryRequired, deadline, Math.max(1, remainingTime),
                IOOperationType.CREATE, targetPath, sizeInBlocks, owner, isPublic, null);
    }

    public static PCB forFileRead(String processName, int computerId, int priority,
                                  long memoryRequired, long deadline, long remainingTime,
                                  String targetPath) {
        return new PCB(processName, computerId, ProcessType.IO_BOUND, priority,
                memoryRequired, deadline, Math.max(1, remainingTime),
                IOOperationType.READ, targetPath, 0, "system", true, null);
    }

    public static PCB forFileUpdate(String processName, int computerId, int priority,
                                    long memoryRequired, long deadline, long remainingTime,
                                    String targetPath, String newName) {
        return new PCB(processName, computerId, ProcessType.IO_BOUND, priority,
                memoryRequired, deadline, Math.max(1, remainingTime),
                IOOperationType.UPDATE, targetPath, 0, "system", true, newName);
    }

    public static PCB forFileDelete(String processName, int computerId, int priority,
                                    long memoryRequired, long deadline, long remainingTime,
                                    String targetPath) {
        return new PCB(processName, computerId, ProcessType.IO_BOUND, priority,
                memoryRequired, deadline, Math.max(1, remainingTime),
                IOOperationType.DELETE, targetPath, 0, "system", true, null);
    }

    public long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public int getComputerId() {
        return computerId;
    }

    public ProcessState getState() {
        return state;
    }

    public void setState(ProcessState state) {
        this.state = state;
    }

    public ProcessType getType() {
        return type;
    }

    public int getPriority() {
        return priority;
    }

    public long getMemoryRequired() {
        return memoryRequired;
    }

    public long getDeadline() {
        return deadline;
    }

    public void setDeadline(long deadline) {
        this.deadline = deadline;
    }

    public long getRemainingTime() {
        return remainingTime;
    }

    public void setRemainingTime(long remainingTime) {
        this.remainingTime = remainingTime;
    }

    public void decrementRemainingTime() {
        if (remainingTime > 0) remainingTime--;
    }

    public int getProgramCounter() {
        return programCounter;
    }

    public void incrementProgramCounter() {
        programCounter++;
    }

    public void setProgramCounter(int pc) {
        this.programCounter = pc;
    }

    public ExecutionMode getMode() {
        return mode;
    }

    public void setMode(ExecutionMode mode) {
        this.mode = mode;
    }

    public BlockReason getBlockReason() {
        return blockReason;
    }

    public void setBlockReason(BlockReason blockReason) {
        this.blockReason = blockReason;
    }

    public int getQuantumUsed() {
        return quantumUsed;
    }

    public void incrementQuantumUsed() {
        quantumUsed++;
    }

    public void resetQuantumUsed() {
        quantumUsed = 0;
    }

    public int getNetworkLatencyRemaining() {
        return networkLatencyRemaining;
    }

    public void setNetworkLatencyRemaining(int cycles) {
        this.networkLatencyRemaining = cycles;
    }

    public void decrementNetworkLatency() {
        if (networkLatencyRemaining > 0) networkLatencyRemaining--;
    }

    public String getBufferId() {
        return bufferId;
    }

    public int getProduceConsumeInterval() {
        return produceConsumeInterval;
    }

    public int getElementsRequired() {
        return elementsRequired;
    }

    public int getElementsProcessed() {
        return elementsProcessed;
    }

    public void incrementElementsProcessed() {
        elementsProcessed++;
    }

    public IOOperationType getIoOperation() {
        return ioOperation;
    }

    public String getTargetPath() {
        return targetPath;
    }

    public int getFileSizeInBlocks() {
        return fileSizeInBlocks;
    }

    public String getFileOwner() {
        return fileOwner;
    }

    public boolean isFilePublic() {
        return filePublic;
    }

    public String getNewFileName() {
        return newFileName;
    }

    public boolean isIoRequestSubmitted() {
        return ioRequestSubmitted;
    }

    public void markIoRequestSubmitted() {
        this.ioRequestSubmitted = true;
    }

    public boolean isIoRequestCompleted() {
        return ioRequestCompleted;
    }

    public void markIoRequestCompleted() {
        this.ioRequestCompleted = true;
    }

    public String getIoErrorMessage() {
        return ioErrorMessage;
    }

    public void setIoErrorMessage(String ioErrorMessage) {
        this.ioErrorMessage = ioErrorMessage;
    }

    public boolean hasPendingIORequest() {
        return ioOperation != null && !ioRequestSubmitted;
    }

    public boolean isDeadlineReached(long currentCycle) {
        return deadline > 0 && currentCycle >= deadline;
    }

    public boolean isFinished() {
        return remainingTime <= 0;
    }

    @Override
    public String toString() {
        return String.format("PCB{id=%d, name='%s', state=%s, pc=%d, mode=%s, remTime=%d, deadline=%d}",
                id, name, state, programCounter, mode, remainingTime, deadline);
    }
}