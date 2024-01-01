package org.example.indexer;

import org.example.utils.DirectoryHandler;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;
import java.util.concurrent.*;
import java.util.stream.Stream;

public class Indexer implements Callable<Double> {
    private final List<File> files;
    private final InvertedIndex invertedIndex;
    private final Integer threadsAmount;
    private final int batchSize;


    public Indexer(InvertedIndex invertedIndex, Integer threadsAmount) {
        this.invertedIndex = invertedIndex;
        this.threadsAmount = threadsAmount;
        this.files = DirectoryHandler.getAllFiles();
        this.batchSize = files.size() / threadsAmount;
    }

    @Override
    public Double call() throws InterruptedException {
        ExecutorService executor = Executors.newFixedThreadPool(threadsAmount);
        CompletionService<Void> completionService = new ExecutorCompletionService<>(executor);

        long startTime = System.nanoTime();
        for (int i = 0; i < threadsAmount; i++) {
            List<File> sublist = files.subList(
                    batchSize * i,
                    i == threadsAmount - 1 ? files.size() : batchSize * (i + 1)
            );
            completionService.submit(() -> populate(sublist), null);
        }

        for (int i = 0; i < threadsAmount; i++) {
            completionService.take();
        }
        long endTime = System.nanoTime();

        executor.shutdown();
        return (endTime - startTime) / 1e6;
    }

    private void populate(List<File> files) {
        for (File file : files) {
            final int[] position = {0};
            try (Stream<String> lines = Files.lines(file.toPath())){
                lines.forEach(line -> {
                    String[] lineWords = line.split("\\W");
                    for (String word : lineWords) {
                        if (!word.isEmpty()) {
                            invertedIndex.addWord(word.toLowerCase(), file.getName(), position[0]);
                            position[0]++;
                        }
                    }
                });
            } catch (IOException e) {
                System.out.println(e.getMessage());
            }
        }
    }
}
