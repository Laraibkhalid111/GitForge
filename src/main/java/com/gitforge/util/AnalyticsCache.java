package com.gitforge.util;

import java.util.Objects;
import java.util.Optional;

/**
 * Custom hash-map cache used by GitForge analytics.
 * Stores computed dashboard snapshots keyed by filter fingerprints.
 *
 * @param <K> cache key type
 * @param <V> cached value type
 */
public class AnalyticsCache<K, V> {

    private static final int DEFAULT_CAPACITY = 16;

    private static final class Entry<K, V> {
        private final K key;
        private V value;
        private Entry<K, V> next;

        private Entry(K key, V value) {
            this.key = key;
            this.value = value;
        }
    }

    private Entry<K, V>[] buckets;
    private int size;
    private int capacity;

    @SuppressWarnings("unchecked")
    public AnalyticsCache() {
        this.capacity = DEFAULT_CAPACITY;
        this.buckets = (Entry<K, V>[]) new Entry[capacity];
    }

    public int size() {
        return size;
    }

    public boolean isEmpty() {
        return size == 0;
    }

    public void put(K key, V value) {
        Objects.requireNonNull(key, "key");
        Objects.requireNonNull(value, "value");
        if (putInternal(key, value) && size > capacity * 0.75) {
            resize();
        }
    }

    public Optional<V> get(K key) {
        if (key == null) {
            return Optional.empty();
        }
        Entry<K, V> current = buckets[indexFor(key)];
        while (current != null) {
            if (Objects.equals(current.key, key)) {
                return Optional.of(current.value);
            }
            current = current.next;
        }
        return Optional.empty();
    }

    public boolean contains(K key) {
        return get(key).isPresent();
    }

    public boolean remove(K key) {
        if (key == null) {
            return false;
        }
        int index = indexFor(key);
        Entry<K, V> current = buckets[index];
        Entry<K, V> previous = null;
        while (current != null) {
            if (Objects.equals(current.key, key)) {
                if (previous == null) {
                    buckets[index] = current.next;
                } else {
                    previous.next = current.next;
                }
                size--;
                return true;
            }
            previous = current;
            current = current.next;
        }
        return false;
    }

    @SuppressWarnings("unchecked")
    public void clear() {
        buckets = (Entry<K, V>[]) new Entry[capacity];
        size = 0;
    }

    private boolean putInternal(K key, V value) {
        int index = indexFor(key);
        Entry<K, V> current = buckets[index];
        while (current != null) {
            if (Objects.equals(current.key, key)) {
                current.value = value;
                return false;
            }
            current = current.next;
        }
        Entry<K, V> created = new Entry<>(key, value);
        created.next = buckets[index];
        buckets[index] = created;
        size++;
        return true;
    }

    private int indexFor(K key) {
        return Math.floorMod(key.hashCode(), capacity);
    }

    @SuppressWarnings("unchecked")
    private void resize() {
        Entry<K, V>[] old = buckets;
        capacity = capacity * 2;
        buckets = (Entry<K, V>[]) new Entry[capacity];
        size = 0;
        for (Entry<K, V> head : old) {
            Entry<K, V> current = head;
            while (current != null) {
                putInternal(current.key, current.value);
                current = current.next;
            }
        }
    }
}
