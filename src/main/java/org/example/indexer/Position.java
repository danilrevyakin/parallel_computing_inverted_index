package org.example.indexer;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Position {
    private final Map<String, List<Integer>> map;

    public Position() {
        this.map = new HashMap<>();
    }

    public Map<String, List<Integer>> getMap() {
        return map;
    }

    @Override
    public String toString() {
        if (map.isEmpty()) {
            return "not found";
        } else {
            StringBuilder sb = new StringBuilder();
            sb.append("Found:\n");
            for (Map.Entry<String, List<Integer>> entry : map.entrySet()) {
                sb.append("\t* [").append(entry.getKey()).append("] positions: ");
                sb.append(entry.getValue()).append(";\n");
            }
            return sb.toString().trim();
        }
    }
}