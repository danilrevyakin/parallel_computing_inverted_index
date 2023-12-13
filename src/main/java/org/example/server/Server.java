package org.example.server;

import org.example.indexer.IndexerThread;
import org.example.indexer.InvertedIndex;
import org.example.utils.FileHandler;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Server {
    private final ExecutorService threadPool = Executors.newCachedThreadPool();
    private final InvertedIndex invertedIndex = new InvertedIndex();
    private static final Logger logger = Logger.getLogger(Server.class.getName());
    private final AtomicBoolean isIndexed = new AtomicBoolean(false);
    private final Object indexingLock = new Object();

    public void start() {
        try (ServerSocket serverSocket = new ServerSocket(10000)) {
           logger.log(Level.INFO, "Server started on port 10000");
            while (true) {
                Socket clientSocket = serverSocket.accept();
                logger.log(Level.INFO,"Client connected: " + clientSocket);
                threadPool.submit(() -> handleClient(clientSocket));
            }
        } catch (IOException e) {
            e.printStackTrace();
            threadPool.shutdownNow();
        }
    }

    private void handleClient(Socket clientSocket) {
        try (
                clientSocket;
                DataInputStream dis = new DataInputStream(clientSocket.getInputStream());
                DataOutputStream dos = new DataOutputStream(clientSocket.getOutputStream())
        ) {

            synchronized (indexingLock) {
                dos.writeBoolean(isIndexed.get());
                if (!isIndexed.get()) {
                    logger.log(Level.WARNING, "Files require indexing");
                    indexingLock.notifyAll();

                    int numberOfThreads = dis.readInt();
                    CompletableFuture<Long> future = CompletableFuture.supplyAsync(() ->
                            processIndexing(numberOfThreads)
                    );

                    try {
                        indexingLock.wait();
                    } catch (InterruptedException e) {
                        logger.log(Level.SEVERE, "Interrupted while waiting for indexing", e);
                    }

                    Long time = future.resultNow();
                    isIndexed.set(true);
                    logger.log(Level.INFO, "Indexing execution time: " + time);
                    dos.writeUTF("Indexing execution time: " + time);
                }
            }

            boolean findOneMoreWord;

            do {
                String word = dis.readUTF();
                logger.log(Level.INFO,"Client want to find such word: " + word);
                try{
                    HashMap<String, List<Integer>> indexedFiles = invertedIndex.getWord(word);
                    logger.log(Level.INFO, "Files were found for word '" + word + "'");
                    dos.writeUTF(indexedFiles.toString());
                } catch (Exception e){
                    logger.log(Level.WARNING,"No files found for this word");
                    dos.writeUTF("no files found");
                }

                findOneMoreWord = dis.readUTF().equalsIgnoreCase("y");
                logger.log(Level.INFO,"Client " + (findOneMoreWord ? "" : "don't ") + "want to find one more word");
            } while (findOneMoreWord);
        } catch (IOException e) {
            logger.log(Level.SEVERE, e.getMessage());
            e.printStackTrace();
        }
        logger.log(Level.INFO,"Client " + clientSocket + " disconnected");
    }

    public Long processIndexing(int threadsNumber) {
        logger.log(Level.INFO, "Start indexing files...");

        ExecutorService executor = Executors.newFixedThreadPool(threadsNumber);
        CompletionService<Void> completionService = new ExecutorCompletionService<>(executor);

        List<File> files = FileHandler.getAllFiles();
        int batchSize = files.size() / threadsNumber;

        long currentTime = System.currentTimeMillis();
        for (int i = 0; i < threadsNumber; i++) {
            List<File> sublist = files.subList(
                    batchSize * i,
                    i == threadsNumber - 1 ? files.size() : batchSize * (i + 1)
            );
            IndexerThread task = new IndexerThread(sublist, invertedIndex);
            completionService.submit(task);
        }

        for (int i = 0; i < threadsNumber; i++) {
            try {
                completionService.take();
            } catch (InterruptedException e) {
                logger.log(Level.SEVERE, e.getMessage());
                e.printStackTrace();
            }
        }
        executor.shutdown();
        Long result = System.currentTimeMillis() - currentTime;

        synchronized (indexingLock){
            indexingLock.notifyAll();
        }

        return result;
    }
}
