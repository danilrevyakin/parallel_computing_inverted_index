package org.example.indexer.entities;

import org.example.indexer.entities.CustomThreadSafeMap;

import java.util.List;
import java.util.Map;

public class Position {
    private final CustomThreadSafeMap<String, List<Integer>> map;

    public Position() {
        this.map = new CustomThreadSafeMap<>();
    }

    public CustomThreadSafeMap<String, List<Integer>> getMap() {
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
                if (!entry.getValue().isEmpty()){
                    sb.append("\t* {").append(entry.getKey()).append("} positions: ");
                    sb.append(entry.getValue()).append(";\n");
                }
            }
            return sb.toString().trim();
        }
    }
}