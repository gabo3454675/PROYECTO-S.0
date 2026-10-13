package avilaos.util.datastructures;

import java.util.Comparator;
import java.util.Iterator;
import java.util.NoSuchElementException;

public class PriorityQueue<E> implements Queue<E> {
    private Object[] heap;
    private int size;
    private final Comparator<? super E> comparator;
    private static final int DEFAULT_CAPACITY = 16;

    @SuppressWarnings("unchecked")
    public PriorityQueue(Comparator<? super E> comparator) {
        this(comparator, DEFAULT_CAPACITY);
    }

    @SuppressWarnings("unchecked")
    public PriorityQueue(Comparator<? super E> comparator, int initialCapacity) {
        this.comparator = comparator;
        this.heap = new Object[initialCapacity];
        this.size = 0;
    }

    @Override
    public void enqueue(E element) {
        if (element == null) throw new NullPointerException();
        ensureCapacity(size + 1);
        heap[size] = element;
        siftUp(size);
        size++;
    }

    @Override
    @SuppressWarnings("unchecked")
    public E dequeue() {
        if (size == 0) throw new NoSuchElementException("Cola vacía");
        E result = (E) heap[0];
        heap[0] = heap[--size];
        heap[size] = null;
        if (size > 0) {
            siftDown(0);
        }
        return result;
    }

    @Override
    @SuppressWarnings("unchecked")
    public E peek() {
        if (size == 0) throw new NoSuchElementException("Cola vacía");
        return (E) heap[0];
    }

    @Override
    public boolean isEmpty() {
        return size == 0;
    }

    @Override
    public int size() {
        return size;
    }

    @Override
    public void clear() {
        for (int i = 0; i < size; i++) {
            heap[i] = null;
        }
        size = 0;
    }

    public boolean remove(E element) {
        for (int i = 0; i < size; i++) {
            if (heap[i].equals(element)) {
                removeAt(i);
                return true;
            }
        }
        return false;
    }

    @SuppressWarnings("unchecked")
    private void removeAt(int index) {
        heap[index] = heap[--size];
        heap[size] = null;
        if (index < size) {
            siftDown(index);
            siftUp(index);
        }
    }

    private void ensureCapacity(int minCapacity) {
        if (minCapacity > heap.length) {
            int newCapacity = Math.max(heap.length * 2, minCapacity);
            Object[] newHeap = new Object[newCapacity];
            System.arraycopy(heap, 0, newHeap, 0, size);
            heap = newHeap;
        }
    }

    private void siftUp(int index) {
        while (index > 0) {
            int parent = (index - 1) >>> 1;
            if (compare(index, parent) >= 0) break;
            swap(index, parent);
            index = parent;
        }
    }

    private void siftDown(int index) {
        int half = size >>> 1;
        while (index < half) {
            int child = (index << 1) + 1;
            int right = child + 1;
            if (right < size && compare(right, child) < 0) {
                child = right;
            }
            if (compare(index, child) <= 0) break;
            swap(index, child);
            index = child;
        }
    }

    private int compare(int i, int j) {
        @SuppressWarnings("unchecked")
        E ei = (E) heap[i];
        @SuppressWarnings("unchecked")
        E ej = (E) heap[j];
        return comparator.compare(ei, ej);
    }

    private void swap(int i, int j) {
        Object tmp = heap[i];
        heap[i] = heap[j];
        heap[j] = tmp;
    }

    @Override
    public Iterator<E> iterator() {
        return new Iterator<E>() {
            private int index = 0;

            @Override
            public boolean hasNext() {
                return index < size;
            }

            @Override
            @SuppressWarnings("unchecked")
            public E next() {
                if (!hasNext()) throw new NoSuchElementException();
                return (E) heap[index++];
            }
        };
    }
}