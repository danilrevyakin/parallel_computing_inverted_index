package org.example.utils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

public class FileHandler {
    private static final List<File> files = new ArrayList<>();

    public static List<File> getAllFiles() {
        scanDirectory(new File("src/main/java/org/example/aclImdb"));
        return files;
    }

    private static void scanDirectory(File directory) {
        if (directory.isDirectory()) {
            File[] files = directory.listFiles();
            for (File file : Objects.requireNonNull(files)) {
                scanDirectory(file);
            }
        } else {
            files.add(directory);
        }
    }

    public static List<String> readFileContent(File file) throws IOException {
        try (Stream<String> lines = Files.lines(file.toPath())) {
            return lines
                    .flatMap(line -> Arrays.stream(line.split("\\W")))
                    .filter(str -> !str.isEmpty())
                    .map(String::toLowerCase)
                    .toList();
        }
    }
}