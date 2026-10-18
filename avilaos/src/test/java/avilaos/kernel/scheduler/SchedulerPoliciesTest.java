package avilaos.kernel.scheduler;

import avilaos.core.hardware.Computer;
import avilaos.model.enumeration.ProcessState;
import avilaos.model.enumeration.ProcessType;
import avilaos.model.enumeration.SchedulingPolicyType;
import avilaos.model.pcb.PCB;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Pruebas Unitarias - Políticas de Planificación y Cambio en Caliente")
public class SchedulerPoliciesTest {

    private PCB createProcess(String name, int priority, long deadline, long remainingTime) {
        return new PCB(name, 1, ProcessType.CPU_BOUND, priority, 512, deadline, remainingTime);
    }

    @Test
    @DisplayName("FCFS selecciona procesos estrictamente por orden de llegada (FIFO)")
    void testFCFSPolicyOrder() {
        Scheduler scheduler = new Scheduler(new FCFSFPolicy());

        PCB p1 = createProcess("P1", 3, 100, 10);
        PCB p2 = createProcess("P2", 1, 50, 10);
        PCB p3 = createProcess("P3", 2, 20, 10);

        scheduler.addToReady(p1);
        scheduler.addToReady(p2);
        scheduler.addToReady(p3);

        assertEquals("P1", scheduler.selectNext().getName());
        assertEquals("P2", scheduler.selectNext().getName());
        assertEquals("P3", scheduler.selectNext().getName());
        assertNull(scheduler.selectNext());
    }

    @Test
    @DisplayName("EDF (Earliest Deadline First) prioriza el proceso con menor deadline")
    void testEDFPolicyEarliestDeadlineFirst() {
        Scheduler scheduler = new Scheduler(new EDFPolicy());

        PCB p1 = createProcess("P1", 1, 80, 10);
        PCB p2 = createProcess("P2", 2, 20, 10);
        PCB p3 = createProcess("P3", 3, 50, 10);

        scheduler.addToReady(p1);
        scheduler.addToReady(p2);
        scheduler.addToReady(p3);

        assertEquals("P2", scheduler.selectNext().getName()); // deadline 20
        assertEquals("P3", scheduler.selectNext().getName()); // deadline 50
        assertEquals("P1", scheduler.selectNext().getName()); // deadline 80
    }

    @Test
    @DisplayName("Priority Preemptive prioriza el proceso con menor valor numérico de prioridad")
    void testPriorityPreemptivePolicyOrder() {
        Scheduler scheduler = new Scheduler(new PriorityPreemptivePolicy());

        PCB p1 = createProcess("P1", 5, 100, 10);
        PCB p2 = createProcess("P2", 1, 100, 10);
        PCB p3 = createProcess("P3", 3, 100, 10);

        scheduler.addToReady(p1);
        scheduler.addToReady(p2);
        scheduler.addToReady(p3);

        assertEquals("P2", scheduler.selectNext().getName()); // prioridad 1 (mayor)
        assertEquals("P3", scheduler.selectNext().getName()); // prioridad 3
        assertEquals("P1", scheduler.selectNext().getName()); // prioridad 5
    }

    @Test
    @DisplayName("Cambio de política en tiempo de ejecución (Runtime Switchable) reordena la cola")
    void testRuntimePolicySwitching() {
        Scheduler scheduler = new Scheduler(new FCFSFPolicy());

        PCB p1 = createProcess("P1", 5, 80, 10);
        PCB p2 = createProcess("P2", 1, 20, 10);
        PCB p3 = createProcess("P3", 3, 50, 10);

        scheduler.addToReady(p1);
        scheduler.addToReady(p2);
        scheduler.addToReady(p3);

        // En FCFS el siguiente sería P1
        assertEquals(SchedulingPolicyType.FCFS, scheduler.getPolicy().getType());

        // Cambio a EDF: el orden debe pasar a P2 (20), P3 (50), P1 (80)
        scheduler.setPolicy(new EDFPolicy());
        assertEquals(SchedulingPolicyType.EDF, scheduler.getPolicy().getType());
        assertEquals("P2", scheduler.selectNext().getName());

        // Cambio a Priority: entre P1 (5) y P3 (3), debe ganar P3
        scheduler.setPolicy(new PriorityPreemptivePolicy());
        assertEquals(SchedulingPolicyType.PRIORITY_PREEMPTIVE, scheduler.getPolicy().getType());
        assertEquals("P3", scheduler.selectNext().getName());
        assertEquals("P1", scheduler.selectNext().getName());
    }

    @Test
    @DisplayName("Preempción por prioridad en el Kernel cuando llega un proceso más prioritario")
    void testPriorityPreemptionInKernelCycle() {
        Computer computer = new Computer(1, 4096, SchedulingPolicyType.PRIORITY_PREEMPTIVE, 2);

        PCB pLow = createProcess("PLow", 5, 200, 20);
        computer.getKernel().submitProcess(pLow);

        // Ciclo 1: PLow entra a la CPU
        computer.executeCycle(1);
        assertEquals(pLow, computer.getCpu().getCurrentProcess());
        assertEquals(ProcessState.RUNNING, pLow.getState());

        // Llega un proceso de alta prioridad
        PCB pHigh = createProcess("PHigh", 1, 200, 20);
        computer.getKernel().submitProcess(pHigh);

        // Ciclo 2: Se activa la preempción; PLow vuelve a READY y PHigh toma la CPU
        computer.executeCycle(2);
        assertEquals(pHigh, computer.getCpu().getCurrentProcess());
        assertEquals(ProcessState.RUNNING, pHigh.getState());
        assertEquals(ProcessState.READY, pLow.getState());
    }

    @Test
    @DisplayName("Round Robin rota procesos al expirar el quantum")
    void testRoundRobinQuantumExpirationInKernel() {
        int quantum = 2;
        Computer computer = new Computer(1, 4096, SchedulingPolicyType.ROUND_ROBIN, quantum);

        PCB p1 = createProcess("P1", 1, 200, 10);
        PCB p2 = createProcess("P2", 1, 200, 10);

        computer.getKernel().submitProcess(p1);
        computer.getKernel().submitProcess(p2);

        // Ciclo 1: P1 corre (quantum 1/2)
        computer.executeCycle(1);
        assertEquals(p1, computer.getCpu().getCurrentProcess());
        assertEquals(1, p1.getQuantumUsed());

        // Ciclo 2: P1 corre (quantum 2/2 -> expira y vuelve a READY al final del ciclo)
        computer.executeCycle(2);

        // Ciclo 3: P2 debe haber sido seleccionado por Round Robin
        computer.executeCycle(3);
        assertEquals(p2, computer.getCpu().getCurrentProcess());
    }
}
