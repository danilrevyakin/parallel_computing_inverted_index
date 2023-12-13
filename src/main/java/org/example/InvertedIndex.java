package org.example;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class InvertedIndex {
    private final HashMap<String, HashMap<String, List<Integer>>> index;
    private final ReadWriteLock indexLock;

    public InvertedIndex() {
        this.index = new HashMap<>();
        this.indexLock = new ReentrantReadWriteLock();
    }

    public void add(String word, String filename, int position) {
        indexLock.writeLock().lock();
        try {
            HashMap<String, List<Integer>> fileMap = index.getOrDefault(word, new HashMap<>());
            List<Integer> positions = fileMap.getOrDefault(filename, new ArrayList<>());
            positions.add(position);
            fileMap.put(filename, positions);
            index.put(word, fileMap);
        } finally {
            indexLock.writeLock().unlock();
        }
    }

    public HashMap<String, List<Integer>> getWord(String word) throws WordNotFoundException {
        indexLock.readLock().lock();
        try {
            HashMap<String, List<Integer>> fileMap = index.get(word);
            if (fileMap == null) {
                throw new WordNotFoundException("Word " + word + " not found in the index.");
            } else {
                return new HashMap<>(fileMap);
            }
        } finally {
            indexLock.readLock().unlock();
        }
    }

    public static class WordNotFoundException extends Exception {
        public WordNotFoundException(String message) {
            super(message);
        }
    }
}
