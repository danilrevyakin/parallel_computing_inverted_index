package org.example.client;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Client {
    private static final Logger logger = Logger.getLogger(Client.class.getName());

    public static void main(String[] args) {
        Client client = new Client();
        client.start();
    }

    public void start() {
        try (Socket socket = new Socket("localhost", 10000);
             DataInputStream dis = new DataInputStream(socket.getInputStream());
             DataOutputStream dos = new DataOutputStream(socket.getOutputStream())) {

            boolean isIndexed = dis.readBoolean();
            if (!isIndexed) {
                logger.log(Level.WARNING, "Server indicates that files require indexing");
                int numberOfThreads = 4; // You can adjust this based on your needs
                dos.writeInt(numberOfThreads);

                String indexingMessage = dis.readUTF();
                logger.log(Level.INFO, indexingMessage);
            } else {
                logger.log(Level.INFO, "Server indicates that files are already indexed");
            }

            // Client word search logic
            Scanner scanner = new Scanner(System.in);
            do {
                System.out.print("Enter a word to search (or 'exit' to end): ");
                String word = scanner.nextLine();

                dos.writeUTF(word);

                String response = dis.readUTF();
                if (response.equals("no files found")) {
                    logger.log(Level.WARNING, "No files found for the word: " + word);
                } else {
                    logger.log(Level.INFO, "Files found for the word '" + word + "': " + response);
                }

                System.out.print("Do you want to find one more word? (y/n): ");
                String moreWord = scanner.nextLine();
                dos.writeUTF(moreWord);

            } while (!"exit".equalsIgnoreCase(scanner.nextLine()));

        } catch (IOException e) {
            logger.log(Level.SEVERE, e.getLocalizedMessage(), e);
        }
    }
}
