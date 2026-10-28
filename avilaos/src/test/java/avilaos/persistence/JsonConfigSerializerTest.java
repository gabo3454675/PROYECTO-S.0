package avilaos.persistence;

import avilaos.model.dto.BufferConfig;
import avilaos.model.dto.ComputerConfig;
import avilaos.model.dto.ProcessConfig;
import avilaos.model.dto.SimulationConfig;
import avilaos.model.enumeration.ProcessType;
import avilaos.model.enumeration.SchedulingPolicyType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

public class JsonConfigSerializerTest {

    private static final String TEST_FILE = "target/test_simulation_config.json";
    private JsonConfigSerializer serializer;

    @BeforeEach
    public void setUp() {
        serializer = new JsonConfigSerializer();
    }

    @AfterEach
    public void tearDown() {
        File file = new File(TEST_FILE);
        if (file.exists()) {
            file.delete();
        }
    }

    private SimulationConfig createSampleConfig() {
        ComputerConfig[] computers = new ComputerConfig[]{
                new ComputerConfig(0, 1024L, SchedulingPolicyType.ROUND_ROBIN, 2),
                new ComputerConfig(1, 2048L, SchedulingPolicyType.PRIORITY_PREEMPTIVE, 4)
        };

        BufferConfig[] buffers = new BufferConfig[]{
                new BufferConfig("buf_main", 10, 0)
        };

        ProcessConfig[] processes = new ProcessConfig[]{
                new ProcessConfig("Worker_1", 100, 256L, 1, 50L, ProcessType.CPU_BOUND, 0, null, 0, 0),
                new ProcessConfig("Prod_1", 60, 128L, 2, 40L, ProcessType.PRODUCER, 1, "buf_main", 2, 5),
                new ProcessConfig("Cons_1", 80, 128L, 2, 45L, ProcessType.CONSUMER, 0, "buf_main", 3, 5)
        };

        return new SimulationConfig(100, 2, computers, processes, buffers);
    }

    @Test
    public void testSerializeAndDeserializeString() throws IOException {
        SimulationConfig original = createSampleConfig();

        String json = serializer.serializeToString(original);
        assertNotNull(json, "El JSON serializado no debe ser null.");
        assertTrue(json.contains("\"cycleDurationMs\" : 100"), "Debe contener cycleDurationMs.");
        assertTrue(json.contains("\"buf_main\""), "Debe contener el buffer buf_main.");

        SimulationConfig deserialized = serializer.deserializeFromString(json);
        assertNotNull(deserialized, "La configuración deserializada no debe ser null.");
        assertEquals(original.cycleDurationMs(), deserialized.cycleDurationMs());
        assertEquals(original.networkLatencyCycles(), deserialized.networkLatencyCycles());
        assertEquals(2, deserialized.computers().length);
        assertEquals(1, deserialized.buffers().length);
        assertEquals(3, deserialized.processes().length);

        // Validar computadores
        assertEquals(0, deserialized.computers()[0].id());
        assertEquals(1024L, deserialized.computers()[0].ramSizeMB());
        assertEquals(SchedulingPolicyType.ROUND_ROBIN, deserialized.computers()[0].initialPolicy());
        assertEquals(2, deserialized.computers()[0].quantum());

        // Validar buffer
        assertEquals("buf_main", deserialized.buffers()[0].id());
        assertEquals(10, deserialized.buffers()[0].capacity());
        assertEquals(0, deserialized.buffers()[0].hostComputerId());

        // Validar procesos
        assertEquals("Worker_1", deserialized.processes()[0].name());
        assertEquals(ProcessType.CPU_BOUND, deserialized.processes()[0].type());
        assertEquals("Prod_1", deserialized.processes()[1].name());
        assertEquals(ProcessType.PRODUCER, deserialized.processes()[1].type());
        assertEquals("buf_main", deserialized.processes()[1].bufferId());
    }

    @Test
    public void testSerializeAndDeserializeFile() throws IOException {
        SimulationConfig original = createSampleConfig();
        File file = new File(TEST_FILE);

        serializer.serialize(original, file);
        assertTrue(file.exists(), "El archivo JSON debe haberse creado.");
        assertTrue(file.length() > 0, "El archivo debe tener contenido.");

        SimulationConfig loaded = serializer.deserialize(file);
        assertNotNull(loaded);
        assertEquals(original.cycleDurationMs(), loaded.cycleDurationMs());
        assertEquals(original.computers().length, loaded.computers().length);
        assertEquals(original.buffers().length, loaded.buffers().length);
        assertEquals(original.processes().length, loaded.processes().length);
    }

    @Test
    public void testDeserializeInvalidJsonThrowsIOException() {
        String invalidJson = "{ \"cycleDurationMs\": \"NOT_A_NUMBER\" ";
        assertThrows(IOException.class, () -> serializer.deserializeFromString(invalidJson));
    }

    @Test
    public void testDeserializeEmptyOrBlankThrowsException() {
        assertThrows(IllegalArgumentException.class, () -> serializer.deserializeFromString(""));
        assertThrows(IllegalArgumentException.class, () -> serializer.deserializeFromString("   "));
        assertThrows(IllegalArgumentException.class, () -> serializer.deserializeFromString(null));
    }

    @Test
    public void testSerializeNullThrowsException() {
        assertThrows(IllegalArgumentException.class, () -> serializer.serializeToString(null));
        assertThrows(IllegalArgumentException.class, () -> serializer.serialize(null, new File(TEST_FILE)));
    }

    @Test
    public void testDeserializeNonExistentFileThrowsIOException() {
        File nonExistent = new File("non_existent_dir/non_existent_file.json");
        assertThrows(IOException.class, () -> serializer.deserialize(nonExistent));
    }

    @Test
    public void testLoadDefaultSimulationFile() throws IOException {
        File defaultFile = new File("config/default-simulation.json");
        assertTrue(defaultFile.exists(), "El archivo default-simulation.json debe existir en el directorio config.");

        SimulationConfig config = serializer.deserialize(defaultFile);
        assertNotNull(config);
        assertEquals(2, config.computers().length);
        assertEquals(2, config.buffers().length);
        assertEquals(5, config.processes().length);

        SimulationConfigValidator validator = new SimulationConfigValidator();
        ValidationResult result = validator.validate(config);
        assertTrue(result.isValid(), "El archivo default-simulation.json debe pasar todas las validaciones.");
    }
}
