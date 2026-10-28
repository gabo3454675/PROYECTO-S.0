package avilaos.config;

import avilaos.kernel.Kernel;
import avilaos.model.dto.BufferConfig;
import avilaos.model.dto.ComputerConfig;
import avilaos.model.dto.ProcessConfig;
import avilaos.model.dto.SimulationConfig;
import avilaos.model.enumeration.ProcessType;
import avilaos.model.enumeration.SchedulingPolicyType;
import avilaos.model.pcb.PCB;
import avilaos.kernel.sync.Buffer;
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

        applySimulationConfigToKernel(config, kernel);
    }

    /**
     * Extrae un DTO SimulationConfig inmutable y serializable a partir del estado actual del Kernel.
     */
    public static SimulationConfig extractSimulationConfigFromKernel(Kernel kernel) {
        int compId = (kernel.getComputer() != null) ? kernel.getComputer().getId() : 0;
        long ramMB = (kernel.getComputer() != null && kernel.getComputer().getRam() != null)
                ? kernel.getComputer().getRam().getTotalSize()
                : 1024L;
        SchedulingPolicyType policy = (kernel.getScheduler() != null && kernel.getScheduler().getPolicy() != null)
                ? kernel.getScheduler().getPolicy().getType()
                : SchedulingPolicyType.FCFS;
        int quantum = 2;

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
            PCB p = allProcesses.get(i);
            ProcessType type = (p.getType() != null) ? p.getType() : ProcessType.CPU_BOUND;
            long inst = p.getRemainingTime() > 0 ? p.getRemainingTime() : 100L;
            long mem = p.getMemoryRequired() > 0 ? p.getMemoryRequired() : 64L;
            long deadline = p.getDeadline() >= 0 ? p.getDeadline() : 0L;

            String bufId = null;
            int interval = 0;
            int elemReq = 0;
            if (type == ProcessType.PRODUCER || type == ProcessType.CONSUMER) {
                bufId = (buffersArr.length > 0) ? buffersArr[0].id() : "buf_0";
                interval = 2;
                elemReq = 1;
            }

            processesArr[i] = new ProcessConfig(
                    p.getName(),
                    (int) inst,
                    mem,
                    p.getPriority(),
                    deadline,
                    type,
                    p.getComputerId(),
                    bufId,
                    interval,
                    elemReq
            );
        }

        return new SimulationConfig(100, 1, computers, processesArr, buffersArr);
    }

    /**
     * Aplica la configuración cargada en el Kernel dado.
     */
    public static void applySimulationConfigToKernel(SimulationConfig config, Kernel kernel) {
        if (config == null || kernel == null) return;

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
                        pc.instructions()
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
