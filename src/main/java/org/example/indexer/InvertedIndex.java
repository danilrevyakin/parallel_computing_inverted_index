package org.example.indexer;

import org.example.indexer.entities.CustomThreadSafeMap;
import org.example.indexer.entities.Position;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class InvertedIndex {
    private final CustomThreadSafeMap<String, Position> index;

    public InvertedIndex() {
        this.index = new CustomThreadSafeMap<>();
    }

    public void addWord(String word, String filename, int position) {
        Position pos = index.computeIfAbsent(word, k -> new Position());
        List<Integer> positions = pos.getMap().computeIfAbsent(filename, k -> new ArrayList<>());
        positions.add(position);
    }

    public Position getPositions(String phrase) {
        List<String> words = Arrays.stream(phrase.split("\\W"))
                .filter(str -> !str.isEmpty())
                .map(String::toLowerCase)
                .toList();
        return searchPhraseRecursive(words, 0);
    }

    public void clear() {
        index.clear();
    }

    private Position searchPhraseRecursive(List<String> words, int index) {
        if (index == words.size() - 1) {
            return getPositionsForWord(words.get(index));
        } else {
            Position currentPositions = getPositionsForWord(words.get(index));
            Position remainingPositions = searchPhraseRecursive(words, index + 1);
            return mergePositions(currentPositions, remainingPositions);
        }
    }

    private Position getPositionsForWord(String word) {
        return index.getOrDefault(word, new Position());
    }

    private Position mergePositions(Position positions1, Position positions2) {
        Position result = new Position();
        for (Map.Entry<String, List<Integer>> entry : positions1.getMap().entrySet()) {
            String word = entry.getKey();
            if (positions2.getMap().containsKey(word)) {
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
