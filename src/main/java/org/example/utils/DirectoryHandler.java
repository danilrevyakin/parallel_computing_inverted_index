package org.example.utils;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class DirectoryHandler {
    private static final List<File> files = new ArrayList<>();

    public static List<File> getAllFiles() {
        ClassLoader classLoader = DirectoryHandler.class.getClassLoader();
        URL url = classLoader.getResource("org" + File.separator + "example" + File.separator + "aclImdb");

        if (url != null) {
            File directory = new File(url.getFile());
            scanDirectory(directory);
        }
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
}