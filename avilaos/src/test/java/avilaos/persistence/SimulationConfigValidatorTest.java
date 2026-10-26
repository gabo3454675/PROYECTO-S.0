package avilaos.persistence;

import avilaos.model.dto.BufferConfig;
import avilaos.model.dto.ComputerConfig;
import avilaos.model.dto.ProcessConfig;
import avilaos.model.dto.SimulationConfig;
import avilaos.model.enumeration.ProcessType;
import avilaos.model.enumeration.SchedulingPolicyType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class SimulationConfigValidatorTest {

    private SimulationConfigValidator validator;

    @BeforeEach
    public void setUp() {
        validator = new SimulationConfigValidator();
    }

    private SimulationConfig createValidConfig() {
        ComputerConfig[] computers = new ComputerConfig[]{
                new ComputerConfig(0, 1024L, SchedulingPolicyType.FCFS, 2),
                new ComputerConfig(1, 2048L, SchedulingPolicyType.ROUND_ROBIN, 4)
        };

        BufferConfig[] buffers = new BufferConfig[]{
                new BufferConfig("buf_0", 5, 0)
        };

        ProcessConfig[] processes = new ProcessConfig[]{
                new ProcessConfig("P1", 50, 200L, 1, 30L, ProcessType.CPU_BOUND, 0, null, 0, 0),
                new ProcessConfig("Prod", 40, 100L, 2, 25L, ProcessType.PRODUCER, 1, "buf_0", 2, 4)
        };

        return new SimulationConfig(50, 1, computers, processes, buffers);
    }

    @Test
    public void testValidConfigPassesValidation() {
        SimulationConfig config = createValidConfig();
        ValidationResult result = validator.validate(config);

        assertTrue(result.isValid(), "Una configuración válida debe pasar la validación.");
        assertTrue(result.getErrors().isEmpty(), "No deben haber errores.");
        assertEquals("Sin errores.", result.getFormattedErrors());
    }

    @Test
    public void testNullConfigReturnsError() {
        ValidationResult result = validator.validate(null);
        assertFalse(result.isValid());
        assertTrue(result.getErrors().size() > 0);
        assertTrue(result.getFormattedErrors().contains("null"));
    }

    @Test
    public void testDuplicateComputerIdsDetected() {
        ComputerConfig[] computers = new ComputerConfig[]{
                new ComputerConfig(0, 1024L, SchedulingPolicyType.FCFS, 2),
                new ComputerConfig(0, 2048L, SchedulingPolicyType.ROUND_ROBIN, 4)
        };

        SimulationConfig config = new SimulationConfig(50, 1, computers, new ProcessConfig[0], new BufferConfig[0]);
        ValidationResult result = validator.validate(config);

        assertFalse(result.isValid());
        boolean foundDuplicateError = false;
        for (int i = 0; i < result.getErrors().size(); i++) {
            if (result.getErrors().get(i).contains("duplicado")) {
                foundDuplicateError = true;
                break;
            }
        }
        assertTrue(foundDuplicateError, "Debe reportar error de ID de computador duplicado.");
    }

    @Test
    public void testDuplicateBufferIdsDetected() {
        ComputerConfig[] computers = new ComputerConfig[]{
                new ComputerConfig(0, 1024L, SchedulingPolicyType.FCFS, 2)
        };

        BufferConfig[] buffers = new BufferConfig[]{
                new BufferConfig("b1", 5, 0),
                new BufferConfig("b1", 10, 0)
        };

        SimulationConfig config = new SimulationConfig(50, 1, computers, new ProcessConfig[0], buffers);
        ValidationResult result = validator.validate(config);

        assertFalse(result.isValid());
        boolean foundDuplicateError = false;
        for (int i = 0; i < result.getErrors().size(); i++) {
            if (result.getErrors().get(i).contains("duplicado")) {
                foundDuplicateError = true;
                break;
            }
        }
        assertTrue(foundDuplicateError, "Debe reportar error de ID de buffer duplicado.");
    }

    @Test
    public void testBufferHostComputerDoesNotExist() {
        ComputerConfig[] computers = new ComputerConfig[]{
                new ComputerConfig(0, 1024L, SchedulingPolicyType.FCFS, 2)
        };

        BufferConfig[] buffers = new BufferConfig[]{
                new BufferConfig("b1", 5, 99) // Computer 99 no existe
        };

        SimulationConfig config = new SimulationConfig(50, 1, computers, new ProcessConfig[0], buffers);
        ValidationResult result = validator.validate(config);

        assertFalse(result.isValid());
        assertTrue(result.getFormattedErrors().contains("hostComputerId 99 que no existe"));
    }

    @Test
    public void testProcessComputerDoesNotExist() {
        ComputerConfig[] computers = new ComputerConfig[]{
                new ComputerConfig(0, 1024L, SchedulingPolicyType.FCFS, 2)
        };

        ProcessConfig[] processes = new ProcessConfig[]{
                new ProcessConfig("P_Orphan", 50, 100L, 1, 30L, ProcessType.CPU_BOUND, 42, null, 0, 0)
        };

        SimulationConfig config = new SimulationConfig(50, 1, computers, processes, new BufferConfig[0]);
        ValidationResult result = validator.validate(config);

        assertFalse(result.isValid());
        assertTrue(result.getFormattedErrors().contains("computador ID 42 que no existe"));
    }

    @Test
    public void testProcessMemoryExceedsComputerRam() {
        ComputerConfig[] computers = new ComputerConfig[]{
                new ComputerConfig(0, 512L, SchedulingPolicyType.FCFS, 2)
        };

        ProcessConfig[] processes = new ProcessConfig[]{
                new ProcessConfig("Heavy_Proc", 50, 1024L, 1, 30L, ProcessType.CPU_BOUND, 0, null, 0, 0)
        };

        SimulationConfig config = new SimulationConfig(50, 1, computers, processes, new BufferConfig[0]);
        ValidationResult result = validator.validate(config);

        assertFalse(result.isValid());
        assertTrue(result.getFormattedErrors().contains("supera la capacidad total del computador"));
    }

    @Test
    public void testProducerConsumerBufferDoesNotExist() {
        ComputerConfig[] computers = new ComputerConfig[]{
                new ComputerConfig(0, 1024L, SchedulingPolicyType.FCFS, 2)
        };

        BufferConfig[] buffers = new BufferConfig[]{
                new BufferConfig("buf_valid", 5, 0)
        };

        ProcessConfig[] processes = new ProcessConfig[]{
                new ProcessConfig("Prod_Ghost", 40, 100L, 2, 25L, ProcessType.PRODUCER, 0, "non_existent_buf", 2, 4)
        };

        SimulationConfig config = new SimulationConfig(50, 1, computers, processes, buffers);
        ValidationResult result = validator.validate(config);

        assertFalse(result.isValid());
        assertTrue(result.getFormattedErrors().contains("referencia al buffer 'non_existent_buf' que no existe"));
    }
}
