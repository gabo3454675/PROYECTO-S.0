package avilaos.kernel.disk;

import avilaos.core.disk.DiskBlockMap;
import avilaos.core.filesystem.FileSystemManager;
import avilaos.core.hardware.Computer;
import avilaos.model.enumeration.DiskPolicyType;
import avilaos.model.enumeration.HeadDirection;
import avilaos.model.enumeration.ProcessState;
import avilaos.model.enumeration.SchedulingPolicyType;
import avilaos.model.enumeration.UserMode;
import avilaos.model.pcb.PCB;
import avilaos.simulation.SimulationCluster;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class UserModePermissionTest {

    private SimulationCluster buildClusterWithFilesystem(FileSystemManager fs) {
        SimulationCluster cluster = new SimulationCluster();
        cluster.setCycleDurationMs(1);
        Computer computer = cluster.addComputer(0, 1024L, SchedulingPolicyType.FCFS, 2);
        computer.attachDisk(new DiskScheduler(DiskPolicyType.FIFO), fs, 0, HeadDirection.UP, 10);
        return cluster;
    }

    private void runUntilTerminated(SimulationCluster cluster, PCB pcb, int maxCycles) {
        for (int i = 0; i < maxCycles && pcb.getState() != ProcessState.TERMINATED; i++) {
            cluster.advanceCycle();
        }
        assertEquals(ProcessState.TERMINATED, pcb.getState(),
                "El proceso " + pcb.getName() + " debió terminar dentro de " + maxCycles + " ciclos.");
    }

    @Test
    void requestKeepsSubmittingUsersPermissionsWhileSessionChanges() {
        FileSystemManager fs = new FileSystemManager(new DiskBlockMap(100));
        fs.createDirectory("/", "docs", "admin", true);
        fs.createFile("/docs", "secret.txt", 1, "alice", false, "SYS");
        SimulationCluster cluster = buildClusterWithFilesystem(fs);
        try {
            cluster.setUserMode(UserMode.USER);
            cluster.setActiveUser("bob");
            PCB request = PCB.forFileRead("read-secret", 0, 1, 32, 100, 1, "/docs/secret.txt");
            cluster.submitProcess(request);
            cluster.advanceCycle();
            cluster.setUserMode(UserMode.ADMIN);
            cluster.setActiveUser("alice");
            runUntilTerminated(cluster, request, 20);
            assertTrue(request.getIoErrorMessage().contains("Permiso denegado"));
        } finally {
            cluster.shutdown();
        }
    }

    @Test
    void adminModeAllowsAllOperationsOnPrivateResources() {
        FileSystemManager fs = new FileSystemManager(new DiskBlockMap(100));
        fs.createDirectory("/", "privado_otro", "otro_usuario", false);
        fs.createFile("/privado_otro", "secreto.txt", 2, "otro_usuario", false, "SYS");

        SimulationCluster cluster = buildClusterWithFilesystem(fs);
        cluster.setUserMode(UserMode.ADMIN);
        cluster.setActiveUser("user");

        try {
            PCB createOp = PCB.forFileCreate("admin-create", 0, 1, 32, 100, 1,
                    "/privado_otro/nuevo.txt", 2, "admin", false);
            PCB readOp = PCB.forFileRead("admin-read", 0, 1, 32, 100, 1,
                    "/privado_otro/secreto.txt");
            PCB updateOp = PCB.forFileUpdate("admin-update", 0, 1, 32, 100, 1,
                    "/privado_otro/secreto.txt", "renombrado.txt");
            PCB deleteOp = PCB.forFileDelete("admin-delete", 0, 1, 32, 100, 1,
                    "/privado_otro/renombrado.txt");

            cluster.submitProcess(createOp);
            runUntilTerminated(cluster, createOp, 20);
            assertNull(createOp.getIoErrorMessage());
            assertNotNull(fs.resolve("/privado_otro/nuevo.txt"));

            cluster.submitProcess(readOp);
            runUntilTerminated(cluster, readOp, 20);
            assertNull(readOp.getIoErrorMessage());

            cluster.submitProcess(updateOp);
            runUntilTerminated(cluster, updateOp, 20);
            assertNull(updateOp.getIoErrorMessage());
            assertNotNull(fs.resolve("/privado_otro/renombrado.txt"));

            cluster.submitProcess(deleteOp);
            runUntilTerminated(cluster, deleteOp, 20);
            assertNull(deleteOp.getIoErrorMessage());
            assertNull(fs.resolve("/privado_otro/renombrado.txt"));
        } finally {
            cluster.shutdown();
        }
    }

    @Test
    void userModeReadsPublicAndOwnFilesButRejectsForeignPrivateFiles() {
        FileSystemManager fs = new FileSystemManager(new DiskBlockMap(100));
        fs.createDirectory("/", "docs", "admin", true);
        fs.createFile("/docs", "manual_publico.txt", 2, "admin", true, "SYS");
        fs.createFile("/docs", "clave_admin.txt", 2, "admin", false, "SYS");
        fs.createFile("/docs", "nota_user.txt", 2, "user", false, "SYS");

        SimulationCluster cluster = buildClusterWithFilesystem(fs);
        cluster.setUserMode(UserMode.USER);
        cluster.setActiveUser("user");

        try {
            PCB readPublic = PCB.forFileRead("read-public", 0, 1, 32, 100, 1, "/docs/manual_publico.txt");
            PCB readOwn = PCB.forFileRead("read-own", 0, 1, 32, 100, 1, "/docs/nota_user.txt");
            PCB readForeignPrivate = PCB.forFileRead("read-private", 0, 1, 32, 100, 1, "/docs/clave_admin.txt");

            cluster.submitProcess(readPublic);
            runUntilTerminated(cluster, readPublic, 20);
            assertNull(readPublic.getIoErrorMessage());

            cluster.submitProcess(readOwn);
            runUntilTerminated(cluster, readOwn, 20);
            assertNull(readOwn.getIoErrorMessage());

            cluster.submitProcess(readForeignPrivate);
            runUntilTerminated(cluster, readForeignPrivate, 20);
            assertNotNull(readForeignPrivate.getIoErrorMessage());
            assertTrue(readForeignPrivate.getIoErrorMessage().contains("Permiso denegado"));
        } finally {
            cluster.shutdown();
        }
    }

    @Test
    void userModeCreatesOnlyInOwnDirectoriesAndContinuesAfterRejection() {
        FileSystemManager fs = new FileSystemManager(new DiskBlockMap(100));
        fs.createDirectory("/", "sistema", "admin", true);
        fs.createDirectory("/", "home_user", "user", false);

        SimulationCluster cluster = buildClusterWithFilesystem(fs);
        cluster.setUserMode(UserMode.USER);
        cluster.setActiveUser("user");

        try {
            PCB deniedCreate = PCB.forFileCreate("create-in-admin", 0, 1, 32, 100, 1,
                    "/sistema/hack.bin", 2, "user", true);
            PCB allowedCreate = PCB.forFileCreate("create-in-home", 0, 1, 32, 100, 1,
                    "/home_user/proyecto.txt", 2, "user", false);

            cluster.submitProcess(deniedCreate);
            runUntilTerminated(cluster, deniedCreate, 20);
            assertNotNull(deniedCreate.getIoErrorMessage());
            assertTrue(deniedCreate.getIoErrorMessage().contains("Permiso denegado"));
            assertNull(fs.resolve("/sistema/hack.bin"));

            // La simulación continúa sin detenerse y ejecuta el siguiente proceso válido
            cluster.submitProcess(allowedCreate);
            runUntilTerminated(cluster, allowedCreate, 20);
            assertNull(allowedCreate.getIoErrorMessage());
            assertNotNull(fs.resolve("/home_user/proyecto.txt"));
        } finally {
            cluster.shutdown();
        }
    }

    @Test
    void userModeModifiesAndDeletesOnlyOwnFiles() {
        FileSystemManager fs = new FileSystemManager(new DiskBlockMap(100));
        fs.createDirectory("/", "shared", "admin", true);
        fs.createFile("/shared", "admin_file.txt", 2, "admin", true, "SYS");
        fs.createFile("/shared", "user_file.txt", 2, "user", true, "SYS");

        SimulationCluster cluster = buildClusterWithFilesystem(fs);
        cluster.setUserMode(UserMode.USER);
        cluster.setActiveUser("user");

        try {
            PCB deniedRename = PCB.forFileUpdate("rename-admin", 0, 1, 32, 100, 1,
                    "/shared/admin_file.txt", "robado.txt");
            PCB deniedDelete = PCB.forFileDelete("delete-admin", 0, 1, 32, 100, 1,
                    "/shared/admin_file.txt");
            PCB allowedRename = PCB.forFileUpdate("rename-user", 0, 1, 32, 100, 1,
                    "/shared/user_file.txt", "user_v2.txt");
            PCB allowedDelete = PCB.forFileDelete("delete-user", 0, 1, 32, 100, 1,
                    "/shared/user_v2.txt");

            cluster.submitProcess(deniedRename);
            runUntilTerminated(cluster, deniedRename, 20);
            assertNotNull(deniedRename.getIoErrorMessage());
            assertNotNull(fs.resolve("/shared/admin_file.txt"));

            cluster.submitProcess(deniedDelete);
            runUntilTerminated(cluster, deniedDelete, 20);
            assertNotNull(deniedDelete.getIoErrorMessage());
            assertNotNull(fs.resolve("/shared/admin_file.txt"));

            cluster.submitProcess(allowedRename);
            runUntilTerminated(cluster, allowedRename, 20);
            assertNull(allowedRename.getIoErrorMessage());
            assertNotNull(fs.resolve("/shared/user_v2.txt"));

            cluster.submitProcess(allowedDelete);
            runUntilTerminated(cluster, allowedDelete, 20);
            assertNull(allowedDelete.getIoErrorMessage());
            assertNull(fs.resolve("/shared/user_v2.txt"));
        } finally {
            cluster.shutdown();
        }
    }
}
