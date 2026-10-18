package avilaos.kernel.sync;

import avilaos.model.enumeration.ProcessState;
import avilaos.model.pcb.PCB;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Pruebas Unitarias - SyncManager")
public class SyncManagerTest {

    private SyncManager syncManager;

    @BeforeEach
    void setUp() {
        syncManager = new SyncManager(pcb -> {
            pcb.setState(ProcessState.READY);
            pcb.setBlockReason(null);
        });
    }

    @Test
    @DisplayName("Creación, obtención y conteo de buffers")
    void testCreateAndGetBuffer() {
        assertEquals(0, syncManager.getBufferCount());

        Buffer b1 = syncManager.createBuffer("B1", 10, 1);
        assertNotNull(b1);
        assertEquals("B1", b1.getId());
        assertEquals(10, b1.getCapacity());
        assertEquals(1, b1.getHostComputerId());
        assertEquals(1, syncManager.getBufferCount());

        Buffer fetched = syncManager.getBuffer("B1");
        assertNotNull(fetched);
        assertSame(b1, fetched);

        assertNull(syncManager.getBuffer("INEXISTENTE"));
    }

    @Test
    @DisplayName("Eliminación de buffers")
    void testRemoveBuffer() {
        syncManager.createBuffer("B1", 5, 1);
        syncManager.createBuffer("B2", 5, 2);
        assertEquals(2, syncManager.getBufferCount());

        syncManager.removeBuffer("B1");
        assertEquals(1, syncManager.getBufferCount());
        assertNull(syncManager.getBuffer("B1"));
        assertNotNull(syncManager.getBuffer("B2"));
    }

    @Test
    @DisplayName("Configuración y propagación de latencia de red")
    void testNetworkLatencyConfig() {
        assertEquals(3, syncManager.getNetworkLatencyCycles()); // default

        syncManager.setNetworkLatencyCycles(5);
        assertEquals(5, syncManager.getNetworkLatencyCycles());

        Buffer b = syncManager.createBuffer("B_LAT", 5, 1);
        assertEquals(5, b.getNetworkLatencyCycles());
    }
}
