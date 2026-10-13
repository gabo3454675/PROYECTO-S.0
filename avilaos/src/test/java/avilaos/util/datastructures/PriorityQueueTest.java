package avilaos.util.datastructures;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class PriorityQueueTest {

    @Test
    void testMinHeap() {
        PriorityQueue<Integer> pq = new PriorityQueue<>(Integer::compareTo);
        pq.enqueue(5);
        pq.enqueue(2);
        pq.enqueue(8);
        pq.enqueue(1);
        pq.enqueue(3);

        assertEquals(1, pq.dequeue());
        assertEquals(2, pq.dequeue());
        assertEquals(3, pq.dequeue());
        assertEquals(5, pq.dequeue());
        assertEquals(8, pq.dequeue());
    }

    @Test
    void testCustomComparator() {
        PriorityQueue<String> pq = new PriorityQueue<>((a, b) -> b.length() - a.length());
        pq.enqueue("A");
        pq.enqueue("BBB");
        pq.enqueue("CC");

        assertEquals("BBB", pq.dequeue());
        assertEquals("CC", pq.dequeue());
        assertEquals("A", pq.dequeue());
    }

    @Test
    void testRemove() {
        PriorityQueue<Integer> pq = new PriorityQueue<>(Integer::compareTo);
        pq.enqueue(5);
        pq.enqueue(2);
        pq.enqueue(8);
        pq.enqueue(1);

        assertTrue(pq.remove(2));
        assertEquals(1, pq.dequeue());
        assertEquals(5, pq.dequeue());
        assertEquals(8, pq.dequeue());
        assertFalse(pq.remove(99));
    }

    @Test
    void testClear() {
        PriorityQueue<Integer> pq = new PriorityQueue<>(Integer::compareTo);
        pq.enqueue(1);
        pq.enqueue(2);
        pq.clear();
        assertTrue(pq.isEmpty());
        assertEquals(0, pq.size());
    }

    @Test
    void testPeek() {
        PriorityQueue<Integer> pq = new PriorityQueue<>(Integer::compareTo);
        pq.enqueue(5);
        pq.enqueue(2);
        assertEquals(2, pq.peek());
        assertEquals(2, pq.size());
    }
}