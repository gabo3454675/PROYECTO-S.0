package avilaos.kernel.disk;

import avilaos.model.enumeration.HeadDirection;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** Valores tomados del anexo y de los casos P3/P4 del enunciado. */
class ScanPoliciesTest {

    private static final int[] ANNEX = {82, 17, 43, 140, 24, 16, 190};
    private static final int[] CASES = {95, 180, 34, 119, 11, 123, 62, 64};

    @Test
    void scanMatchesAnnex() {
        DiskPolicyRun run = DiskPolicyRun.run(new SCANDiskPolicy(), 50, HeadDirection.UP, 200, ANNEX);
        assertArrayEquals(new int[]{82, 140, 190, 43, 24, 17, 16}, run.order);
        assertEquals(332, run.totalSeek);
    }

    @Test
    void scanMatchesCaseP3() {
        DiskPolicyRun run = DiskPolicyRun.run(new SCANDiskPolicy(), 50, HeadDirection.UP, 200, CASES);
        assertArrayEquals(new int[]{62, 64, 95, 119, 123, 180, 34, 11}, run.order);
    }

    @Test
    void scanMovingDownServesLowerBlocksFirst() {
        DiskPolicyRun run = DiskPolicyRun.run(new SCANDiskPolicy(), 50, HeadDirection.DOWN, 200, 60, 40, 10);
        assertArrayEquals(new int[]{40, 10, 60}, run.order);
        // 50 -> 40 -> 10 -> 0 -> 60
        assertEquals(10 + 30 + 10 + 60, run.totalSeek);
    }

    @Test
    void cscanMatchesAnnex() {
        DiskPolicyRun run = DiskPolicyRun.run(new CSCANDiskPolicy(), 50, HeadDirection.UP, 200, ANNEX);
        assertArrayEquals(new int[]{82, 140, 190, 16, 17, 24, 43}, run.order);
        assertEquals(192, run.totalSeek);
    }

    @Test
    void cscanMatchesCaseP4() {
        DiskPolicyRun run = DiskPolicyRun.run(new CSCANDiskPolicy(), 50, HeadDirection.UP, 200, CASES);
        assertArrayEquals(new int[]{62, 64, 95, 119, 123, 180, 11, 34}, run.order);
    }

    @Test
    void requestAtHeadPositionIsServedImmediately() {
        DiskPolicyRun run = DiskPolicyRun.run(new SCANDiskPolicy(), 50, HeadDirection.UP, 200, 30, 50);
        assertEquals(50, run.order[0]);
    }
}
