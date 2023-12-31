package org.example.server;

import org.example.indexer.IndexerThread;
import org.example.indexer.InvertedIndex;
import org.example.indexer.Position;
import org.example.utils.FileHandler;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Server {
    private static final int SERVER_PORT = 10000;
    private final ExecutorService threadPool = Executors.newCachedThreadPool();
    private final InvertedIndex invertedIndex = new InvertedIndex();
    private static final Logger logger = Logger.getLogger(Server.class.getName());
    private final AtomicBoolean isIndexed = new AtomicBoolean(false);
    private final AtomicBoolean isIndexingInProcess = new AtomicBoolean(false);

    public void start() {
        try (ServerSocket serverSocket = new ServerSocket(SERVER_PORT)) {
            logger.log(Level.INFO, "Server started on port " + SERVER_PORT);
            while (true) {
                Socket clientSocket = serverSocket.accept();
                logger.log(Level.INFO, "Client connected: " + clientSocket);
                threadPool.submit(() -> handleClient(clientSocket));
            }
        } catch (IOException e) {
            logger.log(Level.SEVERE, e.getMessage());
            threadPool.shutdownNow();
        }
    }

    private void handleClient(Socket clientSocket) {
        try (
                clientSocket;
                DataInputStream dis = new DataInputStream(clientSocket.getInputStream());
                DataOutputStream dos = new DataOutputStream(clientSocket.getOutputStream())
        ) {

            String command;
            String word;
            boolean disconnect = false;

            dos.writeUTF(Messaging.OPTIONS.get());
            while (!disconnect) {
                command = dis.readUTF();
                switch (command) {
                    case "1" -> {
                        if (isIndexed.get()) {
                            dos.writeUTF(Messaging.ENTER_WORD.get());
                            word = dis.readUTF();
                            if (!word.matches("^[a-zA-Z ]+$")) {
                                dos.writeUTF(Messaging.WRONG_INPUT.get() + Messaging.WRONG_STRING.get());
                                break;
                            }
                            logger.log(Level.INFO, "Client " + clientSocket + " entered phrase: " + word);
                            Position pos = invertedIndex.getPositions(word);
                            dos.writeUTF(pos.toString());
                        } else {
                            boolean updated = isIndexingInProcess.compareAndSet(false, true);
                            if (updated) {
                                dos.writeUTF(Messaging.REQUIRE_INDEXING.get());

                                int numberOfThreads;
                                try {
                                    numberOfThreads = Integer.parseInt(dis.readUTF());
                                    if (numberOfThreads <= 0) {
                                        dos.writeUTF(Messaging.WRONG_INPUT.get() + Messaging.WRONG_INTEGER.get());
                                        isIndexingInProcess.compareAndSet(true, false);
                                        break;
                                    }
                                } catch (Exception e) {
                                    dos.writeUTF(Messaging.WRONG_INPUT.get() + Messaging.WRONG_INTEGER.get());
                                    isIndexingInProcess.compareAndSet(true, false);
                                    break;
                                }

                                logger.log(
                                        Level.INFO, "Client " + clientSocket
                                                + " entered num of threads for indexing: " + numberOfThreads
                                );

                                try {
                                    Long time = processIndexing(numberOfThreads);
                                    isIndexed.compareAndSet(false, true);

                                    logger.log(Level.INFO, Messaging.EXECUTION_TIME.get() + time);
                                    dos.writeUTF(Messaging.EXECUTION_TIME.get() + time);
                                } catch (InterruptedException e) {
                                    logger.log(Level.SEVERE, e.getLocalizedMessage());
                                    invertedIndex.clear();
                                    isIndexingInProcess.compareAndSet(true, false);
                                    dos.writeUTF(Messaging.INDEXING_ERROR.get());
                                }
                            } else {
                                dos.writeUTF(Messaging.IN_PROCESS.get());
                            }
                        }
                    }
                    case "2" -> dos.writeUTF(
                            isIndexed.get() ? Messaging.INDEX_READY.get() :
                                    isIndexingInProcess.get() ? Messaging.IN_PROCESS.get() :
                                            Messaging.INDEX_NOT_READY.get()
                    );
                    case "3" -> dos.writeUTF(Messaging.OPTIONS.get());
                    case "4" -> {
                        disconnect = true;
                        dos.writeUTF(Messaging.DISCONNECT.get());
                    }
                    default -> dos.writeUTF(Messaging.WRONG_COMMAND.get());
                }
            }

        } catch (IOException e) {
            logger.log(Level.SEVERE, e.getMessage());
        }
        logger.log(Level.INFO, "Client " + clientSocket + " disconnected");
    }

    public Long processIndexing(int threadsNumber) throws InterruptedException {
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
            completionService.take();
        }
        executor.shutdown();

        return System.currentTimeMillis() - currentTime;
    }
}
