package avilaos.kernel.disk;

import avilaos.model.disk.DiskRequest;
import avilaos.model.disk.DiskSelection;
import avilaos.model.enumeration.DiskPolicyType;
import avilaos.model.enumeration.HeadDirection;
import avilaos.model.enumeration.IOOperationType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class DiskSchedulerTest {

    private static DiskRequest request(int pid, int block) {
        return new DiskRequest(pid, "P" + pid, IOOperationType.READ, "/f" + pid, block, 0);
    }

    @Test
    void factoryCreatesEveryPolicyType() {
        for (DiskPolicyType type : DiskPolicyType.values()) {
            assertEquals(type, DiskPolicyFactory.create(type).getType());
        }
    }

    @Test
    void dispatchOnEmptyQueueReturnsNull() {
        DiskScheduler scheduler = new DiskScheduler(DiskPolicyType.FIFO);
        assertNull(scheduler.dispatch(50, HeadDirection.UP, 200));
    }

    @Test
    void dispatchRemovesTheSelectedRequest() {
        DiskScheduler scheduler = new DiskScheduler(DiskPolicyType.SSTF);
        DiskRequest far = request(1, 180);
        DiskRequest near = request(2, 55);
        scheduler.submit(far);
        scheduler.submit(near);

        DiskSelection selection = scheduler.dispatch(50, HeadDirection.UP, 200);

        assertSame(near, selection.request());
        assertEquals(1, scheduler.getPendingCount());
        assertSame(far, scheduler.snapshotQueue()[0]);
    }

    @Test
    void policyCanChangeWhileRequestsArePending() {
        DiskScheduler scheduler = new DiskScheduler(DiskPolicyType.FIFO);
        scheduler.submit(request(1, 180));
        scheduler.submit(request(2, 55));

        scheduler.setPolicy(DiskPolicyType.SSTF);

        assertEquals(DiskPolicyType.SSTF, scheduler.getPolicyType());
        assertEquals(55, scheduler.dispatch(50, HeadDirection.UP, 200).request().getTargetBlock());
    }

    @Test
    void cancelRemovesRequestWithoutServingIt() {
        DiskScheduler scheduler = new DiskScheduler(DiskPolicyType.FIFO);
        DiskRequest r = request(1, 10);
        scheduler.submit(r);

        assertTrue(scheduler.cancel(r));
        assertFalse(scheduler.hasPending());
    }

    @Test
    void concurrentSubmitsAreNotLost() throws InterruptedException {
        DiskScheduler scheduler = new DiskScheduler(DiskPolicyType.FIFO);
        Thread[] threads = new Thread[4];
        for (int t = 0; t < threads.length; t++) {
            final int base = t * 100;
            threads[t] = new Thread(() -> {
                for (int i = 0; i < 100; i++) {
                    scheduler.submit(request(base + i, i));
                }
            });
            threads[t].start();
        }
        for (Thread thread : threads) {
            thread.join();
        }
        assertEquals(400, scheduler.getPendingCount());
    }
}
