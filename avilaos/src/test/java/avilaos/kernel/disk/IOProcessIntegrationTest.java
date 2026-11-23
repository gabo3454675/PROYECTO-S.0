package avilaos.kernel.disk;

import avilaos.core.disk.DiskBlockMap;
import avilaos.core.filesystem.FileSystemManager;
import avilaos.core.filesystem.FileSystemNode;
import avilaos.core.hardware.Computer;
import avilaos.kernel.Kernel;
import avilaos.model.disk.DiskRequest;
import avilaos.model.disk.DiskSelection;
import avilaos.model.enumeration.BlockReason;
import avilaos.model.enumeration.DiskPolicyType;
import avilaos.model.enumeration.ExecutionMode;
import avilaos.model.enumeration.HeadDirection;
import avilaos.model.enumeration.ProcessState;
import avilaos.model.enumeration.SchedulingPolicyType;
import avilaos.model.pcb.PCB;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class IOProcessIntegrationTest {

    private DiskBlockMap diskMap;
    private FileSystemManager fsManager;
    private DiskScheduler diskScheduler;
    private Computer computer;
    private Kernel kernel;

    @BeforeEach
    void setUp() {
        diskMap = new DiskBlockMap(100);
        fsManager = new FileSystemManager(diskMap);
        fsManager.createDirectory("/", "docs", "system", true);

        diskScheduler = new DiskScheduler(DiskPolicyType.FIFO);
        computer = new Computer(1, 1024, SchedulingPolicyType.FCFS, 2);
        kernel = computer.getKernel();
        kernel.attachIOSubsystem(diskScheduler, fsManager);
    }

    @AfterEach
    void tearDown() {
        if (computer != null) {
            computer.shutdown();
        }
    }

    @Test
    void createProcessEntersKernelModeBlocksAndCreatesFileWhenDiskCompletes() {
        PCB creator = PCB.forFileCreate(
                "CrearNotas", 1, 1, 64, 50, 2,
                "/docs/notas.txt", 3, "kelvin", true
        );

        kernel.submitProcess(creator);
        kernel.executeCycle(1);

        assertEquals(ProcessState.BLOCKED, creator.getState());
        assertEquals(BlockReason.IO_WAIT, creator.getBlockReason());
        assertEquals(ExecutionMode.KERNEL, creator.getMode());
        assertEquals(1, diskScheduler.getPendingCount());
        assertEquals(1, kernel.getIoProcessManager().getSystemModeInstructions());
        assertNull(fsManager.resolve("/docs/notas.txt"));

        DiskSelection selection = diskScheduler.dispatch(0, HeadDirection.UP, diskMap.getTotalBlocks());
        assertNotNull(selection);
        DiskRequest request = selection.request();
        kernel.getIoProcessManager().completeDiskRequest(request, kernel);

        FileSystemNode created = fsManager.resolve("/docs/notas.txt");
        assertNotNull(created);
        assertEquals(3, created.getSizeInBlocks());
        assertEquals("kelvin", created.getOwner());
        assertEquals(ProcessState.READY, creator.getState());
        assertEquals(ExecutionMode.USER, creator.getMode());
        assertNull(creator.getIoErrorMessage());

        kernel.executeCycle(2);
        assertEquals(ProcessState.TERMINATED, creator.getState());
    }

    @Test
    void fullCrudLifecycleThroughIOProcesses() {
        // 1. CREATE
        PCB createProc = PCB.forFileCreate("P_Create", 1, 1, 32, 100, 1,
                "/docs/reporte.pdf", 4, "admin", false);
        kernel.submitProcess(createProc);
        kernel.executeCycle(1);
        kernel.getIoProcessManager().completeDiskRequest(
                diskScheduler.dispatch(0, HeadDirection.UP, 100).request(), kernel
        );
        assertEquals(ProcessState.TERMINATED, createProc.getState());
        assertNotNull(fsManager.resolve("/docs/reporte.pdf"));

        // 2. READ
        PCB readProc = PCB.forFileRead("P_Read", 1, 1, 32, 100, 1, "/docs/reporte.pdf");
        kernel.submitProcess(readProc);
        kernel.executeCycle(2);
        DiskRequest readReq = diskScheduler.dispatch(0, HeadDirection.UP, 100).request();
        assertEquals(0, readReq.getTargetBlock());
        kernel.getIoProcessManager().completeDiskRequest(readReq, kernel);
        assertNull(readProc.getIoErrorMessage());
        assertEquals(ProcessState.TERMINATED, readProc.getState());

        // 3. UPDATE (renombrar)
        PCB updateProc = PCB.forFileUpdate("P_Update", 1, 1, 32, 100, 1,
                "/docs/reporte.pdf", "final.pdf");
        kernel.submitProcess(updateProc);
        kernel.executeCycle(3);
        kernel.getIoProcessManager().completeDiskRequest(
                diskScheduler.dispatch(0, HeadDirection.UP, 100).request(), kernel
        );
        assertNull(fsManager.resolve("/docs/reporte.pdf"));
        assertNotNull(fsManager.resolve("/docs/final.pdf"));

        // 4. DELETE (libera los 4 bloques)
        int freeBeforeDelete = diskMap.getFreeCount();
        PCB deleteProc = PCB.forFileDelete("P_Delete", 1, 1, 32, 100, 1, "/docs/final.pdf");
        kernel.submitProcess(deleteProc);
        kernel.executeCycle(4);
        kernel.getIoProcessManager().completeDiskRequest(
                diskScheduler.dispatch(0, HeadDirection.UP, 100).request(), kernel
        );
        assertNull(fsManager.resolve("/docs/final.pdf"));
        assertEquals(freeBeforeDelete + 4, diskMap.getFreeCount());
    }

    @Test
    void recordsErrorAndTerminatesCleanlyWhenDiskIsFull() {
        PCB tooLarge = PCB.forFileCreate("P_Full", 1, 1, 32, 100, 1,
                "/docs/gigante.iso", 150, "user", true);
        kernel.submitProcess(tooLarge);
        kernel.executeCycle(1);

        DiskRequest req = diskScheduler.dispatch(0, HeadDirection.UP, 100).request();
        kernel.getIoProcessManager().completeDiskRequest(req, kernel);

        assertNotNull(tooLarge.getIoErrorMessage());
        assertEquals(ProcessState.TERMINATED, tooLarge.getState());
        assertNull(fsManager.resolve("/docs/gigante.iso"));
    }
}