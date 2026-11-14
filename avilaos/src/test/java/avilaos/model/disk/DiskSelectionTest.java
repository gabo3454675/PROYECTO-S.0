package avilaos.model.disk;

import avilaos.model.enumeration.HeadDirection;
import avilaos.model.enumeration.IOOperationType;
import avilaos.model.enumeration.SeekMode;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class DiskSelectionTest {

    private static DiskRequest requestAt(int block) {
        return new DiskRequest(1, "P1", IOOperationType.READ, "/a", block, 0);
    }

    @Test
    void directSeekIsAbsoluteDifference() {
        DiskSelection s = new DiskSelection(requestAt(17), SeekMode.DIRECT, HeadDirection.DOWN);
        assertEquals(65, s.seekDistance(82, HeadDirection.UP, 200));
    }

    @Test
    void sweepAndReverseGoesToTheEdgeFirst() {
        DiskSelection s = new DiskSelection(requestAt(43), SeekMode.SWEEP_AND_REVERSE, HeadDirection.DOWN);
        // 190 -> 199 -> 43
        assertEquals(9 + 156, s.seekDistance(190, HeadDirection.UP, 200));
    }

    @Test
    void sweepAndWrapDoesNotCountTheJump() {
        DiskSelection s = new DiskSelection(requestAt(16), SeekMode.SWEEP_AND_WRAP, HeadDirection.UP);
        // 190 -> 199, salto a 0 (no cuenta), 0 -> 16
        assertEquals(9 + 16, s.seekDistance(190, HeadDirection.UP, 200));
    }

    @Test
    void rejectsNullFields() {
        assertThrows(IllegalArgumentException.class,
                () -> new DiskSelection(null, SeekMode.DIRECT, HeadDirection.UP));
    }
}
