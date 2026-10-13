package avilaos.util.datastructures;

import java.util.Iterator;
import java.util.NoSuchElementException;

public class LinkedList<E> implements List<E> {
    private Node<E> head;
    private Node<E> tail;
    private int size;

    public LinkedList() {
        this.head = null;
        this.tail = null;
        this.size = 0;
    }

    public void add(E element) {
        Node<E> newNode = new Node<>(element);
        if (tail == null) {
            head = tail = newNode;
        } else {
            tail.next = newNode;
            tail = newNode;
        }
        size++;
    }

    public void addFirst(E element) {
        Node<E> newNode = new Node<>(element);
        newNode.next = head;
        head = newNode;
        if (tail == null) {
            tail = newNode;
        }
        size++;
    }

    public E get(int index) {
        checkIndex(index);
        Node<E> current = head;
        for (int i = 0; i < index; i++) {
            current = current.next;
        }
        return current.data;
    }

    public E remove(int index) {
        checkIndex(index);
        if (index == 0) {
            return removeFirst();
        }
        Node<E> prev = head;
        for (int i = 0; i < index - 1; i++) {
            prev = prev.next;
        }
        E data = prev.next.data;
        prev.next = prev.next.next;
        if (prev.next == null) {
            tail = prev;
        }
        size--;
        return data;
    }

    public E removeFirst() {
        if (head == null) {
            throw new NoSuchElementException("Lista vacía");
        }
        E data = head.data;
        head = head.next;
        if (head == null) {
            tail = null;
        }
        size--;
        return data;
    }

    public boolean remove(E element) {
        if (head == null) return false;
        if (head.data.equals(element)) {
            removeFirst();
            return true;
        }
        Node<E> prev = head;
        while (prev.next != null) {
            if (prev.next.data.equals(element)) {
                prev.next = prev.next.next;
                if (prev.next == null) {
                    tail = prev;
                }
                size--;
                return true;
            }
            prev = prev.next;
        }
        return false;
    }

    public int size() {
        return size;
    }

    public boolean isEmpty() {
        return size == 0;
    }

    public void clear() {
        head = tail = null;
        size = 0;
    }

    public E getFirst() {
        if (head == null) throw new NoSuchElementException("Lista vacía");
        return head.data;
    }

    public E getLast() {
        if (tail == null) throw new NoSuchElementException("Lista vacía");
        return tail.data;
    }

    public int indexOf(E element) {
        Node<E> current = head;
        int index = 0;
        while (current != null) {
            if (current.data.equals(element)) return index;
            current = current.next;
            index++;
        }
        return -1;
    }

    public boolean contains(E element) {
        return indexOf(element) != -1;
    }

    @Override
    @SuppressWarnings("unchecked")
    public E[] toArray(E[] array) {
        if (array.length < size) {
            array = (E[]) java.lang.reflect.Array.newInstance(array.getClass().getComponentType(), size);
        }
        int index = 0;
        for (E element : this) {
            array[index++] = element;
        }
        if (array.length > size) {
            array[size] = null;
        }
        return array;
    }

    private void checkIndex(int index) {
        if (index < 0 || index >= size) {
            throw new IndexOutOfBoundsException("Índice: " + index + ", Tamaño: " + size);
        }
    }

    @Override
    public Iterator<E> iterator() {
        return new Iterator<E>() {
            private Node<E> current = head;

            @Override
            public boolean hasNext() {
                return current != null;
            }

            @Override
            public E next() {
                if (!hasNext()) throw new NoSuchElementException();
                E data = current.data;
                current = current.next;
                return data;
            }
        };
    }
}