package avilaos.util.datastructures;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class LinkedQueueTest {

    @Test
    void testEnqueueDequeue() {
        Queue<String> queue = new LinkedQueue<>();
        queue.enqueue("A");
        queue.enqueue("B");
        queue.enqueue("C");
        assertEquals(3, queue.size());
        assertEquals("A", queue.dequeue());
        assertEquals("B", queue.dequeue());
        assertEquals("C", queue.dequeue());
        assertTrue(queue.isEmpty());
    }

    @Test
    void testPeek() {
        Queue<String> queue = new LinkedQueue<>();
        queue.enqueue("A");
        queue.enqueue("B");
        assertEquals("A", queue.peek());
        assertEquals(2, queue.size());
    }

    @Test
    void testClear() {
        Queue<String> queue = new LinkedQueue<>();
        queue.enqueue("A");
        queue.enqueue("B");
        queue.clear();
        assertTrue(queue.isEmpty());
        assertEquals(0, queue.size());
    }

    @Test
    void testRemove() {
        Queue<String> queue = new LinkedQueue<>();
        queue.enqueue("A");
        queue.enqueue("B");
        queue.enqueue("C");
        assertTrue(queue.remove("B"));
        assertEquals(2, queue.size());
        assertEquals("A", queue.dequeue());
        assertEquals("C", queue.dequeue());
        assertFalse(queue.remove("Z"));
    }
}