/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package avilaos.core.filesystem;

import avilaos.core.disk.DiskBlockMap;
import avilaos.model.enumeration.UserMode;
import avilaos.util.datastructures.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class FileSystemManagerTest {

    private DiskBlockMap blockMap;
    private FileSystemManager fs;

    @BeforeEach
    void setUp() {
        blockMap = new DiskBlockMap(100);
        fs = new FileSystemManager(blockMap);
    }

    @Test
    void createDirectoryAndFileAllocatesChainedBlocks() {
        FileSystemNode dir = fs.createDirectory("/", "sistema", "admin", true);
        assertEquals("/sistema", dir.getPath());
        assertTrue(dir.isDirectory());

        FileSystemNode file = fs.createFile("/sistema", "boot.bin", 12, "admin", true, "P1");
        assertEquals("/sistema/boot.bin", file.getPath());
        assertFalse(file.isDirectory());
        assertEquals(12, file.getSizeInBlocks());
        assertEquals(88, blockMap.getFreeCount());
        assertEquals(12, blockMap.getChain(file.getFirstBlock()).size());
    }

    @Test
    void failsWhenNotEnoughFreeBlocks() {
        fs.createDirectory("/", "usuarios", "admin", true);
        assertThrows(IllegalStateException.class, () ->
                fs.createFile("/usuarios", "gigante.iso", 150, "user1", false, "P2")
        );
        assertEquals(100, blockMap.getFreeCount());
    }

    @Test
    void renameUpdatesNodeNameAndPath() {
        fs.createDirectory("/", "usuarios", "admin", true);
        FileSystemNode file = fs.createFile("/usuarios", "notas.txt", 5, "user1", false, "P3");

        fs.rename("/usuarios/notas.txt", "apuntes.txt");
        assertNull(fs.resolve("/usuarios/notas.txt"));
        assertSame(file, fs.resolve("/usuarios/apuntes.txt"));
        assertEquals("/usuarios/apuntes.txt", file.getPath());
    }

    @Test
    void deleteFileAndRecursiveDirectoryReleasesAllBlocks() {
        fs.createDirectory("/", "usuarios", "admin", true);
        fs.createDirectory("/usuarios", "docs", "user1", false);
        fs.createFile("/usuarios", "a.txt", 10, "user1", false, "P1");
        fs.createFile("/usuarios/docs", "b.txt", 15, "user1", false, "P2");

        assertEquals(75, blockMap.getFreeCount());

        int released = fs.delete("/usuarios");
        assertEquals(25, released);
        assertEquals(100, blockMap.getFreeCount());
        assertNull(fs.resolve("/usuarios"));
    }

    @Test
    void enforcesUserAndAdminPermissions() {
        fs.createDirectory("/", "usuarios", "admin", true);
        FileSystemNode privateFile = fs.createFile("/usuarios", "secreto.txt", 4, "user1", false, "P1");
        FileSystemNode publicFile = fs.createFile("/usuarios", "manual.txt", 4, "admin", true, "P2");

        assertTrue(fs.canRead(privateFile, UserMode.ADMIN, "admin"));
        assertTrue(fs.canRead(privateFile, UserMode.USER, "user1"));
        assertFalse(fs.canRead(privateFile, UserMode.USER, "user2"));

        assertTrue(fs.canRead(publicFile, UserMode.USER, "user2"));
        assertFalse(fs.canModify(publicFile, UserMode.USER, "user2"));
        assertTrue(fs.canModify(privateFile, UserMode.USER, "user1"));
        assertTrue(fs.canModify(publicFile, UserMode.ADMIN, "admin"));

        List<FileSystemNode> allFiles = fs.getAllFiles();
        assertEquals(2, allFiles.size());
    }

    @Test
    void invalidFileNameDoesNotConsumeBlocks() {
        assertThrows(IllegalArgumentException.class,
                () -> fs.createFile("/", "bad/name", 3, "admin", true, "P1"));
        assertEquals(100, blockMap.getFreeCount());
        assertEquals(0, fs.getAllFiles().size());
    }

    @Test
    void renamingDirectoryUpdatesBlockOwnershipOfDescendants() {
        fs.createDirectory("/", "old", "admin", true);
        FileSystemNode file = fs.createFile("/old", "report.txt", 3, "admin", true, "P1");
        fs.rename("/old", "new");
        for (Integer index : blockMap.getChain(file.getFirstBlock())) {
            assertEquals("/new/report.txt", blockMap.getBlock(index).getOwnerId());
        }
    }
}
