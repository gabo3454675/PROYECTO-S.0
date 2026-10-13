package avilaos.util.datastructures;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class AssociativeListTest {

    @Test
    public void putAndGet() {
        AssociativeList<String, Integer> map = new AssociativeList<>();
        assertTrue(map.isEmpty());
        assertNull(map.put("a", 1));
        assertNull(map.put("b", 2));
        assertEquals(2, map.size());
        assertEquals(1, map.get("a"));
        assertEquals(2, map.get("b"));
        assertNull(map.get("c"));
    }

    @Test
    public void putReplacesExistingValue() {
        AssociativeList<String, Integer> map = new AssociativeList<>();
        map.put("a", 1);
        assertEquals(1, map.put("a", 5));
        assertEquals(5, map.get("a"));
        assertEquals(1, map.size());
    }

    @Test
    public void removeShiftsAndKeepsOrder() {
        AssociativeList<Integer, String> map = new AssociativeList<>();
        map.put(1, "uno");
        map.put(2, "dos");
        map.put(3, "tres");
        assertEquals("dos", map.remove(2));
        assertEquals(2, map.size());
        assertFalse(map.containsKey(2));
        assertEquals("uno", map.get(1));
        assertEquals("tres", map.get(3));
        assertNull(map.remove(99));
    }

    @Test
    public void growsBeyondInitialCapacity() {
        AssociativeList<Integer, Integer> map = new AssociativeList<>();
        for (int i = 0; i < 100; i++) {
            map.put(i, i * 2);
        }
        assertEquals(100, map.size());
        for (int i = 0; i < 100; i++) {
            assertEquals(i * 2, map.get(i));
        }
    }

    @Test
    public void keysAndValuesAreReturnedAsOwnLists() {
        AssociativeList<String, Integer> map = new AssociativeList<>();
        map.put("a", 1);
        map.put("b", 2);
        avilaos.util.datastructures.List<String> keys = map.keys();
        avilaos.util.datastructures.List<Integer> values = map.values();
        assertEquals(2, keys.size());
        assertEquals(2, values.size());
        assertTrue(keys.contains("a"));
        assertTrue(values.contains(2));
    }

    @Test
    public void clearRemovesEverything() {
        AssociativeList<String, Integer> map = new AssociativeList<>();
        map.put("a", 1);
        map.clear();
        assertTrue(map.isEmpty());
        assertNull(map.get("a"));
    }

    @Test
    public void getWithFallbackAndPutIfAbsent() {
        AssociativeList<String, Integer> map = new AssociativeList<>();
        assertEquals(7, map.get("missing", 7));
        map.put("a", 1);
        assertEquals(1, map.putIfAbsent("a", 9));
        assertEquals(1, map.get("a"));
        assertNull(map.putIfAbsent("b", 5));
        assertEquals(5, map.get("b"));
    }
}
