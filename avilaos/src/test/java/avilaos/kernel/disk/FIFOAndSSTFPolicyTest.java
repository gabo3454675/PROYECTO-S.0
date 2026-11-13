package avilaos.kernel.disk;

import avilaos.model.enumeration.HeadDirection;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** Valores tomados del anexo y de los casos P1/P2 del enunciado. */
class FIFOAndSSTFPolicyTest {

    private static final int[] ANNEX = {82, 17, 43, 140, 24, 16, 190};
    private static final int[] CASES = {95, 180, 34, 119, 11, 123, 62, 64};

    @Test
    void fifoMatchesAnnex() {
        DiskPolicyRun run = DiskPolicyRun.run(new FIFODiskPolicy(), 50, HeadDirection.UP, 200, ANNEX);
        assertArrayEquals(ANNEX, run.order);
        assertEquals(518, run.totalSeek);
    }

    @Test
    void fifoMatchesCaseP1() {
        DiskPolicyRun run = DiskPolicyRun.run(new FIFODiskPolicy(), 50, HeadDirection.UP, 200, CASES);
        assertArrayEquals(CASES, run.order);
    }

    @Test
    void sstfMatchesAnnex() {
        DiskPolicyRun run = DiskPolicyRun.run(new SSTFDiskPolicy(), 50, HeadDirection.UP, 200, ANNEX);
        assertArrayEquals(new int[]{43, 24, 17, 16, 82, 140, 190}, run.order);
        assertEquals(208, run.totalSeek);
    }

    @Test
    void sstfMatchesCaseP2() {
        DiskPolicyRun run = DiskPolicyRun.run(new SSTFDiskPolicy(), 50, HeadDirection.UP, 200, CASES);
        assertArrayEquals(new int[]{62, 64, 34, 11, 95, 119, 123, 180}, run.order);
    }

    @Test
    void sstfBreaksTiesByArrivalOrder() {
        DiskPolicyRun run = DiskPolicyRun.run(new SSTFDiskPolicy(), 50, HeadDirection.UP, 200, 60, 40);
        assertEquals(60, run.order[0]);
    }
}
