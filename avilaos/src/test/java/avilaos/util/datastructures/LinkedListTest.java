package avilaos.util.datastructures;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class LinkedListTest {

    @Test
    void testAddAndGet() {
        LinkedList<String> list = new LinkedList<>();
        list.add("A");
        list.add("B");
        list.add("C");
        assertEquals(3, list.size());
        assertEquals("A", list.get(0));
        assertEquals("B", list.get(1));
        assertEquals("C", list.get(2));
    }

    @Test
    void testAddFirst() {
        LinkedList<String> list = new LinkedList<>();
        list.add("B");
        list.addFirst("A");
        assertEquals("A", list.get(0));
        assertEquals("B", list.get(1));
    }

    @Test
    void testRemove() {
        LinkedList<String> list = new LinkedList<>();
        list.add("A");
        list.add("B");
        list.add("C");
        assertEquals("B", list.remove(1));
        assertEquals(2, list.size());
        assertEquals("A", list.get(0));
        assertEquals("C", list.get(1));
    }

    @Test
    void testRemoveFirst() {
        LinkedList<String> list = new LinkedList<>();
        list.add("A");
        list.add("B");
        assertEquals("A", list.removeFirst());
        assertEquals(1, list.size());
        assertEquals("B", list.get(0));
    }

    @Test
    void testRemoveElement() {
        LinkedList<String> list = new LinkedList<>();
        list.add("A");
        list.add("B");
        list.add("C");
        assertTrue(list.remove("B"));
        assertEquals(2, list.size());
        assertFalse(list.remove("Z"));
    }

    @Test
    void testClear() {
        LinkedList<String> list = new LinkedList<>();
        list.add("A");
        list.add("B");
        list.clear();
        assertTrue(list.isEmpty());
        assertEquals(0, list.size());
    }

    @Test
    void testIterator() {
        LinkedList<Integer> list = new LinkedList<>();
        list.add(1);
        list.add(2);
        list.add(3);
        int sum = 0;
        for (int v : list) sum += v;
        assertEquals(6, sum);
    }

    @Test
    void testContainsAndIndexOf() {
        LinkedList<String> list = new LinkedList<>();
        list.add("A");
        list.add("B");
        assertTrue(list.contains("A"));
        assertFalse(list.contains("Z"));
        assertEquals(0, list.indexOf("A"));
        assertEquals(1, list.indexOf("B"));
        assertEquals(-1, list.indexOf("Z"));
    }

    @Test
    void testToArray() {
        LinkedList<String> list = new LinkedList<>();
        list.add("A");
        list.add("B");
        list.add("C");

        String[] array = list.toArray(new String[0]);
        assertEquals(3, array.length);
        assertEquals("A", array[0]);
        assertEquals("B", array[1]);
        assertEquals("C", array[2]);

        String[] padded = new String[5];
        String[] result = list.toArray(padded);
        assertSame(padded, result);
        assertNull(result[3]);

        String[] exact = new String[1];
        String[] grown = list.toArray(exact);
        assertEquals(3, grown.length);
        assertEquals("A", grown[0]);
    }
}