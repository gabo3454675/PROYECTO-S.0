package avilaos.core.hardware;

import avilaos.core.disk.DiskBlockMap;
import avilaos.core.filesystem.FileSystemManager;
import avilaos.kernel.disk.DiskScheduler;
import avilaos.model.enumeration.DiskPolicyType;
import avilaos.model.enumeration.HeadDirection;
import avilaos.model.enumeration.ProcessState;
import avilaos.model.enumeration.SchedulingPolicyType;
import avilaos.model.enumeration.IOOperationType;
import avilaos.model.disk.DiskRequest;
import avilaos.model.pcb.PCB;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class DiskClockTest {
    @Test
    void advancesHeadThenWritesOneBlockPerCycleAndWakesProcess() {
        Computer computer = new Computer(1, 1024, SchedulingPolicyType.FCFS, 2);
        try {
            FileSystemManager files = new FileSystemManager(new DiskBlockMap(100));
            DiskScheduler scheduler = new DiskScheduler(DiskPolicyType.FIFO);
            computer.attachDisk(scheduler, files, 0, HeadDirection.UP, 2);
            PCB creator = PCB.forFileCreate("creator", 1, 1, 32, 100, 5,
                    "/result.txt", 3, "admin", true);
            computer.getKernel().submitProcess(creator);

            computer.executeCycle(1);
            assertEquals(ProcessState.BLOCKED, creator.getState());
            computer.executeCycle(2);
            assertNull(files.resolve("/result.txt"));
            computer.executeCycle(3);
            assertEquals(ProcessState.READY, creator.getState());
            assertNotNull(files.resolve("/result.txt"));
            assertEquals(1, computer.getDisk().getCompletedRequests());
        } finally {
            computer.shutdown();
        }
    }

    @Test
    void waitsForSeekAndEachDataBlock() {
        Computer computer = new Computer(1, 1024, SchedulingPolicyType.FCFS, 2);
        try {
            FileSystemManager files = new FileSystemManager(new DiskBlockMap(100));
            files.createFile("/", "data.txt", 3, "admin", true, "seed", 5);
            DiskScheduler scheduler = new DiskScheduler(DiskPolicyType.FIFO);
            computer.attachDisk(scheduler, files, 0, HeadDirection.UP, 2);
            PCB reader = PCB.forFileRead("reader", 1, 1, 32, 100, 5, "/data.txt");
            computer.getKernel().submitProcess(reader);
            computer.executeCycle(1);
            assertEquals(2, computer.getDisk().getHeadPosition());
            computer.executeCycle(2);
            assertEquals(4, computer.getDisk().getHeadPosition());
            computer.executeCycle(3);
            assertEquals(5, computer.getDisk().getHeadPosition());
            assertEquals(0, computer.getDisk().getCompletedRequests());
            computer.executeCycle(4);
            computer.executeCycle(5);
            assertEquals(ProcessState.BLOCKED, reader.getState());
            computer.executeCycle(6);
            assertEquals(ProcessState.READY, reader.getState());
            assertEquals(5, computer.getDisk().getTotalHeadMovement());
            assertEquals(1, computer.getDisk().getCompletedRequests());
        } finally {
            computer.shutdown();
        }
    }

    @Test
    void scanReachesBoundaryBeforeReversingAndCscanWrapDoesNotCountJump() {
        for (DiskPolicyType policy : new DiskPolicyType[]{DiskPolicyType.SCAN, DiskPolicyType.C_SCAN}) {
            Computer computer = new Computer(1, 1024, SchedulingPolicyType.FCFS, 2);
            try {
                FileSystemManager files = new FileSystemManager(new DiskBlockMap(100));
                DiskScheduler scheduler = new DiskScheduler(policy);
                computer.attachDisk(scheduler, files, 98, HeadDirection.UP, 1);
                scheduler.submit(new DiskRequest(999, "probe", IOOperationType.READ,
                        "/missing", 1, 0));
                computer.executeCycle(1);
                assertEquals(policy == DiskPolicyType.C_SCAN ? 0 : 99,
                        computer.getDisk().getHeadPosition());
                computer.executeCycle(2);
                assertEquals(policy == DiskPolicyType.C_SCAN ? 1 : 98,
                        computer.getDisk().getHeadPosition());
                assertEquals(2,
                        computer.getDisk().getTotalHeadMovement());
                assertEquals(policy == DiskPolicyType.C_SCAN ? HeadDirection.UP : HeadDirection.DOWN,
                        computer.getDisk().getDirection());
            } finally {
                computer.shutdown();
            }
        }
    }
}
