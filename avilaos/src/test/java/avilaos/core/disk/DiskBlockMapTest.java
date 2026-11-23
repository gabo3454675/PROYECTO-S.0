package avilaos.core.disk;

import avilaos.model.enumeration.BlockUsage;
import avilaos.util.datastructures.List;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class DiskBlockMapTest {

    @Test
    void rejectsDiskSizesOutsideConfiguredRange() {
        assertThrows(IllegalArgumentException.class, () -> new DiskBlockMap(99));
        assertThrows(IllegalArgumentException.class, () -> new DiskBlockMap(501));
        assertEquals(200, new DiskBlockMap(200).getTotalBlocks());
    }

    @Test
    void allocatesChainedBlocksFromPreferredStart() {
        DiskBlockMap disk = new DiskBlockMap(200);

        int first = disk.allocate(BlockUsage.FILE, "/mbr.bin", 11, 0);

        assertEquals(0, first);
        assertEquals(189, disk.getFreeCount());
        List<Integer> chain = disk.getChain(first);
        assertEquals(11, chain.size());
        assertEquals(10, (int) chain.getLast());
        assertEquals(1, disk.getBlock(0).getNext());
        assertEquals(DiskBlock.NO_NEXT, disk.getBlock(10).getNext());
        assertEquals("/mbr.bin", disk.getBlock(5).getOwnerId());
    }

    @Test
    void chainSkipsOccupiedBlocks() {
        DiskBlockMap disk = new DiskBlockMap(100);
        disk.allocate(BlockUsage.FILE, "/a", 1, 1);
        disk.allocate(BlockUsage.FILE, "/b", 1, 3);

        int first = disk.allocate(BlockUsage.FILE, "/c", 4, 0);

        List<Integer> chain = disk.getChain(first);
        assertEquals(0, (int) chain.get(0));
        assertEquals(2, (int) chain.get(1));
        assertEquals(4, (int) chain.get(2));
        assertEquals(5, (int) chain.get(3));
        assertEquals(2, disk.getBlock(0).getNext());
    }

    @Test
    void allocationWrapsAroundTheEndOfTheDisk() {
        DiskBlockMap disk = new DiskBlockMap(100);

        int first = disk.allocate(BlockUsage.SWAP, "P7", 3, 98);

        List<Integer> chain = disk.getChain(first);
        assertEquals(98, (int) chain.get(0));
        assertEquals(99, (int) chain.get(1));
        assertEquals(0, (int) chain.get(2));
        assertEquals(BlockUsage.SWAP, disk.getBlock(0).getUsage());
    }

    @Test
    void refusesAllocationWhenNotEnoughFreeBlocks() {
        DiskBlockMap disk = new DiskBlockMap(100);
        disk.allocate(BlockUsage.FILE, "/grande", 95);

        int result = disk.allocate(BlockUsage.FILE, "/no-cabe", 6);

        assertEquals(DiskBlock.NO_NEXT, result);
        assertEquals(5, disk.getFreeCount());
    }

    @Test
    void freeReleasesWholeChain() {
        DiskBlockMap disk = new DiskBlockMap(100);
        int a = disk.allocate(BlockUsage.FILE, "/a", 10);
        int b = disk.allocate(BlockUsage.FILE, "/b", 5);

        assertEquals(10, disk.free(a));

        assertEquals(95, disk.getFreeCount());
        assertTrue(disk.getBlock(0).isFree());
        assertEquals("/b", disk.getBlock(b).getOwnerId());
        int reused = disk.allocate(BlockUsage.FILE, "/c", 3);
        assertEquals(0, reused);
    }

    @Test
    void snapshotIsIndependentOfTheDisk() {
        DiskBlockMap disk = new DiskBlockMap(100);
        DiskBlock[] before = disk.snapshot();

        disk.allocate(BlockUsage.FILE, "/a", 1);

        assertTrue(before[0].isFree());
        assertFalse(disk.getBlock(0).isFree());
    }
}
