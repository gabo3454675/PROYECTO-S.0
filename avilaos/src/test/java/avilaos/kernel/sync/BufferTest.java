package avilaos.kernel.sync;

import avilaos.model.enumeration.BlockReason;
import avilaos.model.enumeration.ProcessState;
import avilaos.model.enumeration.ProcessType;
import avilaos.model.pcb.PCB;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Pruebas Unitarias - Buffer Acotado y Sincronización")
public class BufferTest {

    private Buffer buffer;
    private static final int HOST_COMPUTER_ID = 1;
    private static final int BUFFER_CAPACITY = 3;
    private static final int NETWORK_LATENCY = 2;

    @BeforeEach
    void setUp() {
        buffer = new Buffer("buf-1", BUFFER_CAPACITY, HOST_COMPUTER_ID, pcb -> {
            // Callback simulado que pone en READY
            pcb.setState(ProcessState.READY);
            pcb.setBlockReason(null);
        }, NETWORK_LATENCY);
    }

    private PCB createProducer(int computerId, String name) {
        PCB pcb = new PCB(name, computerId, ProcessType.PRODUCER, 1, 512, 200, 20);
        pcb.setState(ProcessState.RUNNING);
        return pcb;
    }

    private PCB createConsumer(int computerId, String name) {
        PCB pcb = new PCB(name, computerId, ProcessType.CONSUMER, 1, 512, 200, 20);
        pcb.setState(ProcessState.RUNNING);
        return pcb;
    }

    @Test
    @DisplayName("Inicialización correcta del buffer y sus semáforos")
    void testBufferInitialization() {
        assertEquals("buf-1", buffer.getId());
        assertEquals(BUFFER_CAPACITY, buffer.getCapacity());
        assertEquals(HOST_COMPUTER_ID, buffer.getHostComputerId());
        assertEquals(0, buffer.getCurrentSize());
        assertTrue(buffer.isEmpty());
        assertFalse(buffer.isFull());

        assertEquals(1, buffer.getSemMutex().getValue());
        assertEquals(BUFFER_CAPACITY, buffer.getSemEmpty().getValue());
        assertEquals(0, buffer.getSemFull().getValue());
    }

    @Test
    @DisplayName("Producción y consumo local exitoso")
    void testLocalPutAndGet() throws InterruptedException {
        PCB producer = createProducer(HOST_COMPUTER_ID, "ProdLocal");
        PCB consumer = createConsumer(HOST_COMPUTER_ID, "ConsLocal");

        boolean putSuccess = buffer.put(producer, "Item-1");
        assertTrue(putSuccess);
        assertEquals(1, buffer.getCurrentSize());
        assertFalse(buffer.isEmpty());

        Object item = buffer.get(consumer);
        assertNotNull(item);
        assertEquals("Item-1", item);
        assertEquals(0, buffer.getCurrentSize());
        assertTrue(buffer.isEmpty());
    }

    @Test
    @DisplayName("Buffer lleno bloquea al productor y se desbloquea al consumir")
    void testPutBlocksWhenBufferFull() throws InterruptedException {
        Buffer smallBuffer = new Buffer("small-buf", 2, HOST_COMPUTER_ID, pcb -> {
            pcb.setState(ProcessState.READY);
            pcb.setBlockReason(null);
        }, 0);

        PCB p1 = createProducer(HOST_COMPUTER_ID, "P1");
        PCB p2 = createProducer(HOST_COMPUTER_ID, "P2");
        PCB p3 = createProducer(HOST_COMPUTER_ID, "P3");
        PCB c1 = createConsumer(HOST_COMPUTER_ID, "C1");

        assertTrue(smallBuffer.put(p1, "Data-1"));
        assertTrue(smallBuffer.put(p2, "Data-2"));
        assertTrue(smallBuffer.isFull());

        // El tercer put debe bloquearse
        boolean putBlocked = smallBuffer.put(p3, "Data-3");
        assertFalse(putBlocked);
        assertEquals(ProcessState.BLOCKED, p3.getState());
        assertEquals(BlockReason.SEMAPHORE_WAIT, p3.getBlockReason());

        // Consumir un elemento debe desbloquear a P3
        Object item = smallBuffer.get(c1);
        assertEquals("Data-1", item);
        assertEquals(ProcessState.READY, p3.getState());
        assertNull(p3.getBlockReason());

        // Ahora P3 puede completar su put
        p3.setState(ProcessState.RUNNING);
        assertTrue(smallBuffer.put(p3, "Data-3"));
        assertEquals(2, smallBuffer.getCurrentSize());
    }

    @Test
    @DisplayName("Buffer vacío bloquea al consumidor y se desbloquea al producir")
    void testGetBlocksWhenBufferEmpty() throws InterruptedException {
        PCB consumer = createConsumer(HOST_COMPUTER_ID, "ConsEmpty");
        PCB producer = createProducer(HOST_COMPUTER_ID, "ProdItem");

        Object item = buffer.get(consumer);
        assertNull(item);
        assertEquals(ProcessState.BLOCKED, consumer.getState());
        assertEquals(BlockReason.SEMAPHORE_WAIT, consumer.getBlockReason());

        // Producir un elemento debe desbloquear al consumidor
        assertTrue(buffer.put(producer, "Arrived-Item"));
        assertEquals(ProcessState.READY, consumer.getState());
        assertNull(consumer.getBlockReason());

        // Consumidor reintenta y obtiene el elemento
        consumer.setState(ProcessState.RUNNING);
        Object consumed = buffer.get(consumer);
        assertEquals("Arrived-Item", consumed);
        assertEquals(0, buffer.getCurrentSize());
    }

    @Test
    @DisplayName("Acceso remoto aplica latencia de red y bloquea temporalmente")
    void testRemoteAccessNetworkLatency() throws InterruptedException {
        int remoteComputerId = 2; // Diferente a hostComputerId (1)
        PCB remoteProducer = createProducer(remoteComputerId, "RemoteProd");

        // Intento 1: se bloquea por latencia de red
        boolean putResult1 = buffer.put(remoteProducer, "RemoteData");
        assertFalse(putResult1);
        assertEquals(ProcessState.BLOCKED, remoteProducer.getState());
        assertEquals(BlockReason.NETWORK_LATENCY, remoteProducer.getBlockReason());
        assertEquals(NETWORK_LATENCY, remoteProducer.getNetworkLatencyRemaining());

        // Simular paso de ciclos de reloj (reducción de latencia)
        remoteProducer.decrementNetworkLatency();
        assertEquals(1, remoteProducer.getNetworkLatencyRemaining());
        // Mientras tenga latencia restante no puede insertar
        assertFalse(buffer.put(remoteProducer, "RemoteData"));

        // Último ciclo de latencia consumido
        remoteProducer.decrementNetworkLatency();
        assertEquals(0, remoteProducer.getNetworkLatencyRemaining());
        remoteProducer.setState(ProcessState.READY);
        remoteProducer.setBlockReason(null);

        // Intento tras cumplirse la latencia: inserción exitosa
        remoteProducer.setState(ProcessState.RUNNING);
        boolean putResult2 = buffer.put(remoteProducer, "RemoteData");
        assertTrue(putResult2);
        assertEquals(1, buffer.getCurrentSize());
    }

    @Test
    @DisplayName("Múltiples productores y consumidores sin condiciones de carrera ni interbloqueos")
    void testMultiProducerMultiConsumerScenario() throws InterruptedException {
        Buffer testBuf = new Buffer("multi-buf", 5, HOST_COMPUTER_ID, pcb -> {
            pcb.setState(ProcessState.READY);
            pcb.setBlockReason(null);
        }, 0);

        PCB prod1 = createProducer(HOST_COMPUTER_ID, "P1");
        PCB prod2 = createProducer(HOST_COMPUTER_ID, "P2");
        PCB cons1 = createConsumer(HOST_COMPUTER_ID, "C1");
        PCB cons2 = createConsumer(HOST_COMPUTER_ID, "C2");

        AtomicInteger itemsProcessed = new AtomicInteger(0);

        // 10 rondas de producción y consumo alternadas
        for (int i = 0; i < 10; i++) {
            assertTrue(testBuf.put(prod1, "P1-" + i));
            assertTrue(testBuf.put(prod2, "P2-" + i));

            Object it1 = testBuf.get(cons1);
            Object it2 = testBuf.get(cons2);

            assertNotNull(it1);
            assertNotNull(it2);
            itemsProcessed.addAndGet(2);
        }

        assertEquals(20, itemsProcessed.get());
        assertEquals(0, testBuf.getCurrentSize());
        assertTrue(testBuf.isEmpty());
    }
}
