package avilaos.config;

import avilaos.kernel.Kernel;
import avilaos.core.hardware.Computer;
import avilaos.model.dto.BufferConfig;
import avilaos.model.dto.ComputerConfig;
import avilaos.model.dto.ProcessConfig;
import avilaos.model.dto.SimulationConfig;
import avilaos.model.enumeration.ProcessType;
import avilaos.model.enumeration.SchedulingPolicyType;
import avilaos.model.pcb.PCB;
import avilaos.kernel.sync.Buffer;
import avilaos.simulation.SimulationCluster;
import avilaos.persistence.JsonConfigSerializer;
import avilaos.persistence.SimulationConfigValidator;
import avilaos.persistence.ValidationResult;
import avilaos.util.datastructures.LinkedList;
import avilaos.util.datastructures.List;
import avilaos.util.datastructures.Queue;

import java.io.File;
import java.io.IOException;

/**
 * Fachada de persistencia e integración para el Kernel del simulador ÁvilaOS.
 * Delega en JsonConfigSerializer (Jackson) y SimulationConfigValidator,
 * garantizando cero uso de colecciones java.util y validación semántica completa.
 */
public class ConfigManager {

    private static final JsonConfigSerializer serializer = new JsonConfigSerializer();
    private static final SimulationConfigValidator validator = new SimulationConfigValidator();

    public static JsonConfigSerializer getSerializer() {
        return serializer;
    }

    public static SimulationConfigValidator getValidator() {
        return validator;
    }

    /**
     * Guarda la configuración de computadores, procesos y buffers en formato JSON.
     */
    public static void saveConfigToJson(String filePath, Kernel kernel) throws IOException {
        if (filePath == null || filePath.isBlank()) {
            throw new IllegalArgumentException("La ruta del archivo no puede estar vacía.");
        }
        if (kernel == null) {
            throw new IllegalArgumentException("El Kernel no puede ser null.");
        }

        SimulationConfig config = extractSimulationConfigFromKernel(kernel);
        serializer.serialize(config, new File(filePath));
    }

    public static void saveConfigToJson(String filePath, SimulationCluster simulation) throws IOException {
        validatePath(filePath);
        if (simulation == null) {
            throw new IllegalArgumentException("La simulación no puede ser null.");
        }
        serializer.serialize(extractSimulationConfigFromCluster(simulation), new File(filePath));
    }

    /**
     * Carga una configuración desde un archivo JSON e inicializa el Kernel.
     */
    public static void loadConfigFromJson(String filePath, Kernel kernel) throws IOException {
        if (filePath == null || filePath.isBlank()) {
            throw new IllegalArgumentException("La ruta del archivo no puede estar vacía.");
        }
        if (kernel == null) {
            throw new IllegalArgumentException("El Kernel no puede ser null.");
        }

        SimulationConfig config = serializer.deserialize(new File(filePath));
        ValidationResult validation = validator.validate(config);
        if (!validation.isValid()) {
            throw new IllegalArgumentException("Configuración inválida:\n" + validation.getFormattedErrors());
        }

        if (config.computers().length != 1
                || config.computers()[0].id() != kernel.getComputer().getId()) {
            throw new IllegalArgumentException("La carga en un Kernel requiere una configuración de un único "
                    + "computador con ID coincidente; use loadSimulationFromJson para varios computadores.");
        }
        applySimulationConfigToKernel(config, kernel);
    }

    public static SimulationCluster loadSimulationFromJson(String filePath) throws IOException {
        validatePath(filePath);
        SimulationConfig config = serializer.deserialize(new File(filePath));
        ValidationResult validation = validator.validate(config);
        if (!validation.isValid()) {
            throw new IllegalArgumentException("Configuración inválida:\n" + validation.getFormattedErrors());
        }

        SimulationCluster simulation = new SimulationCluster();
        try {
            simulation.setCycleDurationMs(config.cycleDurationMs());
            simulation.setNetworkLatencyCycles(config.networkLatencyCycles());
            for (ComputerConfig computer : config.computers()) {
                simulation.addComputer(computer);
            }
            for (BufferConfig buffer : config.buffers()) {
                simulation.getSyncManager().createBuffer(buffer.id(), buffer.capacity(), buffer.hostComputerId());
            }
            for (ProcessConfig process : config.processes()) {
                simulation.submitProcess(new PCB(process.name(), process.computerId(), process.type(),
                        process.priority(), process.memoryRequired(), process.deadline(), process.instructions(),
                        process.bufferId(), process.produceConsumeInterval(), process.elementsRequired()));
            }
            return simulation;
        } catch (RuntimeException ex) {
            simulation.shutdown();
            throw ex;
        }
    }

    public static SimulationConfig extractSimulationConfigFromCluster(SimulationCluster simulation) {
        ComputerConfig[] computers = new ComputerConfig[simulation.getComputers().size()];
        List<PCB> processes = new LinkedList<>();
        for (int i = 0; i < simulation.getComputers().size(); i++) {
            Computer computer = simulation.getComputers().get(i);
            Kernel kernel = computer.getKernel();
            computers[i] = new ComputerConfig(computer.getId(),
                    computer.getRam().getTotalSize() / (1024L * 1024L),
                    kernel.getScheduler().getPolicy().getType(), kernel.getQuantum());
            collectActiveProcesses(kernel, processes);
        }

        List<Buffer> configuredBuffers = simulation.getSyncManager().getAllBuffers();
        BufferConfig[] buffers = new BufferConfig[configuredBuffers.size()];
        for (int i = 0; i < configuredBuffers.size(); i++) {
            Buffer buffer = configuredBuffers.get(i);
            buffers[i] = new BufferConfig(buffer.getId(), buffer.getCapacity(), buffer.getHostComputerId());
        }

        ProcessConfig[] processConfigs = new ProcessConfig[processes.size()];
        for (int i = 0; i < processes.size(); i++) {
            processConfigs[i] = toProcessConfig(processes.get(i), buffers);
        }
        return new SimulationConfig(simulation.getCycleDurationMs(), simulation.getNetworkLatencyCycles(),
                computers, processConfigs, buffers);
    }

    /**
     * Extrae un DTO SimulationConfig inmutable y serializable a partir del estado actual del Kernel.
     */
    public static SimulationConfig extractSimulationConfigFromKernel(Kernel kernel) {
        int compId = (kernel.getComputer() != null) ? kernel.getComputer().getId() : 0;
        long ramMB = (kernel.getComputer() != null && kernel.getComputer().getRam() != null)
                ? kernel.getComputer().getRam().getTotalSize() / (1024L * 1024L)
                : 1024L;
        SchedulingPolicyType policy = (kernel.getScheduler() != null && kernel.getScheduler().getPolicy() != null)
                ? kernel.getScheduler().getPolicy().getType()
                : SchedulingPolicyType.FCFS;
        int quantum = kernel.getQuantum();

        ComputerConfig[] computers = new ComputerConfig[]{
                new ComputerConfig(compId, ramMB, policy, quantum)
        };

        // Buffers
        BufferConfig[] buffersArr;
        if (kernel.getSyncManager() != null && kernel.getSyncManager().getAllBuffers() != null) {
            List<Buffer> buffersList = kernel.getSyncManager().getAllBuffers();
            buffersArr = new BufferConfig[buffersList.size()];
            for (int i = 0; i < buffersList.size(); i++) {
                Buffer b = buffersList.get(i);
                buffersArr[i] = new BufferConfig(b.getId(), b.getCapacity(), b.getHostComputerId());
            }
        } else {
            buffersArr = new BufferConfig[0];
        }

        // Recopilar procesos de colas New, Ready, Terminated y Bloqueadas
        List<PCB> allProcesses = new LinkedList<>();
        collectProcessesFromQueue(kernel.getNewQueue(), allProcesses);
        collectProcessesFromQueue(kernel.getReadyQueue(), allProcesses);
        collectProcessesFromQueue(kernel.getTerminatedQueue(), allProcesses);

        ProcessConfig[] processesArr = new ProcessConfig[allProcesses.size()];
        for (int i = 0; i < allProcesses.size(); i++) {
            processesArr[i] = toProcessConfig(allProcesses.get(i), buffersArr);
        }

        return new SimulationConfig(100, 1, computers, processesArr, buffersArr);
    }

    private static ProcessConfig toProcessConfig(PCB process, BufferConfig[] buffers) {
        ProcessType type = process.getType() == null ? ProcessType.CPU_BOUND : process.getType();
        long remaining = process.getRemainingTime() > 0 ? process.getRemainingTime() : 1L;
        int instructions = (int) Math.min(Integer.MAX_VALUE, remaining);
        long memory = process.getMemoryRequired() > 0 ? process.getMemoryRequired() : 64L;
        String bufferId = process.getBufferId();
        int interval = process.getProduceConsumeInterval();
        int elements = process.getElementsRequired();
        if (type == ProcessType.PRODUCER || type == ProcessType.CONSUMER) {
            if (bufferId == null && buffers.length > 0) bufferId = buffers[0].id();
            if (interval <= 0) interval = 1;
            if (elements <= 0) elements = 1;
        }
        return new ProcessConfig(process.getName(), instructions, memory, process.getPriority(),
                Math.max(0, process.getDeadline()), type, process.getComputerId(), bufferId, interval, elements);
    }

    private static void collectActiveProcesses(Kernel kernel, List<PCB> target) {
        collectUnique(kernel.getNewQueue(), target);
        collectUnique(kernel.getReadyQueue(), target);
        collectUnique(kernel.getComputer().getCpu().getCurrentProcess(), target);
        for (int i = 0; i < kernel.getAllBlockedQueues().size(); i++) {
            collectUnique(kernel.getAllBlockedQueues().get(i), target);
        }
    }

    private static void collectUnique(Queue<PCB> queue, List<PCB> target) {
        if (queue == null) return;
        for (PCB pcb : queue) collectUnique(pcb, target);
    }

    private static void collectUnique(PCB process, List<PCB> target) {
        if (process == null || process.getRemainingTime() <= 0) return;
        for (PCB existing : target) {
            if (existing == process) return;
        }
        target.add(process);
    }

    private static void validatePath(String filePath) {
        if (filePath == null || filePath.isBlank()) {
            throw new IllegalArgumentException("La ruta del archivo no puede estar vacía.");
        }
    }

    /**
     * Aplica la configuración cargada en el Kernel dado.
     */
    public static void applySimulationConfigToKernel(SimulationConfig config, Kernel kernel) {
        if (config == null || kernel == null) {
            throw new IllegalArgumentException("La configuración y el Kernel son obligatorios.");
        }
        int computerId = kernel.getComputer().getId();
        for (ComputerConfig computer : config.computers()) {
            if (computer.id() != computerId) {
                throw new IllegalArgumentException("El computador configurado no coincide con el Kernel destino.");
            }
        }
        for (BufferConfig buffer : config.buffers()) {
            if (buffer.hostComputerId() != computerId) {
                throw new IllegalArgumentException("Los buffers remotos requieren la carga en SimulationCluster.");
            }
        }
        for (ProcessConfig process : config.processes()) {
            if (process.computerId() != computerId) {
                throw new IllegalArgumentException("Los procesos remotos requieren la carga en SimulationCluster.");
            }
        }

        // Cargar Buffers
        if (config.buffers() != null && kernel.getSyncManager() != null) {
            for (BufferConfig b : config.buffers()) {
                kernel.getSyncManager().createBuffer(b.id(), b.capacity(), b.hostComputerId());
            }
        }

        // Cargar Procesos
        if (config.processes() != null) {
            for (ProcessConfig pc : config.processes()) {
                PCB pcb = new PCB(
                        pc.name(),
                        pc.computerId(),
                        pc.type(),
                        pc.priority(),
                        pc.memoryRequired(),
                        pc.deadline(),
                        pc.instructions(),
                        pc.bufferId(),
                        pc.produceConsumeInterval(),
                        pc.elementsRequired()
                );
                kernel.submitProcess(pcb);
            }
        }
    }

    private static void collectProcessesFromQueue(Queue<PCB> queue, List<PCB> targetList) {
        if (queue == null) return;
        int size = queue.size();
        for (int i = 0; i < size; i++) {
            PCB pcb = queue.dequeue();
            if (pcb != null) {
                targetList.add(pcb);
                queue.enqueue(pcb);
            }
        }
    }
}
