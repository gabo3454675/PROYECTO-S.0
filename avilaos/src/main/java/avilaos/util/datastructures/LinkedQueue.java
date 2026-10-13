package avilaos.util.datastructures;

import java.util.Iterator;

public class LinkedQueue<E> implements Queue<E> {
    private final LinkedList<E> list;

    public LinkedQueue() {
        this.list = new LinkedList<>();
    }

    @Override
    public void enqueue(E element) {
        list.add(element);
    }

    @Override
    public E dequeue() {
        return list.removeFirst();
    }

    @Override
    public E peek() {
        return list.getFirst();
    }

    @Override
    public boolean isEmpty() {
        return list.isEmpty();
    }

    @Override
    public int size() {
        return list.size();
    }

    @Override
    public void clear() {
        list.clear();
    }

    @Override
    public boolean remove(E element) {
        return list.remove(element);
    }

    @Override
    public Iterator<E> iterator() {
        return list.iterator();
    }
}