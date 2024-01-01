package org.example.indexer.entities;

import java.util.AbstractMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.concurrent.locks.Lock;

public class CustomThreadSafeMap<K, V> {

    private static final int DEFAULT_CAPACITY = 16;
    private static final double LOAD_FACTOR_THRESHOLD = 0.75;

    private Node<K, V>[] table;
    private ReentrantReadWriteLock[] locks;

    public CustomThreadSafeMap() {
        this(DEFAULT_CAPACITY);
    }

    public CustomThreadSafeMap(int capacity) {
        this.table = new Node[capacity];
        this.locks = new ReentrantReadWriteLock[capacity];
        for (int i = 0; i < capacity; i++) {
            locks[i] = new ReentrantReadWriteLock();
        }
    }

    public void put(K key, V value) {
        int hash = hash(key);

        Lock writeLock = locks[hash % locks.length].writeLock();
        writeLock.lock();

        try {
            if (needsResize()) {
                resize();
                hash = hash(key);
                writeLock = locks[hash % locks.length].writeLock();
                writeLock.lock();
            }

            Node<K, V> newNode = new Node<>(key, value);
            if (table[hash % table.length] == null) {
                table[hash % table.length] = newNode;
            } else {
                Node<K, V> current = table[hash % table.length];
                while (current.next != null) {
                    if (current.key.equals(key)) {
                        current.value = value;
                        return;
                    }
                    current = current.next;
                }
                current.next = newNode;
            }
        } finally {
            writeLock.unlock();
        }
    }

    public V computeIfAbsent(K key, java.util.function.Function<? super K, ? extends V> mappingFunction) {
        int hash = hash(key);

        Lock writeLock = locks[hash % locks.length].writeLock();
        writeLock.lock();

        try {
            Node<K, V> current = table[hash % table.length];
            while (current != null) {
                if (current.key.equals(key)) {
                    return current.value;
                }
                current = current.next;
            }

            V computedValue = mappingFunction.apply(key);
            Node<K, V> newNode = new Node<>(key, computedValue);

            if (table[hash % table.length] == null) {
                table[hash % table.length] = newNode;
            } else {
                current = table[hash % table.length];
                while (current.next != null) {
                    current = current.next;
                }
                current.next = newNode;
            }

            return computedValue;
        } finally {
            writeLock.unlock();
        }
    }

    public V get(K key) {
        int hash = hash(key);

        Lock readLock = locks[hash % locks.length].readLock();
        readLock.lock();

        try {
            Node<K, V> current = table[hash % table.length];
            while (current != null) {
                if (current.key.equals(key)) {
                    return current.value;
                }
                current = current.next;
            }
            return null;
        } finally {
            readLock.unlock();
        }
    }

    public boolean isEmpty() {
        for (int i = 0; i < table.length; i++) {
            Lock readLock = locks[i].readLock();
            readLock.lock();
            try {
                if (table[i] != null) {
                    return false;
                }
            } finally {
                readLock.unlock();
            }
        }
        return true;
    }

    public void clear() {
        for (int i = 0; i < table.length; i++) {
            Lock writeLock = locks[i].writeLock();
            writeLock.lock();
            try {
                table[i] = null;
            } finally {
                writeLock.unlock();
            }
        }
    }

    public V getOrDefault(K key, V defaultValue) {
        int hash = hash(key);

        Lock readLock = locks[hash % locks.length].readLock();
        readLock.lock();

        try {
            Node<K, V> current = table[hash % table.length];
            while (current != null) {
                if (current.key.equals(key)) {
                    return current.value;
                }
                current = current.next;
            }
            return defaultValue;
        } finally {
            readLock.unlock();
        }
    }

    public Set<Map.Entry<K, V>> entrySet() {
        Set<Map.Entry<K, V>> entrySet = new HashSet<>();
        for (int i = 0; i < table.length; i++) {
            Lock readLock = locks[i].readLock();
            readLock.lock();
            try {
                Node<K, V> current = table[i];
                while (current != null) {
                    entrySet.add(new AbstractMap.SimpleEntry<>(current.key, current.value));
                    current = current.next;
                }
            } finally {
                readLock.unlock();
            }
        }
        return entrySet;
    }

    public boolean containsKey(K key) {
        int hash = hash(key);

        Lock readLock = locks[hash % locks.length].readLock();
        readLock.lock();

        try {
            Node<K, V> current = table[hash % table.length];
            while (current != null) {
                if (current.key.equals(key)) {
                    return true;
                }
                current = current.next;
            }
            return false;
        } finally {
            readLock.unlock();
        }
    }

    private int hash(K key) {
        return Math.abs(key.hashCode());
    }

    private boolean needsResize() {
        return (double) size() / table.length > LOAD_FACTOR_THRESHOLD;
    }

    private void resize() {
        int newCapacity = table.length * 2;
        Node<K, V>[] newTable = new Node[newCapacity];
        ReentrantReadWriteLock[] newLocks = new ReentrantReadWriteLock[newCapacity];

        for (int i = 0; i < newCapacity; i++) {
            newLocks[i] = new ReentrantReadWriteLock();
        }

        // Rehash and insert existing elements into the new array
        for (int i = 0; i < table.length; i++) {
            Lock readLock = locks[i].readLock();
            readLock.lock();
            try {
                Node<K, V> current = table[i];
                while (current != null) {
                    int hash = hash(current.key);
                    Lock writeLock = newLocks[hash % newCapacity].writeLock();
                    writeLock.lock();
                    try {
                        Node<K, V> newNode = new Node<>(current.key, current.value);
                        if (newTable[hash % newCapacity] == null) {
                            newTable[hash % newCapacity] = newNode;
                        } else {
                            Node<K, V> newCurrent = newTable[hash % newCapacity];
                            while (newCurrent.next != null) {
                                newCurrent = newCurrent.next;
                            }
                            newCurrent.next = newNode;
                        }
                    } finally {
                        writeLock.unlock();
                    }
                    current = current.next;
                }
            } finally {
                readLock.unlock();
            }
        }

        table = newTable;
        locks = newLocks;
    }

    private int size() {
        int count = 0;
        for (int i = 0; i < table.length; i++) {
            Lock readLock = locks[i].readLock();
            readLock.lock();
            try {
                Node<K, V> current = table[i];
                while (current != null) {
                    count++;
                    current = current.next;
                }
            } finally {
                readLock.unlock();
            }
        }
        return count;
    }

    private static class Node<K, V> {
        K key;
        V value;
        Node<K, V> next;

        public Node(K key, V value) {
            this.key = key;
            this.value = value;
        }
    }
}
