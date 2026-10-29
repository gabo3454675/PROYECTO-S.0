package avilaos.kernel.sync;

import avilaos.model.enumeration.BlockReason;
import avilaos.model.enumeration.ProcessState;
import avilaos.model.enumeration.ProcessType;
import avilaos.model.pcb.PCB;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Pruebas Unitarias - Semáforo Propio")
public class SemaphoreTest {

    private Semaphore semaphore;

    @BeforeEach
    void setUp() {
        semaphore = new Semaphore(1);
    }

    private PCB createTestProcess(String name) {
        PCB pcb = new PCB(name, 1, ProcessType.CPU_BOUND, 1, 1024, 100, 10);
        pcb.setState(ProcessState.RUNNING);
        return pcb;
    }

    @Test
    @DisplayName("Valor inicial se asigna correctamente")
    void testInitialValue() {
        assertEquals(1, semaphore.getValue());
        assertEquals(0, semaphore.getWaitingCount());
        assertTrue(semaphore.getWaitingQueue().isEmpty());
    }

    @Test
    @DisplayName("wait() exitoso decrementa valor sin bloquear")
    void testWaitNonBlocking() {
        PCB pcb = createTestProcess("P1");
        semaphore.wait(pcb);

        assertEquals(0, semaphore.getValue());
        assertEquals(ProcessState.RUNNING, pcb.getState());
        assertNull(pcb.getBlockReason());
        assertEquals(0, semaphore.getWaitingCount());
    }

    @Test
    @DisplayName("wait() bloquea cuando no hay recursos disponibles")
    void testWaitBlocking() {
        PCB pcb1 = createTestProcess("P1");
        PCB pcb2 = createTestProcess("P2");

        semaphore.wait(pcb1);
        semaphore.wait(pcb2);

        assertEquals(-1, semaphore.getValue());
        assertEquals(ProcessState.BLOCKED, pcb2.getState());
        assertEquals(BlockReason.SEMAPHORE_WAIT, pcb2.getBlockReason());
        assertEquals(1, semaphore.getWaitingCount());
    }

    @Test
    @DisplayName("Bloqueo de múltiples procesos respeta cola FIFO")
    void testFIFOQueueOrder() {
        Semaphore sem = new Semaphore(0);
        PCB pcb1 = createTestProcess("P1");
        PCB pcb2 = createTestProcess("P2");
        PCB pcb3 = createTestProcess("P3");

        sem.wait(pcb1);
        sem.wait(pcb2);
        sem.wait(pcb3);

        assertEquals(-3, sem.getValue());
        assertEquals(3, sem.getWaitingCount());

        sem.signal();
        assertEquals(ProcessState.READY, pcb1.getState());
        assertNull(pcb1.getBlockReason());
        assertEquals(2, sem.getWaitingCount());

        sem.signal();
        assertEquals(ProcessState.READY, pcb2.getState());
        assertEquals(1, sem.getWaitingCount());

        sem.signal();
        assertEquals(ProcessState.READY, pcb3.getState());
        assertEquals(0, sem.getWaitingCount());
    }

    @Test
    @DisplayName("signal() invoca el callback de desbloqueo configurado")
    void testUnblockCallback() {
        Semaphore sem = new Semaphore(0);
        PCB pcb = createTestProcess("BlockedProc");

        AtomicReference<PCB> unblockedPCB = new AtomicReference<>();
        sem.setOnUnblockCallback(unblockedPCB::set);

        sem.wait(pcb);
        assertEquals(ProcessState.BLOCKED, pcb.getState());

        sem.signal();
        assertNotNull(unblockedPCB.get());
        assertEquals(pcb.getId(), unblockedPCB.get().getId());
        assertEquals(ProcessState.READY, unblockedPCB.get().getState());
    }

    @Test
    @DisplayName("signal() sin procesos en espera incrementa el valor")
    void testSignalWithoutWaiters() {
        semaphore.signal();
        assertEquals(2, semaphore.getValue());
        assertEquals(0, semaphore.getWaitingCount());
    }

    @Test
    @DisplayName("Cancelar una espera restaura el contador y quita el proceso de la cola")
    void testCancelWaitRestoresSemaphoreCount() {
        Semaphore sem = new Semaphore(0);
        PCB pcb = createTestProcess("CancelledProc");
        sem.wait(pcb);

        assertTrue(sem.cancelWait(pcb));
        assertFalse(sem.cancelWait(pcb));
        assertEquals(0, sem.getValue());
        assertEquals(0, sem.getWaitingCount());
    }
}
