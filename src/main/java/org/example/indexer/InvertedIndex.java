package org.example.indexer;

import java.util.*;
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
            Position pos = index.computeIfAbsent(word, k -> new Position());
            List<Integer> positions = pos.getMap().computeIfAbsent(filename, k -> new ArrayList<>());
            positions.add(position);
        } finally {
            indexLock.writeLock().unlock();
        }
    }

    public Position getPositions(String phrase) {
        indexLock.readLock().lock();
        try {
            List<String> words = Arrays.stream(phrase.split("\\W"))
                    .filter(str -> !str.isEmpty())
                    .map(String::toLowerCase)
                    .toList();
            return searchPhraseRecursive(words, 0);
        } finally {
            indexLock.readLock().unlock();
        }
    }

    public void clear() {
        indexLock.writeLock().lock();
        try {
            index.clear();
        } finally {
            indexLock.writeLock().unlock();
        }
    }

    private Position searchPhraseRecursive(List<String> words, int index) {
        if (index == words.size() - 1) {
            return getPositionsForWord(words.get(index));
        } else {
            Position currentPositions = getPositionsForWord(words.get(index));
            Position remainingPositions;
            indexLock.readLock().lock();
            try {
                remainingPositions = searchPhraseRecursive(words, index + 1);
            } finally {
                indexLock.readLock().unlock();
            }
            return mergePositions(currentPositions, remainingPositions);
        }
    }

    private Position getPositionsForWord(String word) {
        indexLock.readLock().lock();
        try {
            return index.getOrDefault(word, new Position());
        } finally {
            indexLock.readLock().unlock();
        }
    }

    private Position mergePositions(Position positions1, Position positions2) {
        Position result = new Position();
        for (Map.Entry<String, List<Integer>> entry : positions1.getMap().entrySet()) {
            String word = entry.getKey();
            if (positions2.getMap().containsKey(word)){
                List<Integer> mergedList = new ArrayList<>();

                int i = 0, j = 0;

                while (i < entry.getValue().size() && j < positions2.getMap().get(word).size()) {
                    int pos1 = entry.getValue().get(i);
                    int pos2 = positions2.getMap().get(word).get(j);

                    if (pos1 == pos2 - 1) {
                        mergedList.add(pos1);
                        i++;
                        j++;
                    } else if (pos1 > pos2 - 1) {
                        j++;
                    } else {
                        i++;
                    }
                }

                result.getMap().put(word, mergedList);
            }
        }
        return result;
    }
}
