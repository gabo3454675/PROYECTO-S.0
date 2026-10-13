package avilaos.util.datastructures;

import java.util.Iterator;

public interface List<E> extends Iterable<E> {
    void add(E element);
    void addFirst(E element);
    E get(int index);
    E remove(int index);
    E removeFirst();
    boolean remove(E element);
    int size();
    boolean isEmpty();
    void clear();
    E getFirst();
    E getLast();
    int indexOf(E element);
    boolean contains(E element);
    E[] toArray(E[] array);
}