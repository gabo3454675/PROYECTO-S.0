package avilaos.util.datastructures;

/**
 * Lista de asociaciones (mapa propio) basada en búsqueda lineal.
 * Sustituye a java.util.HashMap para cumplir con la restricción de
 * no usar colecciones del framework de Java.
 */
public class AssociativeList<K, V> {
    private static final int INITIAL_CAPACITY = 8;

    private Object[] keys;
    private Object[] values;
    private int size;

    public AssociativeList() {
        this.keys = new Object[INITIAL_CAPACITY];
        this.values = new Object[INITIAL_CAPACITY];
    }

    @SuppressWarnings("unchecked")
    public V put(K key, V value) {
        requireKey(key);
        int index = indexOf(key);
        if (index >= 0) {
            V previous = (V) values[index];
            values[index] = value;
            return previous;
        }
        if (size == keys.length) {
            resize();
        }
        keys[size] = key;
        values[size] = value;
        size++;
        return null;
    }

    @SuppressWarnings("unchecked")
    public V get(K key) {
        if (key == null) {
            return null;
        }
        int index = indexOf(key);
        return index >= 0 ? (V) values[index] : null;
    }

    @SuppressWarnings("unchecked")
    public V get(K key, V fallback) {
        V value = get(key);
        return value != null ? value : fallback;
    }

    public V putIfAbsent(K key, V value) {
        V existing = get(key);
        if (existing != null) {
            return existing;
        }
        put(key, value);
        return null;
    }

    @SuppressWarnings("unchecked")
    public V remove(K key) {
        if (key == null) {
            return null;
        }
        int index = indexOf(key);
        if (index < 0) {
            return null;
        }
        V previous = (V) values[index];
        for (int i = index; i < size - 1; i++) {
            keys[i] = keys[i + 1];
            values[i] = values[i + 1];
        }
        keys[size - 1] = null;
        values[size - 1] = null;
        size--;
        return previous;
    }

    public boolean containsKey(K key) {
        return key != null && indexOf(key) >= 0;
    }

    public int size() {
        return size;
    }

    public boolean isEmpty() {
        return size == 0;
    }

    public void clear() {
        for (int i = 0; i < size; i++) {
            keys[i] = null;
            values[i] = null;
        }
        size = 0;
    }

    public List<K> keys() {
        List<K> result = new LinkedList<>();
        for (int i = 0; i < size; i++) {
            @SuppressWarnings("unchecked")
            K key = (K) keys[i];
            result.add(key);
        }
        return result;
    }

    public List<V> values() {
        List<V> result = new LinkedList<>();
        for (int i = 0; i < size; i++) {
            @SuppressWarnings("unchecked")
            V value = (V) values[i];
            result.add(value);
        }
        return result;
    }

    private int indexOf(K key) {
        for (int i = 0; i < size; i++) {
            if (keys[i].equals(key)) {
                return i;
            }
        }
        return -1;
    }

    private void resize() {
        int newCapacity = keys.length * 2;
        Object[] newKeys = new Object[newCapacity];
        Object[] newValues = new Object[newCapacity];
        for (int i = 0; i < size; i++) {
            newKeys[i] = keys[i];
            newValues[i] = values[i];
        }
        this.keys = newKeys;
        this.values = newValues;
    }

    private void requireKey(K key) {
        if (key == null) {
            throw new IllegalArgumentException("La clave no puede ser null");
        }
    }
}
