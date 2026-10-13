package avilaos.util.datastructures;

import java.util.Iterator;

public interface Queue<E> extends Iterable<E> {
    void enqueue(E element);
    E dequeue();
    E peek();
    boolean isEmpty();
    int size();
    void clear();
    boolean remove(E element);
}