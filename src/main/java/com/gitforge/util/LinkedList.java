package com.gitforge.util;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * Generic doubly-linked list used by GitForge for in-memory commit history.
 *
 * @param <T> element type stored in each node
 */
public class LinkedList<T> {

    /**
     * A single node in the linked list.
     *
     * @param <T> element type
     */
    public static final class Node<T> {

        private T data;
        private Node<T> next;
        private Node<T> previous;

        Node(T data) {
            this.data = data;
        }

        public T getData() {
            return data;
        }

        public Node<T> getNext() {
            return next;
        }

        public Node<T> getPrevious() {
            return previous;
        }
    }

    private Node<T> head;
    private Node<T> tail;
    private int size;

    public int size() {
        return size;
    }

    public boolean isEmpty() {
        return size == 0;
    }

    public Node<T> getHead() {
        return head;
    }

    public void clear() {
        head = null;
        tail = null;
        size = 0;
    }

    /**
     * Inserts an element at the head of the list (most recent position).
     */
    public Node<T> insertFirst(T data) {
        Objects.requireNonNull(data, "data");
        Node<T> node = new Node<>(data);
        if (head == null) {
            head = node;
            tail = node;
        } else {
            node.next = head;
            head.previous = node;
            head = node;
        }
        size++;
        return node;
    }

    /**
     * Inserts an element at the tail of the list.
     */
    public Node<T> insertLast(T data) {
        Objects.requireNonNull(data, "data");
        Node<T> node = new Node<>(data);
        if (tail == null) {
            head = node;
            tail = node;
        } else {
            tail.next = node;
            node.previous = tail;
            tail = node;
        }
        size++;
        return node;
    }

    /**
     * Inserts an element — defaults to head insertion for newest-first histories.
     */
    public Node<T> insert(T data) {
        return insertFirst(data);
    }

    /**
     * Removes the first node whose data matches the predicate.
     */
    public boolean deleteIf(Predicate<T> predicate) {
        Objects.requireNonNull(predicate, "predicate");
        Node<T> current = head;
        while (current != null) {
            if (predicate.test(current.data)) {
                removeNode(current);
                return true;
            }
            current = current.next;
        }
        return false;
    }

    /**
     * Finds the first element matching the predicate.
     */
    public Optional<T> find(Predicate<T> predicate) {
        Objects.requireNonNull(predicate, "predicate");
        Node<T> current = head;
        while (current != null) {
            if (predicate.test(current.data)) {
                return Optional.of(current.data);
            }
            current = current.next;
        }
        return Optional.empty();
    }

    /**
     * Finds the node containing the first matching element.
     */
    public Optional<Node<T>> findNode(Predicate<T> predicate) {
        Objects.requireNonNull(predicate, "predicate");
        Node<T> current = head;
        while (current != null) {
            if (predicate.test(current.data)) {
                return Optional.of(current);
            }
            current = current.next;
        }
        return Optional.empty();
    }

    /**
     * Traverses the list from head to tail.
     */
    public void traverse(Consumer<T> consumer) {
        Objects.requireNonNull(consumer, "consumer");
        Node<T> current = head;
        while (current != null) {
            consumer.accept(current.data);
            current = current.next;
        }
    }

    /**
     * Returns list elements in head-to-tail order.
     */
    public List<T> toList() {
        List<T> values = new ArrayList<>(size);
        traverse(values::add);
        return values;
    }

    private void removeNode(Node<T> node) {
        if (node.previous != null) {
            node.previous.next = node.next;
        } else {
            head = node.next;
        }
        if (node.next != null) {
            node.next.previous = node.previous;
        } else {
            tail = node.previous;
        }
        node.next = null;
        node.previous = null;
        size--;
    }
}
