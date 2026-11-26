package avilaos.kernel;

import avilaos.core.disk.DiskBlockMap;
import avilaos.core.filesystem.FileSystemManager;
import avilaos.kernel.disk.DiskScheduler;
import avilaos.model.enumeration.*;
import avilaos.model.pcb.PCB;
import avilaos.simulation.SimulationCluster;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SwapIntegrationTest {
    @Test
    void blockedProcessMovesThroughSwapAndResumesAfterAdmission() {
        DiskBlockMap blocks = new DiskBlockMap(100);
        blocks.reserveSwapArea(48);
        SimulationCluster cluster = new SimulationCluster();
        var computer = cluster.addComputer(0, 64, SchedulingPolicyType.FCFS, 1);
        computer.attachDisk(new DiskScheduler(DiskPolicyType.FIFO), new FileSystemManager(blocks),
                50, HeadDirection.UP, 50);
        Kernel kernel = computer.getKernel();
        try {
            PCB blocked = new PCB("bloqueado", 0, ProcessType.CPU_BOUND, 0, 48, 500, 2);
            PCB waiting = new PCB("esperando", 0, ProcessType.CPU_BOUND, 0, 32, 500, 1);
            cluster.submitProcess(blocked);
            kernel.getReadyQueue().remove(blocked);
            blocked.setState(ProcessState.BLOCKED);
            blocked.setBlockReason(BlockReason.SEMAPHORE_WAIT);
            kernel.addToBlockedQueue(blocked);
            cluster.submitProcess(waiting);
            assertEquals(ProcessState.NEW, waiting.getState());
            for (int i = 0; i < 65 && blocked.getState() != ProcessState.BLOCKED_SUSPENDED; i++)
                cluster.advanceCycle();
            assertEquals(ProcessState.BLOCKED_SUSPENDED, blocked.getState());
            assertEquals(BlockUsage.SWAP, blocks.getBlock(52).getUsage());
            assertEquals(Long.toString(blocked.getId()), blocks.getBlock(52).getOwnerId());
            kernel.unblockProcess(blocked);
            assertEquals(ProcessState.READY_SUSPENDED, blocked.getState());
            for (int i = 0; i < 120 && blocked.getState() == ProcessState.READY_SUSPENDED; i++)
                cluster.advanceCycle();
            assertNotEquals(ProcessState.READY_SUSPENDED, blocked.getState());
            assertEquals("SWAP_LIBRE", blocks.getBlock(52).getOwnerId());
        } finally {
            cluster.shutdown();
        }
    }
}
