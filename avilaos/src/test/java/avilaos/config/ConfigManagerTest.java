package avilaos.config;

import avilaos.core.hardware.CPU;
import avilaos.core.hardware.Computer;
import avilaos.kernel.Kernel;
import avilaos.model.enumeration.ProcessType;
import avilaos.model.enumeration.SchedulingPolicyType;
import avilaos.model.pcb.PCB;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

public class ConfigManagerTest {

    private static final String TEST_FILE_PATH = "test_config.json";
    private Kernel kernel;

    @BeforeEach
    public void setUp() {
        Computer computer = new Computer(0, 1024L, SchedulingPolicyType.FCFS, 2);
        kernel = new Kernel(computer, SchedulingPolicyType.FCFS, 2);
    }

    @AfterEach
    public void tearDown() {
        File file = new File(TEST_FILE_PATH);
        if (file.exists()) {
            file.delete();
        }
    }

    @Test
    public void testSaveAndLoadConfig() {
        try {
            // 1. Agregar buffers y procesos iniciales
            kernel.getSyncManager().createBuffer("buf_1", 10, 0);

            ProcessType defaultType = (ProcessType.values().length > 0) ? ProcessType.values()[0] : null;
            PCB process = new PCB("Process_Test", 0, defaultType, 1, 100L, 50L, 50L);
            kernel.submitProcess(process);

            // 2. Guardar a archivo JSON
            ConfigManager.saveConfigToJson(TEST_FILE_PATH, kernel);

            File configFile = new File(TEST_FILE_PATH);
            assertTrue(configFile.exists(), "El archivo de configuración JSON debe crearse.");

            // 3. Crear nuevo Kernel y cargar la configuración guardada
            Computer newComputer = new Computer(1, 2048L, SchedulingPolicyType.FCFS, 2);
            Kernel newKernel = new Kernel(newComputer, SchedulingPolicyType.FCFS, 2);

            ConfigManager.loadConfigFromJson(TEST_FILE_PATH, newKernel);

            // 4. Verificar que el buffer se cargó correctamente
            assertNotNull(newKernel.getSyncManager().getAllBuffers(), "Los buffers deben cargarse.");
            assertTrue(newKernel.getSyncManager().getAllBuffers().size() > 0, "Debe existir al menos un buffer cargado.");

        } catch (IOException e) {
            fail("No debería ocurrir una excepción durante la prueba de guardado/carga: " + e.getMessage());
        }
    }
}