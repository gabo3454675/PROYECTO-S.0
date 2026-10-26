package avilaos.persistence;

import avilaos.model.dto.BufferConfig;
import avilaos.model.dto.ComputerConfig;
import avilaos.model.dto.ProcessConfig;
import avilaos.model.dto.SimulationConfig;
import avilaos.model.enumeration.ProcessType;
import avilaos.util.datastructures.AssociativeList;

/**
 * Validador semántico de configuraciones de simulación.
 * Garantiza consistencia referencial, límites de hardware y reglas de negocio
 * sin interrumpir la ejecución con excepciones no controladas.
 */
public class SimulationConfigValidator {

    /**
     * Valida la configuración completa y retorna un ValidationResult con los errores y advertencias detectados.
     *
     * @param config Configuración a validar.
     * @return Objeto ValidationResult con los detalles.
     */
    public ValidationResult validate(SimulationConfig config) {
        ValidationResult result = new ValidationResult();

        if (config == null) {
            result.addError("La configuración no puede ser null.");
            return result;
        }

        // 1. Validar parámetros globales
        if (config.cycleDurationMs() <= 0) {
            result.addError("La duración del ciclo debe ser mayor a 0 ms (valor actual: " + config.cycleDurationMs() + ").");
        }
        if (config.networkLatencyCycles() < 0) {
            result.addError("La latencia de red no puede ser negativa (valor actual: " + config.networkLatencyCycles() + ").");
        }

        // 2. Validar computadores del clúster
        ComputerConfig[] computers = config.computers();
        if (computers == null || computers.length == 0) {
            result.addError("Debe configurarse al menos un computador en el clúster.");
            return result;
        }

        AssociativeList<Integer, ComputerConfig> computerMap = new AssociativeList<>();
        for (int i = 0; i < computers.length; i++) {
            ComputerConfig comp = computers[i];
            if (comp == null) {
                result.addError("El computador en la posición " + i + " es null.");
                continue;
            }

            if (comp.id() < 0) {
                result.addError("El ID del computador no puede ser negativo (ID: " + comp.id() + ").");
            }

            if (computerMap.containsKey(comp.id())) {
                result.addError("Existe más de un computador con el ID duplicado: " + comp.id() + ".");
            } else {
                computerMap.put(comp.id(), comp);
            }

            if (comp.ramSizeMB() <= 0) {
                result.addError("El computador " + comp.id() + " debe tener RAM > 0 MB (actual: " + comp.ramSizeMB() + ").");
            }
            if (comp.quantum() <= 0) {
                result.addError("El computador " + comp.id() + " debe tener un quantum > 0 (actual: " + comp.quantum() + ").");
            }
            if (comp.initialPolicy() == null) {
                result.addError("El computador " + comp.id() + " debe tener una política de planificación inicial definida.");
            }
        }

        // 3. Validar buffers
        BufferConfig[] buffers = config.buffers();
        AssociativeList<String, BufferConfig> bufferMap = new AssociativeList<>();
        if (buffers != null) {
            for (int i = 0; i < buffers.length; i++) {
                BufferConfig buf = buffers[i];
                if (buf == null) {
                    result.addError("El buffer en la posición " + i + " es null.");
                    continue;
                }

                if (buf.id() == null || buf.id().isBlank()) {
                    result.addError("El buffer en la posición " + i + " tiene un ID vacío o null.");
                } else if (bufferMap.containsKey(buf.id())) {
                    result.addError("Existe más de un buffer con el ID duplicado: '" + buf.id() + "'.");
                } else {
                    bufferMap.put(buf.id(), buf);
                }

                if (buf.capacity() <= 0) {
                    result.addError("El buffer '" + buf.id() + "' debe tener capacidad > 0 (actual: " + buf.capacity() + ").");
                }

                if (!computerMap.containsKey(buf.hostComputerId())) {
                    result.addError("El buffer '" + buf.id() + "' hace referencia al hostComputerId "
                            + buf.hostComputerId() + " que no existe en el clúster.");
                }
            }
        }

        // 4. Validar procesos
        ProcessConfig[] processes = config.processes();
        if (processes != null) {
            for (int i = 0; i < processes.length; i++) {
                ProcessConfig proc = processes[i];
                if (proc == null) {
                    result.addError("El proceso en la posición " + i + " es null.");
                    continue;
                }

                String procName = (proc.name() != null && !proc.name().isBlank()) ? proc.name() : ("Proceso#" + i);

                if (proc.name() == null || proc.name().isBlank()) {
                    result.addError("El proceso en la posición " + i + " tiene un nombre vacío o null.");
                }

                if (proc.instructions() <= 0) {
                    result.addError("El proceso '" + procName + "' debe tener instrucciones > 0 (actual: " + proc.instructions() + ").");
                }

                if (proc.priority() < 0) {
                    result.addError("El proceso '" + procName + "' debe tener prioridad >= 0 (actual: " + proc.priority() + ").");
                }

                if (proc.deadline() < 0) {
                    result.addError("El proceso '" + procName + "' no puede tener deadline negativo (actual: " + proc.deadline() + ").");
                }

                if (proc.type() == null) {
                    result.addError("El proceso '" + procName + "' debe tener un tipo definido.");
                }

                if (!computerMap.containsKey(proc.computerId())) {
                    result.addError("El proceso '" + procName + "' está asignado al computador ID "
                            + proc.computerId() + " que no existe en el clúster.");
                } else {
                    ComputerConfig hostComp = computerMap.get(proc.computerId());
                    if (proc.memoryRequired() > hostComp.ramSizeMB()) {
                        result.addError("El proceso '" + procName + "' requiere " + proc.memoryRequired()
                                + " MB de memoria, lo cual supera la capacidad total del computador "
                                + hostComp.id() + " (" + hostComp.ramSizeMB() + " MB).");
                    }
                }

                if (proc.memoryRequired() <= 0) {
                    result.addError("El proceso '" + procName + "' debe requerir memoria > 0 (actual: " + proc.memoryRequired() + ").");
                }

                // Reglas específicas para Productor / Consumidor
                if (proc.type() == ProcessType.PRODUCER || proc.type() == ProcessType.CONSUMER) {
                    if (proc.bufferId() == null || proc.bufferId().isBlank()) {
                        result.addError("El proceso " + proc.type() + " '" + procName + "' requiere un bufferId asignado.");
                    } else if (!bufferMap.containsKey(proc.bufferId())) {
                        result.addError("El proceso " + proc.type() + " '" + procName + "' referencia al buffer '"
                                + proc.bufferId() + "' que no existe.");
                    }

                    if (proc.produceConsumeInterval() <= 0) {
                        result.addError("El proceso " + proc.type() + " '" + procName + "' debe tener un intervalo > 0 (actual: "
                                + proc.produceConsumeInterval() + ").");
                    }

                    if (proc.elementsRequired() <= 0) {
                        result.addError("El proceso " + proc.type() + " '" + procName + "' debe tener elementos requeridos > 0 (actual: "
                                + proc.elementsRequired() + ").");
                    }
                }
            }
        }

        return result;
    }
}
