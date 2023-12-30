package org.example.indexer;

import org.example.utils.FileHandler;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.Callable;

public class IndexerThread implements Callable<Void> {
    private final List<File> files;
    private final InvertedIndex invertedIndex;


    public IndexerThread(List<File> files, InvertedIndex invertedIndex) {
        this.files = files;
        this.invertedIndex = invertedIndex;
    }

    @Override
    public Void call() throws IOException {
        for (File file : files) {
            List<String> list = FileHandler.readFileContent(file);

            for (int i = 0; i < list.size(); i++) {
                String word = list.get(i);
                invertedIndex.addWord(word, file.getName(), i);
            }
        }
        return null;
    }
}
