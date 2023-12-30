package org.example.indexer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class InvertedIndex {
    private final HashMap<String, Position> index;
    private final ReadWriteLock indexLock;

    public InvertedIndex() {
        this.index = new HashMap<>();
        this.indexLock = new ReentrantReadWriteLock();
    }

    public void addWord(String word, String filename, int position) {
        indexLock.writeLock().lock();
        try {
            Position pos = index.getOrDefault(word, new Position());
            List<Integer> positions = new ArrayList<>(
                    pos.getMap().getOrDefault(filename, new ArrayList<>())
            );
            positions.add(position);
            pos.getMap().put(filename, positions);
            index.put(word, pos);
        } finally {
            indexLock.writeLock().unlock();
        }
    }

    public Position getPositions(String word){
        indexLock.readLock().lock();
        try {
            return index.get(word);
        } finally {
            indexLock.readLock().unlock();
        }
    }
}
